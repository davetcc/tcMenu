package com.thecoderscorner.embedcontrol.jfx.controlmgr;

import com.thecoderscorner.embedcontrol.core.controlmgr.*;
import com.thecoderscorner.embedcontrol.core.controlmgr.color.ConditionalColoring;
import com.thecoderscorner.menu.domain.MenuItem;
import com.thecoderscorner.menu.domain.RuntimeListMenuItem;
import com.thecoderscorner.menu.domain.state.ListResponse;
import com.thecoderscorner.menu.domain.state.MenuState;
import com.thecoderscorner.menu.domain.util.MenuItemFormatter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;

import java.util.List;

import static com.thecoderscorner.embedcontrol.core.controlmgr.color.ControlColor.asFxColor;
import static com.thecoderscorner.menu.domain.state.ListResponse.ResponseType.INVOKE_ITEM;
import static com.thecoderscorner.menu.domain.state.ListResponse.ResponseType.SELECT_ITEM;

public class ListEditorComponent extends BaseEditorComponent<Node> {
    private final ObservableList<String> actualData = FXCollections.observableArrayList();
    private ListView<String> listView;
    private Label titleLabel;

    public ListEditorComponent(MenuComponentControl remote, ComponentSettings settings, MenuItem item, ThreadMarshaller marshaller) {
        super(remote, settings, item, marshaller);
    }

    @Override
    public void changeControlSettings(RenderingStatus status, String text) {
        ConditionalColoring condColor = getDrawingSettings().getColors();
        var bgPaint = asFxColor(condColor.backgroundFor(RenderingStatus.NORMAL, ConditionalColoring.ColorComponentType.BUTTON));
        listView.setBackground(new Background(new BackgroundFill(bgPaint, new CornerRadii(0), new Insets(0))));
    }

    public Node createComponent() {
        if (item instanceof RuntimeListMenuItem) {
            listView = new ListView<>();
            ConditionalColoring condColor = getDrawingSettings().getColors();
            var bgPaint = asFxColor(condColor.backgroundFor(RenderingStatus.NORMAL, ConditionalColoring.ColorComponentType.BUTTON));
            listView.setBackground(new Background(new BackgroundFill(bgPaint, new CornerRadii(0), new Insets(0))));
            listView.setItems(actualData);

            if(!item.isReadOnly()) {
                listView.setOnMouseClicked((MouseEvent evt) -> {
                    var selIdx = listView.getSelectionModel().getSelectedIndex();
                    if(selIdx >= 0 && selIdx < actualData.size()) {
                        var selMode = evt.getClickCount() == 2 ? INVOKE_ITEM : SELECT_ITEM;
                        componentControl.editorUpdatedItem(item, new ListResponse(selIdx, selMode));
                    }
                });
            }
            RedrawingMode drawMode = getDrawingSettings().getDrawMode();
            if(drawMode == RedrawingMode.SHOW_NAME || drawMode == RedrawingMode.SHOW_NAME_VALUE) {
                var border = new BorderPane();
                titleLabel = new Label(MenuItemFormatter.defaultInstance().getItemName(item));
                border.setTop(titleLabel);
                border.setCenter(listView);
                return listView;

            } else {
                titleLabel = null;
                return listView;
            }
        } else {
            return new Label("item not a list");
        }
    }

    @Override
    public String getControlText() {
        return null;
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public void onItemUpdated(MenuItem item, MenuState<?> state) {
        this.item = item;
        if (state.getValue() instanceof List) {
            updateAll((List<String>) state.getValue());
        }
    }

    private void updateAll(List<String> values) {
        threadMarshaller.runOnUiThread(() -> {
            if(titleLabel != null) titleLabel.setText(MenuItemFormatter.defaultInstance().getItemName(item));
            actualData.clear();
            actualData.addAll(values);
        });
    }
}
