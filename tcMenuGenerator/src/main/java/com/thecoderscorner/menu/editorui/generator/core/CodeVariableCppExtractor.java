/*
 * Copyright (c)  2016-2019 https://www.thecoderscorner.com (Dave Cherry).
 * This product is licensed under an Apache license, see the LICENSE file in the top-level directory.
 *
 */

package com.thecoderscorner.menu.editorui.generator.core;

import com.thecoderscorner.menu.editorui.generator.arduino.MenuItemToEmbeddedGenerator;
import com.thecoderscorner.menu.editorui.generator.core.HeaderDefinition.HeaderType;
import com.thecoderscorner.menu.editorui.generator.parameters.*;
import com.thecoderscorner.menu.editorui.generator.plugin.CodeVariable;
import com.thecoderscorner.menu.editorui.generator.plugin.FunctionDefinition;
import com.thecoderscorner.menu.editorui.generator.plugin.VariableDefinitionMode;
import com.thecoderscorner.menu.editorui.util.StringHelper;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.thecoderscorner.menu.editorui.generator.arduino.ArduinoGenerator.LINE_BREAK;
import static com.thecoderscorner.menu.editorui.generator.core.CreatorProperty.PropType;
import static com.thecoderscorner.menu.editorui.generator.parameters.FontDefinition.*;
import static com.thecoderscorner.menu.editorui.generator.ui.UICodePluginItem.USE_TC_UNICODE_PROP_NAME;

public class CodeVariableCppExtractor implements CodeVariableExtractor {
    private final CodeConversionContext context;
    private final boolean progMemNeeded;
    private int levels = 1;
    private final CodeParameter expando = new CodeParameter(CodeParameter.NO_TYPE, "", true, "");

    public CodeVariableCppExtractor(CodeConversionContext context) {
        this(context, true);
    }

    public CodeVariableCppExtractor(CodeConversionContext context, boolean progMemNeeded) {
        this.context = context;
        this.progMemNeeded = progMemNeeded;
    }

    @Override
    public String mapFunctions(List<FunctionDefinition> functions, List<CodeVariable> variables) {
        String functionsUsingNew = variables.stream()
                .filter(CodeVariable::isUseNew)
                .filter(fn -> fn.getApplicability().isApplicable(context.getProperties()))
                .map(this::variableNewToCode)
                .collect(Collectors.joining(LINE_BREAK));
        String rawFunctions = functions.stream()
                .filter(fn -> fn.getApplicability().isApplicable(context.getProperties()))
                .map(this::functionToCode)
                .collect(Collectors.joining(LINE_BREAK));
        if(functionsUsingNew.isEmpty()) {
            return rawFunctions;
        } else {
            return functionsUsingNew + LINE_BREAK + rawFunctions;
        }
    }
    
    private String functionToCode(FunctionDefinition func) {
        var memberAccessor = (func.isObjectPointer()) ? "->" : ".";

        String fn = indentCode();
        if(!StringHelper.isStringEmptyOrNull(func.getObjectName())) {
            fn += expando.expandExpression(context, func.getObjectName()) + memberAccessor;
        }
        fn += func.getFunctionName() + "(";
        var parameters = func.getParameters().stream()
                .map(this::transformParam)
                .collect(Collectors.joining(", "));
        fn += parameters;
        fn += ");";
        return fn;
    }

    private String indentCode() {
        return "    ".repeat(levels);
    }

    private String transformLambda(LambdaCodeParameter lambda) {
        var builder = new StringBuilder(64);
        builder.append("[](");
        builder.append(lambda.getLambda().getParams().stream().map(p -> {
                    if(p.isParamUsed()) {
                        return p.getType() + " " + p.expandExpression(context, p.getValue());
                    }
                    else {
                        return p.getType() + " /*" + p.expandExpression(context, p.getValue()) + "*/";
                    }
                })
                .collect(Collectors.joining(", "))
        );
        builder.append(") {").append(LINE_BREAK);
        levels+=2;
        builder.append(mapFunctions(lambda.getLambda().getFunctionDefinitions(), List.of()));
        levels--;
        builder.append(LINE_BREAK).append(indentCode()).append("}");
        levels--;
        return builder.toString();
    }

    private String transformParam(CodeParameter p) {
        String paramVal;
        if (p instanceof LambdaCodeParameter lcp) {
            paramVal = transformLambda(lcp);
        }
        else {
            paramVal = p.expandExpression(context, p.getValue());
            if (StringHelper.isStringEmptyOrNull(paramVal) && !StringHelper.isStringEmptyOrNull(p.getDefaultValue())) {
                paramVal = p.expandExpression(context, p.getDefaultValue());
            }
            if(p instanceof ReferenceCodeParameter && !MenuItemToEmbeddedGenerator.NULL_PTR_NAMES.contains(paramVal)) {
                paramVal = "&" + paramVal;
            }
            else if(p instanceof FontCodeParameter) {
                paramVal = decodeFontDefinition(paramVal);
            }
            else if(p instanceof IoExpanderCodeParameter) {
                paramVal = decodeIoExpanderDefinition(paramVal);
            }
        }
        return paramVal;
    }

