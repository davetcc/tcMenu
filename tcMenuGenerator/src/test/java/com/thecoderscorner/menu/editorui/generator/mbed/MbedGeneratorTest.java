/*
 * Copyright (c)  2016-2019 https://www.thecoderscorner.com (Dave Cherry).
 * This product is licensed under an Apache license, see the LICENSE file in the top-level directory.
 *
 */

package com.thecoderscorner.menu.editorui.generator.mbed;

import com.thecoderscorner.menu.domain.*;
import com.thecoderscorner.menu.domain.state.MenuTree;
import com.thecoderscorner.menu.editorui.generator.CodeGeneratorOptionsBuilder;
import com.thecoderscorner.menu.editorui.generator.arduino.ArduinoGenerator;
import com.thecoderscorner.menu.editorui.generator.arduino.ArduinoLibraryInstaller;
import com.thecoderscorner.menu.editorui.generator.arduino.ArduinoSketchFileAdjuster;
import com.thecoderscorner.menu.editorui.generator.parameters.IoExpanderDefinitionCollection;
import com.thecoderscorner.menu.editorui.generator.parameters.auth.ReadOnlyAuthenticatorDefinition;
import com.thecoderscorner.menu.editorui.generator.parameters.eeprom.BspStm32EepromDefinition;
import com.thecoderscorner.menu.editorui.generator.plugin.CodePluginConfig;
import com.thecoderscorner.menu.editorui.generator.plugin.DefaultXmlPluginLoader;
import com.thecoderscorner.menu.editorui.generator.plugin.DefaultXmlPluginLoaderTest;
import com.thecoderscorner.menu.editorui.generator.plugin.PluginEmbeddedPlatformsImpl;
import com.thecoderscorner.menu.editorui.generator.util.LibraryStatus;
import com.thecoderscorner.menu.editorui.storage.ConfigurationStorage;
import com.thecoderscorner.menu.persist.VersionInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static com.thecoderscorner.menu.domain.CustomBuilderMenuItem.CustomMenuType.AUTHENTICATION;
import static com.thecoderscorner.menu.domain.CustomBuilderMenuItem.CustomMenuType.REMOTE_IOT_MONITOR;
import static com.thecoderscorner.menu.editorui.generator.arduino.ArduinoGeneratorTest.SERVER_UUID;
import static com.thecoderscorner.menu.editorui.generator.arduino.ArduinoLibraryInstaller.InstallationType;
import static com.thecoderscorner.menu.editorui.generator.parameters.auth.ReadOnlyAuthenticatorDefinition.FlashRemoteId;
import static com.thecoderscorner.menu.editorui.generator.plugin.EmbeddedPlatform.MBED_RTOS;
import static com.thecoderscorner.menu.editorui.util.MenuItemDataSets.LARGE_MENU_STRUCTURE;
import static com.thecoderscorner.menu.editorui.util.TestUtils.assertEqualsIgnoringCRLF;
import static com.thecoderscorner.menu.editorui.util.TestUtils.buildTreeFromJson;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class MbedGeneratorTest {
    private Path projectDir;
    private Path pluginDir;
    private Path rootDir;
    private CodePluginConfig pluginConfig;

    @BeforeEach
    public void setUp() throws Exception {
        rootDir = Files.createTempDirectory("tcmenutest");
        projectDir = rootDir.resolve("project");
        Files.createDirectories(projectDir);

        pluginDir = rootDir.resolve("plugin");
        pluginDir = DefaultXmlPluginLoaderTest.makeStandardPluginInPath(pluginDir, true);
        var embeddedPlatforms = new PluginEmbeddedPlatformsImpl();
        var storage = Mockito.mock(ConfigurationStorage.class);
        when(storage.getVersion()).thenReturn("2.1.0");
        var loader = new DefaultXmlPluginLoader(embeddedPlatforms, storage, false);
        pluginConfig = loader.loadPluginLib(pluginDir);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @AfterEach
    public void tearDown() throws Exception {
        Files.walk(rootDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testMbedConversion() throws IOException {

        MenuTree tree = buildTreeFromJson(LARGE_MENU_STRUCTURE);
        tree.addMenuItem(MenuTree.ROOT, new CustomBuilderMenuItemBuilder().withId(10001).withName("Authenticator")
                .withMenuType(AUTHENTICATION).withEepromAddr(-1).menuItem());
        tree.addMenuItem(MenuTree.ROOT, new CustomBuilderMenuItemBuilder().withId(10002).withName("IoT Monitor")
                .withMenuType(REMOTE_IOT_MONITOR).withEepromAddr(-1).menuItem());
        tree.addMenuItem(MenuTree.ROOT, new AnalogMenuItemBuilder().withId(10003).withName("Analog RAM")
                .withDivisor(10).withOffset(0).withMaxValue(100).withStaticDataInRAM(true).menuItem());
        tree.addMenuItem(MenuTree.ROOT, new EditableTextMenuItemBuilder().withId(10003).withName("Analog RAM")
                .withLength(10).withEditItemType(EditItemType.PLAIN_TEXT).withFunctionName("textRenderRtCall").menuItem());

        ArduinoLibraryInstaller installer = Mockito.mock(ArduinoLibraryInstaller.class);
        when(installer.statusOfAllLibraries()).thenReturn(new LibraryStatus(true, true, true, true));
        when(installer.getVersionOfLibrary("core-remote", InstallationType.CURRENT_PLUGIN)).thenReturn(VersionInfo.fromString("2.2.1"));

        var flashRemotes = List.of(
                new FlashRemoteId("name1", "first-uuid"),
                new FlashRemoteId("name2", "second-uuid")
        );

        var options = new CodeGeneratorOptionsBuilder()
                .withPlatform(MBED_RTOS.getBoardId()).withAppName("tester").withNewId(SERVER_UUID)
                .withEepromDefinition(new BspStm32EepromDefinition(50))
                .withAuthenticationDefinition(new ReadOnlyAuthenticatorDefinition("1234", flashRemotes))
                .withExpanderDefinitions(new IoExpanderDefinitionCollection())
                .withRecursiveNaming(true).withSaveToSrc(true)
                .codeOptions();

        ArduinoSketchFileAdjuster adjuster = new ArduinoSketchFileAdjuster(options);
        ArduinoGenerator generator = new ArduinoGenerator(adjuster, installer, MBED_RTOS);

        var firstPlugin = pluginConfig.getPlugins().get(0);
        firstPlugin.getProperties().stream()
                .filter(p -> p.getName().equals("SWITCH_IODEVICE"))
                .findFirst()
                .ifPresent(p -> p.setLatestValue("io23017"));

        assertTrue(generator.startConversion(projectDir, pluginConfig.getPlugins(), tree, List.of(), options));

        var sourceDir = projectDir.resolve("src");

        var cppGenerated = new String(Files.readAllBytes(sourceDir.resolve(projectDir.getFileName() + "_menu.cpp")));
        var hGenerated = new String(Files.readAllBytes(sourceDir.resolve(projectDir.getFileName() + "_menu.h")));
        var pluginGeneratedH = new String(Files.readAllBytes(sourceDir.resolve("source.h")));
        var pluginGeneratedCPP = new String(Files.readAllBytes(sourceDir.resolve("source.cpp")));
        var pluginGeneratedTransport = new String(Files.readAllBytes(sourceDir.resolve("MySpecialTransport.h")));

        var cppTemplate = new String(Objects.requireNonNull(getClass().getResourceAsStream("/generator/templateMbed.cpp")).readAllBytes());
        var hTemplate = new String(Objects.requireNonNull(getClass().getResourceAsStream("/generator/templateMbed.h")).readAllBytes());

        cppGenerated = cppGenerated.replaceAll("#include \"tcmenu[^\"]*\"", "replacedInclude");
        cppTemplate = cppTemplate.replaceAll("#include \"tcmenu[^\"]*\"", "replacedInclude");

        // these files should line up. IF they do not because of the change in the ArduinoGenerator,
        // then make sure the change is good before adjusting the templates.
        assertEqualsIgnoringCRLF(cppTemplate, cppGenerated);
        assertEqualsIgnoringCRLF(hTemplate, hGenerated);
        assertEqualsIgnoringCRLF("CPP_FILE_CONTENT 10 otherKey", pluginGeneratedCPP);
        assertEqualsIgnoringCRLF("H_FILE_CONTENT 10 otherKey", pluginGeneratedH);
        assertEqualsIgnoringCRLF("""
                My Transport file
                #define THE_SERIAL Serial
                """, pluginGeneratedTransport);
    }
}