package com.thecoderscorner.embedcontrol.jfx.controlmgr.panels;

import com.thecoderscorner.embedcontrol.core.controlmgr.PanelPresentable;
import com.thecoderscorner.embedcontrol.core.service.GlobalSettings;
import com.thecoderscorner.embedcontrol.customization.ColorCustomizable;
import com.thecoderscorner.embedcontrol.customization.ComponentSettingsCustomizer;
import com.thecoderscorner.embedcontrol.jfx.controlmgr.JfxNavigationManager;
import com.thecoderscorner.menu.domain.MenuItem;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

public class ItemSettingsPresentable implements PanelPresentable<Node> {
    private final String name;
    private final int id;
    private final JfxNavigationManager navigator;
    private final GlobalSettings globalSettings;
    private final ComponentSettingsCustomizer customizerConsumer;
    private ItemSettingsController controller;

    public ItemSettingsPresentable(MenuItem item, JfxNavigationManager navigator, GlobalSettings globalSettings,
                                   ComponentSettingsCustomizer colorSettings) {
        this(item.getId(), item.getName(), navigator, globalSettings, colorSettings, colorSettings);
    }

    public ItemSettingsPresentable(int id, String name, JfxNavigationManager navigator, GlobalSettings globalSettings,
                                   ColorCustomizable colorSettings, ComponentSettingsCustomizer customizerConsumer) {

        this.name = name;
        this.id = id;
        this.navigator = navigator;
        this.globalSettings = globalSettings;
        this.customizerConsumer = customizerConsumer;
    }

    @Override
    public Node getPanelToPresent(double width) throws Exception {
        var loader = new FXMLLoader(ColorSettingsPresentable.class.getResource("/core_fxml/itemSettings.fxml"));
        Pane loadedPane = loader.load();
        controller = loader.getController();
        controller.initialise(navigator, globalSettings, name, id, customizerConsumer);
        return loadedPane;
    }

    @Override
    public String getPanelName() {
        return "Item Settings " + name;
    }

    @Override
    public boolean canBeRemoved() {
        return true;
    }

    @Override
    public boolean canClose() {
        return true;
    }

    @Override
    public void closePanel() {
        controller.panelWasClosed();
    }
}