    private String decodeIoExpanderDefinition(String paramVal) {
        var expanders = context.getOptions().getExpanderDefinitions();
        var expander = expanders.getDefinitionById(paramVal).orElse(expanders.getInternalExpander());
        return expander.getVariableName();
    }

    private String decodeFontDefinition(String paramVal) {
        var def = fromString(paramVal);
        return (def.isPresent()) ? def.get().getFontDef() : emptyDef();
    }

    @Override
    public String mapVariables(List<CodeVariable> variables) {
        return variables.stream()
                .filter(CodeVariable::isVariableDefNeeded)
                .filter(cv -> cv.getApplicability().isApplicable(context.getProperties()))
                .distinct().map(this::variableToCode)
                .collect(Collectors.joining(LINE_BREAK));
    }

    private String variableToCode(CodeVariable var) {
        String paramList;
        List<CodeParameter> params = var.getParameterList();
        if(params.isEmpty()) {
            paramList = "";
        }
        else {
            paramList = "(" + params.stream()
                    .map(this::paramOrDefaultValue)
                    .collect(Collectors.joining(", ")) + ")";
        }

        String objectName = expando.expandExpression(context, var.getObjectName());
        String varName = expando.expandExpression(context, var.getVariableName());
        if(var.isUseNew()) {
            return objectName + "* " + varName + ";";
        }
        if(var.isProgMem() && var.getObjectName().equals("char") && var.getVariableName().endsWith("[]")) {
            // progmem strings need special handling and can only have one param.
            var firstParam = params.stream().findFirst().map(this::paramOrDefaultValue).orElseThrow();
            return "const " + objectName + " " + varName + " " + progMem() + " = " + firstParam + ";";
        } else if(var.isProgMem()) {
            return "const " + objectName + " " + progMem() + varName + paramList + ";";
        } else {
            return objectName + " " + varName + paramList + ";";
        }
    }

    private String variableNewToCode(CodeVariable var) {
        String varName = expando.expandExpression(context, var.getVariableName());
        String objectName = expando.expandExpression(context, var.getObjectName());

        List<CodeParameter> params = var.getParameterList();
        var paramList = "(" + params.stream()
                .map(this::paramOrDefaultValue)
                .collect(Collectors.joining(", ")) + ")";
        return indentCode() + varName + " = new " + objectName + paramList + ";";
    }


    private boolean isTcUnicode() {
        return context.getProperties().stream()
                .anyMatch(p -> p.getName().equals(USE_TC_UNICODE_PROP_NAME) && Boolean.parseBoolean(p.getLatestValue()));
    }

    private String paramOrDefaultValue(CodeParameter p) {
        String val = p.getValue();
        if(StringHelper.isStringEmptyOrNull(val) && p.getDefaultValue() != null) {
            val = p.getDefaultValue();
        }
        var expanded = p.expandExpression(context, val);
        if(p instanceof ReferenceCodeParameter) {
            return (val == null || MenuItemToEmbeddedGenerator.NULL_PTR_NAMES.contains(val)) ? "nullptr" : ("&" + expanded);
        }
        else return expanded;
    }

    @Override
    public String mapExports(List<CodeVariable> variables) {
        return variables.stream().filter(CodeVariable::isExported)
                .filter(cv -> cv.getApplicability().isApplicable(context.getProperties()))
                .map(this::exportToCode)
                .distinct()
                .collect(Collectors.joining(LINE_BREAK));

    }

    private String exportToCode(CodeVariable exp) {
        String varName = expando.expandExpression(context, exp.getVariableName());
        if(exp.getDefinitionMode() == VariableDefinitionMode.FONT_EXPORT) {
            var font = fromString(varName).orElseThrow(() -> new IllegalStateException("Font not defined for " + exp.getVariableName()));
            if(font.fontMode() == FontMode.ADAFRUIT || font.fontMode() == FontMode.ADAFRUIT_LOCAL) {
                if(isTcUnicode() && font.fontNumber() == 0) {
                    return "extern const UnicodeFont " + font.fontName() + "[];";
                } else {
                    return "extern const GFXfont " + font.fontName() + ";";
                }
            } else return "";
        } else {
            String varType = expando.expandExpression(context, exp.getObjectName());
            if(exp.isUseNew()) {
                varType += "*";
            }
            return "extern " + (exp.isProgMem() ? "const " : "") + varType + " " + varName + ";";
        }
    }

    @Override
    public String mapDefines() {
        return context.getProperties().stream()
                .filter(cp-> cp.getPropType() == PropType.USE_IN_DEFINE)
                .map(prop -> ("#define " + prop.getName() + " " + prop.getLatestValue()))
                .distinct()
                .collect(Collectors.joining(LINE_BREAK));
    }

