/*
 * Copyright (c)  2016-2019 https://www.thecoderscorner.com (Dave Cherry).
 * This product is licensed under an Apache license, see the LICENSE file in the top-level directory.
 *
 */

package com.thecoderscorner.menu.editorui.project;

import com.thecoderscorner.menu.domain.MenuItem;
import com.thecoderscorner.menu.domain.state.MenuTree;
import com.thecoderscorner.menu.editorui.generator.CodeGeneratorOptionsBuilder;
import com.thecoderscorner.menu.editorui.generator.applicability.AlwaysApplicable;
import com.thecoderscorner.menu.editorui.generator.core.CreatorProperty;
import com.thecoderscorner.menu.editorui.generator.parameters.IoExpanderDefinitionCollection;
import com.thecoderscorner.menu.editorui.generator.parameters.auth.NoAuthenticatorDefinition;
import com.thecoderscorner.menu.editorui.generator.parameters.eeprom.NoEepromDefinition;
import com.thecoderscorner.menu.editorui.generator.plugin.PluginEmbeddedPlatformsImpl;
import com.thecoderscorner.menu.editorui.generator.validation.CannedPropertyValidators;
import com.thecoderscorner.menu.editorui.util.TestUtils;
import com.thecoderscorner.menu.persist.LocaleMappingHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static com.thecoderscorner.menu.editorui.generator.core.SubSystem.DISPLAY;
import static com.thecoderscorner.menu.editorui.generator.plugin.EmbeddedPlatform.ARDUINO_AVR;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileBasedProjectPersistorTest {
    public static final UUID APPLICATION_UUID = UUID.randomUUID();
    private Path dir;

    @BeforeEach
    public void setUp() throws Exception {
        dir = Files.createTempDirectory("tcmenu");
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @AfterEach
    public void tearDown() throws Exception {
        Files.walk(dir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @Test
    public void testSaveThenLoad() throws IOException {

        Path projFile = dir.resolve("projectSave.emf");

        FileBasedProjectPersistor persistor = new FileBasedProjectPersistor(new PluginEmbeddedPlatformsImpl());
        MenuTree tree = TestUtils.buildCompleteTree();
        List<String> remoteUuids = List.of("uuid3");
        List<CreatorProperty> propsList = Collections.singletonList(new CreatorProperty("name", "desc", "extra desc", "123", DISPLAY, CreatorProperty.PropType.USE_IN_DEFINE, CannedPropertyValidators.textValidator(), new AlwaysApplicable()));
        var options = new CodeGeneratorOptionsBuilder()
                .withPlatform(ARDUINO_AVR)
                .withDisplay("uuid1").withInput("uuid2").withTheme("uuid4").withRemotes(remoteUuids)
                .withProperties(propsList).withAppName("app name").withNewId(APPLICATION_UUID)
                .withEepromDefinition(new NoEepromDefinition()).withAuthenticationDefinition(new NoAuthenticatorDefinition())
                .withExpanderDefinitions(new IoExpanderDefinitionCollection())
                .codeOptions();
        persistor.save(projFile.toString(), "", tree, options, LocaleMappingHandler.NOOP_IMPLEMENTATION);

        MenuTreeWithCodeOptions openResult = persistor.open(projFile.toString());

        compareTrees(tree, openResult.getMenuTree());


        assertEquals(ARDUINO_AVR, openResult.getOptions().getEmbeddedPlatform());
        assertEquals("uuid1", openResult.getOptions().getLastDisplayUuid());
        assertEquals("uuid2", openResult.getOptions().getLastInputUuid());
        assertEquals("uuid3", openResult.getOptions().getLastRemoteCapabilitiesUuids().get(0));
        assertEquals("app name", openResult.getOptions().getApplicationName());
        assertEquals("app name", openResult.getOptions().getApplicationName());
        assertEquals(APPLICATION_UUID, openResult.getOptions().getApplicationUUID());

        List<CreatorProperty> returnedProps = openResult.getOptions().getLastProperties();
        assertEquals("123", returnedProps.get(0).getLatestValue());
        assertEquals("name", returnedProps.get(0).getName());
        assertEquals(DISPLAY, returnedProps.get(0).getSubsystem());
    }

    private void compareTrees(MenuTree sourceTree, MenuTree compTree) {
        Set<MenuItem> srcSubs = sourceTree.getAllSubMenus();
        Set<MenuItem> dstSubs = compTree.getAllSubMenus();

        assertEquals(dstSubs, srcSubs);

        srcSubs.forEach(subMenu -> {
            List<MenuItem> srcItems = sourceTree.getMenuItems(subMenu);
            List<MenuItem> dstItems = compTree.getMenuItems(subMenu);

            assertEquals(dstItems, srcItems);
        });
    }
}