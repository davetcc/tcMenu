package com.thecoderscorner.embedcontrol.customization.formbuilder;

import com.thecoderscorner.embedcontrol.core.controlmgr.ComponentPositioning;
import com.thecoderscorner.embedcontrol.core.controlmgr.ComponentSettings;
import com.thecoderscorner.embedcontrol.core.controlmgr.MenuGridComponent;
import com.thecoderscorner.embedcontrol.core.service.GlobalSettings;
import com.thecoderscorner.embedcontrol.customization.*;
import com.thecoderscorner.embedcontrol.jfx.controlmgr.HorizontalSliderAnalogComponent;
import com.thecoderscorner.embedcontrol.jfx.controlmgr.JfxMenuEditorFactory;
import com.thecoderscorner.embedcontrol.jfx.controlmgr.JfxNavigationManager;
import com.thecoderscorner.menu.domain.MenuItem;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.effect.InnerShadow;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import static com.thecoderscorner.embedcontrol.customization.MenuFormItem.NO_FORM_ITEM;
import static com.thecoderscorner.embedcontrol.customization.formbuilder.FormEditorController.GridPositionCell.setCurrentDragItem;

/**
 * This represents an item in an embedCONTROL pre-made form.
 */
public class FormMenuComponent extends BorderPane {
    private final GlobalSettings settings;
    private final MenuItemStore store;
    private final JfxNavigationManager navMgr;
    private final Button editTopBtn;
    private final Button removeButton;
    private final ComponentPositioning myPosition;
    private final Label editBottomLabel;
    private MenuFormItem formItem;
    private JfxMenuEditorFactory controlFactory;

    public FormMenuComponent(MenuFormItem item, GlobalSettings settings, ComponentPositioning positioning,
                             JfxNavigationManager navMgr, JfxMenuEditorFactory controlFactory, MenuItemStore store) {
        this.navMgr = navMgr;
        this.controlFactory = controlFactory;
        this.myPosition = positioning;
        this.settings = settings;
        this.store = store;
        this.formItem = item;
        editTopBtn = new Button("Empty");
        editTopBtn.setMaxWidth(999);
        editTopBtn.setOnAction(event -> showEditingForm(store));
        editBottomLabel = new Label("");
        editTopBtn.setMaxWidth(999);
        setTop(editTopBtn);
        removeButton = new Button("X");
        removeButton.setMaxHeight(999);
        removeButton.setOnAction(event -> clearDown());
        setRight(removeButton);
        setBottom(editBottomLabel);
        getStyleClass().add("menu-edit-item");
        setPadding(new Insets(2));

        evaluateFormItem();
        workOutIfDragDropNeeded();
    }
    void workOutIfDragDropNeeded() {
        // do not allow drag and drop other than for NoFormItem cases.
        if(formItem instanceof MenuFormItem.NoFormItem) {
            setOnDragDropped(event -> {
                var dragged = FormEditorController.GridPositionCell.getCurrentDragItem();
                if (dragged != null) {
                    setFormItem(dragged.createComponent(myPosition));
                    workOutIfDragDropNeeded();
                }
                event.consume();
                event.setDropCompleted(true);
                setEffect(null);
            });

            setOnDragExited(event -> {
                event.consume();
                setEffect(null);
            });

            setOnDragOver(event -> {
                var dragged = FormEditorController.GridPositionCell.getCurrentDragItem();
                if (dragged != null) {
                    event.acceptTransferModes(TransferMode.MOVE);
                    InnerShadow shadow = new InnerShadow();
                    shadow.setColor(Color.web("#888888"));
                    shadow.setOffsetX(1.0);
                    shadow.setOffsetY(1.0);
                    setEffect(shadow);
                    event.consume();
                }
            });
            editTopBtn.setOnDragDetected(null);
        } else {
            setOnDragDropped(null);
            setOnDragExited(null);
            setOnDragOver(null);
            editTopBtn.setOnDragDetected(event -> {
                ClipboardContent content = new ClipboardContent();
                content.putString("FormEditComponent");

                Dragboard dragboard = this.startDragAndDrop(TransferMode.MOVE);
                dragboard.setContent(content);

                setCurrentDragItem(new FormEditorController.ExistingGridPositionChoice(this));
                event.consume();
            });
        }
    }

    public void clearDown() {
        setFormItem(NO_FORM_ITEM);
        evaluateFormItem();
        workOutIfDragDropNeeded();
    }

    private void setFormItem(MenuFormItem component) {
        this.formItem = component;
        store.setFormItemAt(myPosition.getRow(), myPosition.getCol(), formItem);
        evaluateFormItem();
    }

    private void showEditingForm(MenuItemStore store) {
        if(formItem instanceof MenuItemFormItem || formItem instanceof TextFormItem) {
            int maxColsLeft = (store.getGridSize() - myPosition.getCol());
            for(int i = 1; i < maxColsLeft; i++) {
                if(!(store.getFormItemAt(myPosition.getRow(), i + myPosition.getCol()) instanceof MenuFormItem.NoFormItem)) {
                    maxColsLeft = Math.max(1, i);
                    break;
                }
            }
            var presentable = new EditFormComponentPresentable(settings, this, navMgr, maxColsLeft);
            navMgr.pushNavigation(presentable);
        }
    }

    public void evaluateFormItem() {
        editTopBtn.setDisable(formItem instanceof MenuFormItem.NoFormItem);
        removeButton.setDisable(formItem instanceof MenuFormItem.NoFormItem);

        editTopBtn.setText(formItem.getDescription());

        var colorScheme = "none";
        if(formItem.getSettings() != null) {
            colorScheme = formItem.getSettings().getColorSchemeName();
        }
        editBottomLabel.setText("Font: " + formItem.getFontInfo().toWire() + ", Col: " + colorScheme);
        GridPane.setColumnSpan(this, formItem.getPositioning().getColSpan());

        if(formItem instanceof TextFormItem tfi){
            setCenter(new Label(tfi.getText()));
        } else if(formItem instanceof SpaceFormItem sfi) {
            var spinner = new Spinner<Integer>(1, 50, sfi.getVerticalSpace());
            spinner.valueProperty().addListener((observable, oldValue, newValue) -> sfi.setVerticalSpace(newValue));
            setCenter(spinner);
        } else if(formItem instanceof MenuItemFormItem mfi) {
            var settings = new ComponentSettings(
                    new MenuGridComponent.ScreenLayoutBasedConditionalColor(store, myPosition),
                    mfi.getFontInfo(), mfi.getAlignment(), mfi.getPositioning(), mfi.getRedrawingMode(),
                    mfi.getControlType(), mfi.getCustomDrawing(), true);
            controlFactory.getComponentEditorItem(mfi.getItem(), settings, this::emptyConsumer)
                    .ifPresentOrElse(control -> setCenter(control.createComponent()), ()->setCenter(new Label("Empty")));
        } else {
            setCenter(new Label(""));
        }

    }

    private void emptyConsumer(MenuItem menuItem) {
    }

    public ColorCustomizable getColorCustomizable() {
        return formItem.getSettings();
    }

    public void setColorCustomizable(ColorCustomizable colorCustomizable) {
        this.getFormItem().setSettings(colorCustomizable);
        formItem.setSettings(colorCustomizable);
    }

    public MenuFormItem getFormItem() {
        return formItem;
    }

    public MenuItemStore getStore() {
        return store;
    }
}
