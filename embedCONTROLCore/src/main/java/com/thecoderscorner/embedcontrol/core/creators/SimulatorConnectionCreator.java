package com.thecoderscorner.embedcontrol.core.creators;

import com.thecoderscorner.embedcontrol.core.simulator.SimulatedRemoteConnection;
import com.thecoderscorner.embedcontrol.core.util.StringHelper;
import com.thecoderscorner.menu.domain.SubMenuItem;
import com.thecoderscorner.menu.domain.state.MenuTree;
import com.thecoderscorner.menu.persist.JsonMenuItemSerializer;
import com.thecoderscorner.menu.remote.AuthStatus;
import com.thecoderscorner.menu.remote.RemoteMenuController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.prefs.Preferences;

import static com.thecoderscorner.menu.domain.state.MenuTree.ROOT;

public class SimulatorConnectionCreator implements ConnectionCreator {
    private final String DEFAULT_DATA = "tcMenuCopy:[\n" +
            "  {\n" +
            "    \"parentId\": 0,\n" +
            "    \"type\": \"analogItem\",\n" +
            "    \"item\": {\n" +
            "      \"maxValue\": 255,\n" +
            "      \"offset\": -128,\n" +
            "      \"divisor\": 2,\n" +
            "      \"unitName\": \"V\",\n" +
            "      \"name\": \"Voltage\",\n" +
            "      \"variableName\": \"\",\n" +
            "      \"id\": 1,\n" +
            "      \"eepromAddress\": 2,\n" +
            "      \"functionName\": \"onVoltageChange\",\n" +
            "      \"readOnly\": false,\n" +
            "      \"localOnly\": false,\n" +
            "      \"visible\": true\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"parentId\": 0,\n" +
            "    \"type\": \"analogItem\",\n" +
            "    \"item\": {\n" +
            "      \"maxValue\": 255,\n" +
            "      \"offset\": 0,\n" +
            "      \"divisor\": 100,\n" +
            "      \"unitName\": \"A\",\n" +
            "      \"name\": \"Current\",\n" +
            "      \"variableName\": \"\",\n" +
            "      \"id\": 2,\n" +
            "      \"eepromAddress\": 4,\n" +
            "      \"functionName\": \"onCurrentChange\",\n" +
            "      \"readOnly\": false,\n" +
            "      \"localOnly\": false,\n" +
            "      \"visible\": true\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"parentId\": 0,\n" +
            "    \"type\": \"enumItem\",\n" +
            "    \"item\": {\n" +
            "      \"enumEntries\": [\n" +
            "        \"Current\",\n" +
            "        \"Voltage\"\n" +
            "      ],\n" +
            "      \"name\": \"Limit\",\n" +
            "      \"variableName\": \"\",\n" +
            "      \"id\": 3,\n" +
            "      \"eepromAddress\": 6,\n" +
            "      \"functionName\": \"onLimitMode\",\n" +
            "      \"readOnly\": false,\n" +
            "      \"localOnly\": false,\n" +
            "      \"visible\": true\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"parentId\": 0,\n" +
            "    \"type\": \"subMenu\",\n" +
            "    \"item\": {\n" +
            "      \"secured\": false,\n" +
            "      \"name\": \"Settings\",\n" +
            "      \"variableName\": \"\",\n" +
            "      \"id\": 4,\n" +
            "      \"eepromAddress\": -1,\n" +
            "      \"readOnly\": false,\n" +
            "      \"localOnly\": false,\n" +
            "      \"visible\": true\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"parentId\": 4,\n" +
            "    \"type\": \"boolItem\",\n" +
            "    \"item\": {\n" +
            "      \"naming\": \"YES_NO\",\n" +
            "      \"name\": \"Pwr Delay\",\n" +
            "      \"variableName\": \"\",\n" +
            "      \"id\": 5,\n" +
            "      \"eepromAddress\": -1,\n" +
            "      \"readOnly\": false,\n" +
            "      \"localOnly\": false,\n" +
            "      \"visible\": true\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"parentId\": 4,\n" +
            "    \"type\": \"actionMenu\",\n" +
            "    \"item\": {\n" +
            "      \"name\": \"Save all\",\n" +
            "      \"variableName\": \"\",\n" +
            "      \"id\": 10,\n" +
            "      \"eepromAddress\": -1,\n" +
            "      \"functionName\": \"onSaveRom\",\n" +
            "      \"readOnly\": false,\n" +
            "      \"localOnly\": false,\n" +
            "      \"visible\": true\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"parentId\": 4,\n" +
            "    \"type\": \"subMenu\",\n" +
            "    \"item\": {\n" +
            "      \"secured\": false,\n" +
            "      \"name\": \"Advanced\",\n" +
            "      \"variableName\": \"\",\n" +
            "      \"id\": 11,\n" +
            "      \"eepromAddress\": -1,\n" +
            "      \"readOnly\": false,\n" +
            "      \"localOnly\": false,\n" +
            "      \"visible\": true\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"parentId\": 11,\n" +
            "    \"type\": \"boolItem\",\n" +
            "    \"item\": {\n" +
            "      \"naming\": \"ON_OFF\",\n" +
            "      \"name\": \"S-Circuit Protect\",\n" +
            "      \"variableName\": \"\",\n" +
            "      \"id\": 12,\n" +
            "      \"eepromAddress\": 8,\n" +
            "      \"readOnly\": false,\n" +
            "      \"localOnly\": false,\n" +
            "      \"visible\": true\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"parentId\": 11,\n" +
            "    \"type\": \"actionMenu\",\n" +
            "    \"item\": {\n" +
            "      \"name\": \"Hidden item\",\n" +
            "      \"variableName\": \"\",\n" +
            "      \"id\": 16,\n" +
            "      \"eepromAddress\": -1,\n" +
            "      \"readOnly\": false,\n" +
            "      \"localOnly\": false,\n" +
            "      \"visible\": true\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"parentId\": 11,\n" +
            "    \"type\": \"boolItem\",\n" +
            "    \"item\": {\n" +
            "      \"naming\": \"ON_OFF\",\n" +
            "      \"name\": \"Temp Check\",\n" +
            "      \"variableName\": \"\",\n" +
            "      \"id\": 13,\n" +
            "      \"eepromAddress\": 9,\n" +
            "      \"readOnly\": false,\n" +
            "      \"localOnly\": false,\n" +
            "      \"visible\": true\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"parentId\": 11,\n" +
            "    \"type\": \"rgbItem\",\n" +
            "    \"item\": {\n" +
            "      \"includeAlphaChannel\": false,\n" +
            "      \"name\": \"RGB\",\n" +
            "      \"variableName\": \"\",\n" +
            "      \"id\": 26,\n" +
            "      \"eepromAddress\": 16,\n" +
            "      \"functionName\": \"onRgbChanged\",\n" +
            "      \"readOnly\": false,\n" +
            "      \"localOnly\": false,\n" +
            "      \"visible\": true\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"parentId\": 0,\n" +
            "    \"type\": \"subMenu\",\n" +
            "    \"item\": {\n" +
            "      \"secured\": false,\n" +
            "      \"name\": \"Status\",\n" +
            "      \"variableName\": \"\",\n" +
            "      \"id\": 7,\n" +
            "      \"eepromAddress\": -1,\n" +
            "      \"readOnly\": false,\n" +
            "      \"localOnly\": false,\n" +
            "      \"visible\": true\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"parentId\": 7,\n" +
            "    \"type\": \"floatItem\",\n" +
            "    \"item\": {\n" +
            "      \"numDecimalPlaces\": 2,\n" +
            "      \"name\": \"Volt A0\",\n" +
            "      \"variableName\": \"\",\n" +
            "      \"id\": 8,\n" +
            "      \"eepromAddress\": -1,\n" +
            "      \"readOnly\": false,\n" +
            "      \"localOnly\": false,\n" +
            "      \"visible\": true\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"parentId\": 7,\n" +
            "    \"type\": \"floatItem\",\n" +
            "    \"item\": {\n" +
            "      \"numDecimalPlaces\": 2,\n" +
            "      \"name\": \"Volt A1\",\n" +
            "      \"variableName\": \"\",\n" +
            "      \"id\": 9,\n" +
            "      \"eepromAddress\": -1,\n" +
            "      \"readOnly\": false,\n" +
            "      \"localOnly\": false,\n" +
            "      \"visible\": true\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"parentId\": 0,\n" +
            "    \"type\": \"subMenu\",\n" +
            "    \"item\": {\n" +
            "      \"secured\": false,\n" +
            "      \"name\": \"Connectivity\",\n" +
            "      \"variableName\": \"\",\n" +
            "      \"id\": 14,\n" +
            "      \"eepromAddress\": -1,\n" +
            "      \"readOnly\": false,\n" +
            "      \"localOnly\": false,\n" +
            "      \"visible\": true\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"parentId\": 14,\n" +
            "    \"type\": \"textItem\",\n" +
            "    \"item\": {\n" +
            "      \"textLength\": 20,\n" +
            "      \"itemType\": \"IP_ADDRESS\",\n" +
            "      \"name\": \"Ip Address\",\n" +
            "      \"variableName\": \"\",\n" +
            "      \"id\": 15,\n" +
            "      \"eepromAddress\": 10,\n" +
            "      \"readOnly\": false,\n" +
            "      \"localOnly\": false,\n" +
            "      \"visible\": true\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"parentId\": 14,\n" +
            "    \"type\": \"customBuildItem\",\n" +
            "    \"item\": {\n" +
            "      \"name\": \"IoT Monitor\",\n" +
            "      \"variableName\": \"IoTMonitor\",\n" +
            "      \"id\": 27,\n" +
            "      \"eepromAddress\": -1,\n" +
            "      \"readOnly\": false,\n" +
            "      \"localOnly\": true,\n" +
            "      \"visible\": true\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"parentId\": 14,\n" +
            "    \"type\": \"customBuildItem\",\n" +
            "    \"item\": {\n" +
            "      \"menuType\": \"AUTHENTICATION\",\n" +
            "      \"name\": \"Authenticator\",\n" +
            "      \"variableName\": \"Authenticator\",\n" +
            "      \"id\": 28,\n" +
            "      \"eepromAddress\": -1,\n" +
            "      \"readOnly\": false,\n" +
            "      \"localOnly\": true,\n" +
            "      \"visible\": true\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"parentId\": 0,\n" +
            "    \"type\": \"subMenu\",\n" +
            "    \"item\": {\n" +
            "      \"secured\": false,\n" +
            "      \"name\": \"Rom Values\",\n" +
            "      \"variableName\": \"\",\n" +
            "      \"id\": 20,\n" +
            "      \"eepromAddress\": -1,\n" +
            "      \"functionName\": \"\",\n" +
            "      \"readOnly\": false,\n" +
            "      \"localOnly\": false,\n" +
            "      \"visible\": true\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"parentId\": 20,\n" +
            "    \"type\": \"scrollItem\",\n" +
            "    \"item\": {\n" +
            "      \"itemWidth\": 10,\n" +
            "      \"eepromOffset\": 1024,\n" +
            "      \"numEntries\": 10,\n" +
            "      \"choiceMode\": \"ARRAY_IN_EEPROM\",\n" +
            "      \"name\": \"Rom Choice\",\n" +
            "      \"variableName\": \"\",\n" +
            "      \"id\": 25,\n" +
            "      \"eepromAddress\": 14,\n" +
            "      \"readOnly\": false,\n" +
            "      \"localOnly\": false,\n" +
            "      \"visible\": true\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"parentId\": 20,\n" +
            "    \"type\": \"scrollItem\",\n" +
            "    \"item\": {\n" +
            "      \"itemWidth\": 10,\n" +
            "      \"eepromOffset\": 0,\n" +
            "      \"numEntries\": 10,\n" +
            "      \"choiceMode\": \"CUSTOM_RENDERFN\",\n" +
            "      \"name\": \"Rom Location\",\n" +
            "      \"variableName\": \"\",\n" +
            "      \"id\": 24,\n" +
            "      \"eepromAddress\": -1,\n" +
            "      \"functionName\": \"\",\n" +
            "      \"readOnly\": false,\n" +
            "      \"localOnly\": false,\n" +
            "      \"visible\": true\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"parentId\": 20,\n" +
            "    \"type\": \"textItem\",\n" +
            "    \"item\": {\n" +
            "      \"textLength\": 10,\n" +
            "      \"itemType\": \"PLAIN_TEXT\",\n" +
            "      \"name\": \"Rom Text\",\n" +
            "      \"variableName\": \"\",\n" +
            "      \"id\": 21,\n" +
            "      \"eepromAddress\": -1,\n" +
            "      \"readOnly\": false,\n" +
            "      \"localOnly\": false,\n" +
            "      \"visible\": true\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"parentId\": 20,\n" +
            "    \"type\": \"actionMenu\",\n" +
            "    \"item\": {\n" +
            "      \"name\": \"Save item\",\n" +
            "      \"variableName\": \"\",\n" +
            "      \"id\": 23,\n" +
            "      \"eepromAddress\": -1,\n" +
            "      \"functionName\": \"onSaveItem\",\n" +
            "      \"readOnly\": false,\n" +
            "      \"localOnly\": false,\n" +
            "      \"visible\": true\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"parentId\": 0,\n" +
            "    \"type\": \"actionMenu\",\n" +
            "    \"item\": {\n" +
            "      \"name\": \"Take display\",\n" +
            "      \"variableName\": \"\",\n" +
            "      \"id\": 17,\n" +
            "      \"eepromAddress\": -1,\n" +
            "      \"functionName\": \"onTakeDisplay\",\n" +
            "      \"readOnly\": false,\n" +
            "      \"localOnly\": false,\n" +
            "      \"visible\": true\n" +
            "    }\n" +
            "  }\n" +
            "]";

