/*
 * Copyright (c)  2016-2020 https://www.thecoderscorner.com (Dave Cherry).
 * This product is licensed under an Apache license, see the LICENSE file in the top-level directory.
 *
 */

package com.thecoderscorner.menu.editorui.generator.core;

import com.thecoderscorner.menu.domain.CustomBuilderMenuItem;
import com.thecoderscorner.menu.domain.MenuItem;
import com.thecoderscorner.menu.domain.ScrollChoiceMenuItem;
import com.thecoderscorner.menu.domain.SubMenuItem;
import com.thecoderscorner.menu.domain.state.MenuTree;
import com.thecoderscorner.menu.domain.util.MenuItemHelper;
import com.thecoderscorner.menu.editorui.generator.CodeGeneratorOptions;
import com.thecoderscorner.menu.editorui.generator.applicability.AlwaysApplicable;
import com.thecoderscorner.menu.editorui.generator.arduino.ArduinoLibraryInstaller;
import com.thecoderscorner.menu.editorui.generator.arduino.CallbackRequirement;
import com.thecoderscorner.menu.editorui.generator.arduino.MenuItemToEmbeddedGenerator;
import com.thecoderscorner.menu.editorui.generator.parameters.CodeGeneratorCapable;
import com.thecoderscorner.menu.editorui.generator.parameters.CodeParameter;
import com.thecoderscorner.menu.editorui.generator.parameters.auth.EepromAuthenticatorDefinition;
import com.thecoderscorner.menu.editorui.generator.parameters.eeprom.NoEepromDefinition;
import com.thecoderscorner.menu.editorui.generator.plugin.CodePluginItem;
import com.thecoderscorner.menu.editorui.generator.plugin.EmbeddedPlatform;
import com.thecoderscorner.menu.editorui.generator.plugin.FunctionDefinition;
import com.thecoderscorner.menu.editorui.generator.util.VersionInfo;
import com.thecoderscorner.menu.editorui.util.StringHelper;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static com.thecoderscorner.menu.domain.CustomBuilderMenuItem.CustomMenuType.AUTHENTICATION;
import static com.thecoderscorner.menu.domain.CustomBuilderMenuItem.CustomMenuType.REMOTE_IOT_MONITOR;
import static com.thecoderscorner.menu.domain.ScrollChoiceMenuItem.ScrollChoiceMode.ARRAY_IN_EEPROM;
import static com.thecoderscorner.menu.editorui.generator.arduino.ArduinoLibraryInstaller.InstallationType;
import static com.thecoderscorner.menu.editorui.util.StringHelper.isStringEmptyOrNull;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;

public abstract class CoreCodeGenerator implements CodeGenerator {
    protected final System.Logger logger = System.getLogger(getClass().getSimpleName());
    public static final String LINE_BREAK = System.getProperty("line.separator");
    public static final String TWO_LINES = LINE_BREAK + LINE_BREAK;
    public static final String NO_REMOTE_ID = "2c101fec-1f7d-4ff3-8d2b-992ad41e7fcb";

    private static final String COMMENT_HEADER = "/*\n" +
            "    The code in this file uses open source libraries provided by thecoderscorner" + LINE_BREAK + LINE_BREAK +
            "    DO NOT EDIT THIS FILE, IT WILL BE GENERATED EVERY TIME YOU USE THE UI DESIGNER" + LINE_BREAK +
            "    INSTEAD EITHER PUT CODE IN YOUR SKETCH OR CREATE ANOTHER SOURCE FILE." + LINE_BREAK + LINE_BREAK +
            "    All the variables you may need access to are marked extern in this file for easy" + LINE_BREAK +
            "    use elsewhere." + LINE_BREAK +
            " */" + LINE_BREAK + LINE_BREAK;

    protected final ArduinoLibraryInstaller installer;
    protected final SketchFileAdjuster sketchAdjuster;
    protected final EmbeddedPlatform embeddedPlatform;
    protected BiConsumer<System.Logger.Level, String> uiLogger = null;
    protected MenuTree menuTree;
    protected List<String> previousPluginFiles = List.of();
    protected boolean usesProgMem;
    protected CodeConversionContext context;
    protected VariableNameGenerator namingGenerator;
    protected boolean hasRemotePlugins;
    protected CodeGeneratorOptions options;
    private final AtomicInteger logEntryNum = new AtomicInteger(0);

