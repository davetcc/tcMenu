/*
 * Copyright (c)  2016-2019 https://www.thecoderscorner.com (Nutricherry LTD).
 * This product is licensed under an Apache license, see the LICENSE file in the top-level directory.
 *
 */

package com.thecoderscorner.menu.pluginapi.model;

import com.thecoderscorner.menu.pluginapi.CreatorProperty;
import com.thecoderscorner.menu.pluginapi.SubSystem;
import com.thecoderscorner.menu.pluginapi.model.parameter.CodeConversionContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;

import static com.thecoderscorner.menu.pluginapi.util.TestUtils.assertEqualsIgnoringCRLF;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

class FunctionCallBuilderTest {

    @Test
    public void testSimpleFunction() {
        FunctionCallBuilder builder = new FunctionCallBuilder()
                .functionName("analogWrite").param("A0").paramFromPropertyWithDefault("PARAM1", "1")
                .fnparam("func");

        CodeConversionContext context = new CodeConversionContext("root", Collections.singletonList(
                new CreatorProperty("PARAM1", "Desc", "1.01", SubSystem.INPUT)
        ));

        assertEqualsIgnoringCRLF("    analogWrite(A0, 1.01, func());", builder.getFunctionCode(context));
        assertThat(builder.getHeaders()).isEmpty();
    }

    @Test
    public void testPointerTypeFunction() {
        FunctionCallBuilder builder = new FunctionCallBuilder()
                .functionName("callMe").pointerType().objectName("pObject");

        CodeConversionContext context = new CodeConversionContext("root", Collections.emptyList());

        assertEqualsIgnoringCRLF("    pObject->callMe();", builder.getFunctionCode(context));
    }

    @Test
    public void testFunctionWithIncludeAndRoot() {
        FunctionCallBuilder builder = new FunctionCallBuilder()
                .functionName("setup").objectName("lcd")
                .paramMenuRoot().param(16).param(2).quoted("abcdef")
                .requiresHeader("abc.h", true);

        CodeConversionContext context = new CodeConversionContext("root", Collections.emptyList());

        assertEqualsIgnoringCRLF("    lcd.setup(&root, 16, 2, \"abcdef\");", builder.getFunctionCode(context));
        var listOfHeaders = new ArrayList<>(builder.getHeaders());
        assertEquals(1, listOfHeaders.size());
        assertEquals("#include \"abc.h\"", listOfHeaders.get(0).getHeaderCode());

    }
}