    private final String jsonForTree;
    private final String name;
    private final ScheduledExecutorService executorService;
    private SimulatedRemoteConnection remoteConnection;

    public SimulatorConnectionCreator(String jsonForTree, String name, ScheduledExecutorService executorService) {
        this.jsonForTree = StringHelper.isStringEmptyOrNull(jsonForTree) ? DEFAULT_DATA : jsonForTree;
        this.name = name;
        this.executorService = executorService;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public AuthStatus currentState() {
        return null;
    }

    @Override
    public RemoteMenuController start() throws Exception {
        var menuTree = new MenuTree();
        JsonMenuItemSerializer serializer = new JsonMenuItemSerializer();
        try {
            var persistedMenus = serializer.copyTextToItems(this.jsonForTree);
            for (var persistedItem : persistedMenus) {
                menuTree.addMenuItem((SubMenuItem) menuTree.getMenuById(persistedItem.getParentId()).orElse(ROOT), persistedItem.getItem());
            }
        }
        catch (Exception ex) {
            // ignore for now
        }

        remoteConnection = new SimulatedRemoteConnection(menuTree, name, 100, new HashMap<>(), executorService);
        var controller = new RemoteMenuController(remoteConnection, menuTree);
        controller.start();
        return controller;
    }

    @Override
    public void load(Preferences prefs) {

    }

    @Override
    public void save(Preferences prefs) {

    }
}