    public CoreCodeGenerator(SketchFileAdjuster adjuster, ArduinoLibraryInstaller installer, EmbeddedPlatform embeddedPlatform) {
        this.installer = installer;
        this.sketchAdjuster = adjuster;
        this.embeddedPlatform = embeddedPlatform;
    }

    public boolean startConversion(Path directory, List<CodePluginItem> codeGenerators, MenuTree menuTree,
                                   List<String> previousPluginFiles, CodeGeneratorOptions options) {
        this.menuTree = menuTree;
        this.options = options;
        namingGenerator = new VariableNameGenerator(menuTree, options.isNamingRecursive());
        this.previousPluginFiles = previousPluginFiles;
        logLine(INFO, "Starting " + embeddedPlatform.getBoardId() + " generate into : " + directory);

        hasRemotePlugins = codeGenerators.stream()
                .anyMatch(p -> p.getSubsystem() == SubSystem.REMOTE && !p.getId().equals(NO_REMOTE_ID));

        usesProgMem = embeddedPlatform.isUsesProgmem();

        try {
            Path srcDir = directory;
            if (options.isSaveToSrc()) {
                srcDir = directory.resolve("src");
            }
            if (!Files.exists(srcDir)) Files.createDirectories(srcDir);
            String cppFile = toSourceFile(srcDir, "_menu.cpp");
            String headerFile = toSourceFile(srcDir, "_menu.h");
            String projectName = directory.getFileName().toString();

            // Prepare the generator by initialising all the structures ready for conversion.
            String root = getFirstMenuVariable(menuTree);
            var allProps = codeGenerators.stream().flatMap(gen -> gen.getProperties().stream()).collect(Collectors.toList());
            context = new CodeConversionContext(embeddedPlatform, root, options, allProps);
            var extractor = new CodeVariableCppExtractor(context, usesProgMem);

            Collection<BuildStructInitializer> menuStructure = generateMenusInOrder(menuTree);

            // generate the source by first generating the CPP and H for the menu definition and then
            // update the sketch. Also, if any plugins have changed, then update them.
            Map<MenuItem, CallbackRequirement> callbackFunctions = callBackFunctions(menuTree);
            generateHeaders(codeGenerators, headerFile, menuStructure, extractor, callbackFunctions);
            generateSource(codeGenerators, cppFile, menuStructure, projectName, extractor, callbackFunctions);
            var fileProcessor = new PluginRequiredFileProcessor(context, uiLogger);
            fileProcessor.dealWithRequiredPlugins(codeGenerators, srcDir, previousPluginFiles);

            internalConversion(directory, srcDir, callbackFunctions, projectName);

            doSanityChecks();

            logLine(INFO, "Process has completed, make sure the code in your IDE is up-to-date.");
            logLine(INFO, "You may need to close the project and then re-open it to pick up changes..");
        } catch (Exception e) {
            logLine(ERROR, "ERROR during conversion---------------------------------------------");
            logLine(ERROR, "The conversion process has failed with an error: " + e.getMessage());
            logLine(ERROR, "A more complete error can be found in the log file in <Home>/.tcMenu");
            logger.log(ERROR, "Exception caught while converting code: ", e);
        }

        return true;
    }

    protected abstract void internalConversion(Path directory, Path srcDir, Map<MenuItem, CallbackRequirement> callbackFunctions, String projectName) throws TcMenuConversionException;