    @Override
    public String mapIncludes(List<HeaderDefinition> includeList) {
        return includeList.stream()
                .filter(inc -> inc.getApplicability().isApplicable(context.getProperties()))
                .filter(inc -> inc.getHeaderType() == HeaderType.GLOBAL || inc.getHeaderType() == HeaderType.SOURCE)
                .sorted(Comparator.comparingInt(HeaderDefinition::getPriority))
                .map(this::headerToString)
                .distinct()
                .collect(Collectors.joining(LINE_BREAK));
    }

    @Override
    public String mapCppIncludes(List<HeaderDefinition> includeList) {
        return includeList.stream()
                .filter(inc -> inc.getApplicability().isApplicable(context.getProperties()))
                .filter(inc -> inc.getHeaderType() == HeaderType.CPP_FILE || inc.getHeaderType() == HeaderType.CPP_SRC_FILE
                        || inc.getHeaderType() == HeaderType.FONT)
                .sorted(Comparator.comparingInt(HeaderDefinition::getPriority))
                .map(this::headerToString)
                .filter(s -> !StringHelper.isStringEmptyOrNull(s))
                .distinct()
                .collect(Collectors.joining(LINE_BREAK));
    }

    private String headerToString(HeaderDefinition headerDefinition) {
        if(headerDefinition.getHeaderType() == HeaderType.SOURCE || headerDefinition.getHeaderType() == HeaderType.CPP_SRC_FILE) {
            return "#include \"" + expando.expandExpression(context, headerDefinition.getHeaderName()) + "\"";
        }
        else if(headerDefinition.getHeaderType() == HeaderType.FONT) {
            var def = fromString(expando.expandExpression(context, headerDefinition.getHeaderName()));
            return def.map(FontDefinition::getIncludeDef).orElse("");
        }
        else {
            return "#include <" + expando.expandExpression(context, headerDefinition.getHeaderName()) + ">";
        }
    }

    @Override
    public String mapStructSource(BuildStructInitializer s) {
        if(s.getStringChoiceType() != BuildStructInitializer.StringChoiceType.NO_STRING_CHOICE) {
            return doStringSource(s);
        }

        StringBuilder sb = new StringBuilder(256);
        if(s.isProgMem()) {
            sb.append("const ").append(progMem()).append(s.getStructType()).append(s.getPrefix()).append(s.getStructName()).append(" = { ");
            sb.append(String.join(", ", s.getStructElements()));
            sb.append(" };");
        } else if(s.isInfoBlock()) {
            sb.append(s.getStructType()).append(s.getPrefix()).append(s.getStructName()).append(" = { ");
            sb.append(String.join(", ", s.getStructElements()));
            sb.append(" };");
        } else {
            sb.append(s.getStructType()).append(s.getPrefix()).append(s.getStructName()).append("(");
            sb.append(String.join(", ", s.getStructElements()));
            sb.append(");");
        }
        return sb.toString();

    }

    private String progMem() {
        return progMemNeeded ? "PROGMEM " : "";
    }

    private String doStringSource(BuildStructInitializer s) {
        StringBuilder sb = new StringBuilder(256);
        IntStream.range(0, s.getStructElements().size()).forEach(i -> {
            String textRep = s.getStructElements().get(i);
            if(s.getStringChoiceType() == BuildStructInitializer.StringChoiceType.STRING_CHOICE_VARS) {
                if (s.isProgMem()) {
                    sb.append(String.format("const char enumStr%s_%d[] " + progMem() + "= %s;%s", s.getStructName(), i, textRep, LINE_BREAK));
                } else {
                    sb.append(String.format("char enumStr%s_%d[] = %s;%s", s.getStructName(), i, textRep, LINE_BREAK));
                }
            }
        });
        if(s.isProgMem()) {
            sb.append(String.format("const char* const enumStr%s[] " + progMem() + " = { ", s.getStructName()));
        } else {
            sb.append(String.format("char* enumStr%s[] = { ", s.getStructName()));
        }
        if(s.getStringChoiceType() == BuildStructInitializer.StringChoiceType.STRING_CHOICE_VARS) {
            sb.append(IntStream.range(0, s.getStructElements().size())
                    .mapToObj(i -> "enumStr" + s.getStructName() + "_" + i)
                    .collect(Collectors.joining(", ")));
        } else {
            sb.append(String.join(", ", s.getStructElements()));
        }
        sb.append(" };");

        return sb.toString();
    }

    public String mapStructHeader(BuildStructInitializer s) {
        if(!s.isRequiresExtern()) return "";

        if(s.getStringChoiceType() == BuildStructInitializer.StringChoiceType.NO_STRING_CHOICE) {
            var constant = s.isProgMem() ? "const " : "";
            return  "extern " + constant + s.getStructType() + s.getPrefix() + s.getStructName() + ";";
        } else if(s.isProgMem()) {
            return "extern const char* const enumStr" + s.getStructName() + "[];";
        } else {
            return "extern char* enumStr" + s.getStructName() + "[];";
        }
    }


}