    private List<FunctionDefinition> generateReadOnlyLocal() {
        var allFunctions = new ArrayList<FunctionDefinition>();

        allFunctions.addAll(menuTree.getAllMenuItems().stream().filter(MenuItem::isReadOnly)
                .map(item -> {
                    var params = List.of(new CodeParameter(CodeParameter.NO_TYPE, null,true, "true"));
                    return new FunctionDefinition("setReadOnly", "menu" + menuNameFor(item), false, false, params, new AlwaysApplicable());
                })
                .collect(Collectors.toList())
        );

        allFunctions.addAll(menuTree.getAllMenuItems().stream().filter(MenuItem::isLocalOnly)
                .map(item -> {
                    var params = List.of(new CodeParameter(CodeParameter.NO_TYPE, null, true, "true"));
                    return new FunctionDefinition("setLocalOnly", "menu" + menuNameFor(item), false, false, params, new AlwaysApplicable());
                })
                .collect(Collectors.toList())
        );

        allFunctions.addAll(menuTree.getAllMenuItems().stream().filter(this::isSecureSubMenu)
                .map(item -> {
                    var params = List.of(new CodeParameter(CodeParameter.NO_TYPE, null, true, "true"));
                    return new FunctionDefinition("setSecured", "menu" + menuNameFor(item), false, false, params, new AlwaysApplicable());
                })
                .collect(Collectors.toList())
        );

        // lastly we deal with any INVISIBLE items, visible is the default.
        allFunctions.addAll(menuTree.getAllMenuItems().stream().filter((item) -> !item.isVisible())
                .map(item -> {
                    var params = List.of(new CodeParameter(CodeParameter.NO_TYPE, null, true, "false"));
                    return new FunctionDefinition("setVisible", "menu" + menuNameFor(item), false, false, params, new AlwaysApplicable());
                })
                .collect(Collectors.toList())
        );

        return allFunctions;
    }

    private String menuNameFor(MenuItem item) {
        if(StringHelper.isStringEmptyOrNull(item.getVariableName())) {
            return namingGenerator.makeNameToVar(item);
        }
        else return item.getVariableName();
    }

    private boolean isSecureSubMenu(MenuItem toCheck) {
        SubMenuItem item = MenuItemHelper.asSubMenu(toCheck);
        return item != null && item.isSecured();
    }


    @Override
    public void setLoggerFunction(BiConsumer<System.Logger.Level, String> uiLogger) {
        this.uiLogger = uiLogger;
    }

    protected String getFirstMenuVariable(MenuTree menuTree) {
        return menuTree.getMenuItems(MenuTree.ROOT).stream().findFirst()
                .map(menuItem -> "menu" + menuNameFor(menuItem))
                .orElse("");
    }

    protected Collection<BuildStructInitializer> generateMenusInOrder(MenuTree menuTree) {
        List<MenuItem> root = menuTree.getMenuItems(MenuTree.ROOT);
        List<List<BuildStructInitializer>> itemsInOrder = renderMenu(menuTree, root);
        Collections.reverse(itemsInOrder);
        return itemsInOrder.stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    protected List<List<BuildStructInitializer>> renderMenu(MenuTree menuTree, Collection<MenuItem> itemsColl) {
        ArrayList<MenuItem> items = new ArrayList<>(itemsColl);
        List<List<BuildStructInitializer>> itemsInOrder = new ArrayList<>(100);
        for (int i = 0; i < items.size(); i++) {

            if (items.get(i).hasChildren()) {
                int nextIdx = i + 1;
                String nextSub = (nextIdx < items.size()) ? menuNameFor(items.get(nextIdx)) : "NULL";

                List<MenuItem> childItems = menuTree.getMenuItems(items.get(i));
                String nextChild = (!childItems.isEmpty()) ? menuNameFor(childItems.get(0)) : "NULL";
                itemsInOrder.add(MenuItemHelper.visitWithResult(items.get(i),
                        new MenuItemToEmbeddedGenerator(menuNameFor(items.get(i)), nextSub, nextChild))
                        .orElse(Collections.emptyList()));
                itemsInOrder.addAll(renderMenu(menuTree, childItems));
            } else {
                int nextIdx = i + 1;
                String next = (nextIdx < items.size()) ? menuNameFor(items.get(nextIdx)) : "NULL";
                itemsInOrder.add(MenuItemHelper.visitWithResult(items.get(i),
                        new MenuItemToEmbeddedGenerator(menuNameFor(items.get(i)), next))
                        .orElse(Collections.emptyList()));
            }
        }
        return itemsInOrder;
    }

    protected Map<MenuItem, CallbackRequirement> callBackFunctions(MenuTree menuTree) {
        return menuTree.getAllSubMenus().stream()
                .flatMap(menuItem -> menuTree.getMenuItems(menuItem).stream())
                .filter(mi -> (!isStringEmptyOrNull(mi.getFunctionName())) || MenuItemHelper.isRuntimeStructureNeeded(mi))
                .map(i -> new CallbackRequirement(namingGenerator, i.getFunctionName(), i))
                .collect(Collectors.toMap(CallbackRequirement::getCallbackItem, cr -> cr));
    }

    protected String toSourceFile(Path directory, String ext) {
        Path file = directory.getFileName();
        if (file.toString().equals("src")) {
            // special case, go back one more level
            file = directory.getParent().getFileName();
        }
        return Paths.get(directory.toString(), file.toString() + ext).toString();
    }

    protected void logLine(System.Logger.Level level, String s) {
        var ent = logEntryNum.incrementAndGet();
        if (uiLogger != null) uiLogger.accept(level,ent + " - " + s);
        logger.log(INFO, s);
    }

    protected void generateSource(List<CodePluginItem> generators, String cppFile,
                                  Collection<BuildStructInitializer> menuStructure,
                                  String projectName, CodeVariableExtractor extractor,
                                  Map<MenuItem, CallbackRequirement> callbackRequirements) throws TcMenuConversionException {

        try (Writer writer = new BufferedWriter(new FileWriter(cppFile))) {
            logLine(INFO, "Writing out source CPP file: " + cppFile);

            writer.write(COMMENT_HEADER);

            writer.write("#include <tcMenu.h>" + LINE_BREAK);
            writer.write("#include \"" + projectName + "_menu.h\"" + LINE_BREAK);

            List<HeaderDefinition> includeList = getHeaderDefinitions(generators, menuStructure);

            // and write out the CPP includes, these are needed for things like adafruit fonts that must only be included once ever
            var includeDefs = extractor.mapCppIncludes(includeList);
            writer.write(includeDefs);
            writer.write(StringHelper.isStringEmptyOrNull(includeDefs) ? LINE_BREAK : TWO_LINES);

            writer.write("// Global variable declarations");
            writer.write(LINE_BREAK);
            writer.write("const " + (usesProgMem ? "PROGMEM " : "") + " ConnectorLocalInfo applicationInfo = { \"" +
                    options.getApplicationName() + "\", \"" + options.getApplicationUUID().toString() + "\" };");
            writer.write(LINE_BREAK);
            if(hasRemotePlugins && requiresGlobalServerDefinition()) {
                writer.write("TcMenuRemoteServer remoteServer(applicationInfo);");
                writer.write(LINE_BREAK);
            }

            writer.write(extraCodeDefinitions().stream()
                    .map(CodeGeneratorCapable::generateGlobal)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.joining(LINE_BREAK))
            );
            writer.write(LINE_BREAK);

            writer.write(extractor.mapVariables(
                    generators.stream().flatMap(ecc -> ecc.getVariables().stream()).collect(Collectors.toList())
            ));

            var localCbReq = new HashMap<>(callbackRequirements);

            writer.write(TWO_LINES + "// Global Menu Item declarations");
            writer.write(LINE_BREAK);
            StringBuilder toWrite = new StringBuilder(255);
            menuStructure.forEach(struct -> {
                var callback = localCbReq.remove(struct.getMenuItem());
                if (callback != null) {
                    var srcList = callback.generateSource();
                    if (!srcList.isEmpty()) {
                        toWrite.append(String.join(LINE_BREAK, srcList));
                        toWrite.append(LINE_BREAK);
                    }
                }
                toWrite.append(extractor.mapStructSource(struct));
                toWrite.append(LINE_BREAK);
            });
            writer.write(toWrite.toString());

            writer.write(LINE_BREAK);
            writer.write("void setupMenu() {");
            writer.write(LINE_BREAK);

            writer.write("    // First we set up eeprom and authentication (if needed)." + LINE_BREAK);
            writer.write(extraCodeDefinitions().stream()
                    .map(CodeGeneratorCapable::generateCode)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.joining(LINE_BREAK))
            );
            writer.write(LINE_BREAK);


            List<FunctionDefinition> readOnlyLocal = generateReadOnlyLocal();
            if (!readOnlyLocal.isEmpty()) {
                writer.write("    // Now add any readonly, non-remote and visible flags." + LINE_BREAK);
                writer.write(extractor.mapFunctions(readOnlyLocal));
                writer.write(LINE_BREAK + LINE_BREAK);
            }

            writer.write("    // Code generated by plugins." + LINE_BREAK);
            writer.write(extractor.mapFunctions(
                    generators.stream().flatMap(ecc -> ecc.getFunctions().stream()).collect(Collectors.toList())
            ));

            if(hasRemotePlugins && requiresGlobalServerDefinition()) {
                menuTree.getAllMenuItems().stream()
                        .filter(menuItem -> menuItem instanceof CustomBuilderMenuItem custom && custom.getMenuType() == REMOTE_IOT_MONITOR)
                        .findFirst()
                        .ifPresent(item -> {
                            try {
                                writer.write(TWO_LINES + "    // We have an IoT monitor, register the server" + LINE_BREAK);
                                writer.write("    menu" + namingGenerator.makeNameToVar(item) + ".setRemoteServer(remoteServer);");
                            } catch (IOException e) {
                                logLine(ERROR, "Exception writing cpp file: remote server on IoT monitor");
                                logger.log(ERROR, "Exception during write of remote server block", e);
                            }
                        });
            }


            writer.write(LINE_BREAK + "}" + LINE_BREAK);
            writer.write(LINE_BREAK);

            logLine(INFO, "Finished processing source file.");

        } catch (Exception e) {
            logLine(ERROR, "Failed to generate CPP: " + e.getMessage());
            throw new TcMenuConversionException("Header Generation failed", e);
        }

    }

    private boolean requiresGlobalServerDefinition() {
        try {
            var pluginVer = installer.getVersionOfLibrary("core-remote", InstallationType.CURRENT_PLUGIN);
            var twoPointTwo = VersionInfo.fromString("2.2.0");
            return pluginVer.isSameOrNewerThan(twoPointTwo);
        } catch (IOException e) {
            logger.log(ERROR, "Cannot determine tcMenu library version, assume > 2.2.0");
            return true;
        }
    }

    protected void generateHeaders(List<CodePluginItem> embeddedCreators,
                                   String headerFile, Collection<BuildStructInitializer> menuStructure,
                                   CodeVariableExtractor extractor,
                                   Map<MenuItem, CallbackRequirement> allCallbacks) throws TcMenuConversionException {
        try (Writer writer = new BufferedWriter(new FileWriter(headerFile))) {

            logLine(INFO, "Writing out header file: " + headerFile);

            writer.write(COMMENT_HEADER);
            writer.write(platformIncludes());

            List<HeaderDefinition> includeList = getHeaderDefinitions(embeddedCreators, menuStructure);

            // and write out the includes
            writer.write(extractor.mapIncludes(includeList));

            writer.write(TWO_LINES);
            writer.write("// variables we declare that you may need to access" + LINE_BREAK);
            writer.write("extern const PROGMEM ConnectorLocalInfo applicationInfo;");
            writer.write(LINE_BREAK);

            if(hasRemotePlugins && requiresGlobalServerDefinition()) {
                writer.write("extern TcMenuRemoteServer remoteServer;" + LINE_BREAK);
            }
            // and put the exports in the file too
            writer.write(extractor.mapExports(embeddedCreators.stream()
                    .flatMap(ecc -> ecc.getVariables().stream())
                    .filter(var -> var.getApplicability().isApplicable(context.getProperties()))
                    .collect(Collectors.toList())
            ));
            writer.write(TWO_LINES);
            writer.write("// Any externals needed by IO expanders, EEPROMs etc");
            writer.write(LINE_BREAK);
            writer.write(extraCodeDefinitions().stream()
                    .map(CodeGeneratorCapable::generateExport)
                    .filter(Optional::isPresent).map(Optional::get)
                    .collect(Collectors.joining(LINE_BREAK)));
            writer.write(TWO_LINES);

            writer.write("// Global Menu Item exports" + LINE_BREAK);
            writer.write(menuStructure.stream()
                    .map(extractor::mapStructHeader)
                    .filter(hdr -> !hdr.isEmpty())
                    .collect(Collectors.joining(LINE_BREAK))
            );

            writer.write(TWO_LINES);

            writer.write("// Provide a wrapper to get hold of the root menu item and export setupMenu");
            writer.write(LINE_BREAK);
            writer.write("inline MenuItem& rootMenuItem() { return " + context.getRootObject() + "; }");
            writer.write(LINE_BREAK);
            writer.write("void setupMenu();");
            writer.write(TWO_LINES);

            writer.write("// Callback functions must always include CALLBACK_FUNCTION after the return type"
                    + LINE_BREAK + "#define CALLBACK_FUNCTION" + LINE_BREAK + LINE_BREAK);

            List<CallbackRequirement> callbackRequirements = new ArrayList<>(allCallbacks.values());
            callbackRequirements.sort((CallbackRequirement o1, CallbackRequirement o2) -> {
                if (o1.getCallbackName() == null && o2.getCallbackName() == null) return 0;
                if (o1.getCallbackName() == null) return -1;
                if (o2.getCallbackName() == null) return 1;
                return o1.getCallbackName().compareTo(o2.getCallbackName());
            });

            var callbacksDeclared = new HashSet<String>();
            for (CallbackRequirement callback : callbackRequirements) {
                var header = callback.generateHeader();
                if (!StringHelper.isStringEmptyOrNull(header) && !callbacksDeclared.contains(callback.getCallbackName())) {
                    writer.write(header + LINE_BREAK);
                    callbacksDeclared.add(callback.getCallbackName());
                }
            }

            writer.write(LINE_BREAK + "#endif // MENU_GENERATED_CODE_H" + LINE_BREAK);

            logLine(INFO, "Finished processing header file.");
        } catch (Exception e) {
            logLine(ERROR, "Failed to generate header file: " + e.getMessage());
            throw new TcMenuConversionException("Header Generation failed", e);
        }
    }

    private List<HeaderDefinition> getHeaderDefinitions(List<CodePluginItem> embeddedCreators, Collection<BuildStructInitializer> menuStructure) {
        // first get a list of includes to add to the header file from the creators
        var includeList = embeddedCreators.stream().flatMap(g -> g.getIncludeFiles().stream()).collect(Collectors.toList());

        // now add any extra headers needed for the menu structure items.
        includeList.addAll(menuStructure.stream()
                .flatMap(s -> s.getHeaderRequirements().stream())
                .collect(Collectors.toList()));

        includeList.addAll(extraCodeDefinitions().stream()
                .map(CodeGeneratorCapable::generateHeader)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList()));

        return includeList;
    }

    private Collection<CodeGeneratorCapable> extraCodeDefinitions() {
        var extraDefs = new ArrayList<CodeGeneratorCapable>(options.getExpanderDefinitions().getAllExpanders());
        extraDefs.add(options.getEepromDefinition());
        extraDefs.add(options.getAuthenticatorDefinition());
        return extraDefs;
    }

    protected void doSanityChecks() {
        boolean eepromAuthenticator = options.getAuthenticatorDefinition() instanceof EepromAuthenticatorDefinition;
        boolean noEeprom = options.getEepromDefinition() instanceof NoEepromDefinition;
        boolean errorFound = false;

        if(noEeprom && eepromAuthenticator) {
            logLine(ERROR, "You have selected No EEPROM but then used an EEPROM based authenticator.");
            errorFound = true;
        }

        Collection<MenuItem> allItems = menuTree.getAllMenuItems();

        if(allItems.isEmpty()) {
            logLine(ERROR, "The menu tree is empty, this is not a supported, please add at least one item.");
            errorFound = true;
        }

        if(noEeprom && allItems.stream().anyMatch(mt -> mt instanceof ScrollChoiceMenuItem sc && sc.getChoiceMode() == ARRAY_IN_EEPROM)) {
            logLine(ERROR, "You have included a scroll choice EEPROM item but have not configured an EEPROM.");
            errorFound = true;
        }

        if(!eepromAuthenticator && allItems.stream().anyMatch(mt -> mt instanceof CustomBuilderMenuItem ci && ci.getMenuType() == AUTHENTICATION)) {
            logLine(ERROR, "You have included an EEPROM authentication menu item without using EEPROM authentication.");
            errorFound = true;
        }

        if(errorFound) {
            logLine(ERROR, "It is highly likely that your menu will not work as expected, please fix any errors before deploying.");
        }
    }

    protected abstract String platformIncludes();

}
