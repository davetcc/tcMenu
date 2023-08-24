package com.thecoderscorner.embedcontrol.customization.formbuilder;

import com.thecoderscorner.embedcontrol.core.controlmgr.ComponentPositioning;
import com.thecoderscorner.embedcontrol.core.service.AppDataStore;
import com.thecoderscorner.embedcontrol.core.service.GlobalSettings;
import com.thecoderscorner.embedcontrol.core.service.TcMenuFormPersistence;
import com.thecoderscorner.embedcontrol.customization.*;
import com.thecoderscorner.embedcontrol.jfx.controlmgr.JfxNavigationManager;
import com.thecoderscorner.embedcontrol.jfx.controlmgr.panels.ColorSettingsPresentable;
import com.thecoderscorner.menu.domain.MenuItem;
import com.thecoderscorner.menu.domain.SubMenuItem;
import com.thecoderscorner.menu.domain.state.MenuTree;
import com.thecoderscorner.menu.domain.util.MenuItemHelper;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.thecoderscorner.embedcontrol.core.controlmgr.EditorComponent.PortableAlignment.*;

public class FormEditorController {
    public GridPane editGrid;
    public ListView<GridPositionChoice> selectionList;
    public Button menuButton;
    public Label storeDetailLabel;
    public Label subMenuLabel;
    private GlobalSettings settings;
    private JfxNavigationManager navMgr;
    private MenuItemStore itemStore;
    private MenuTree tree;
    private UUID boardUuid;
    private TcMenuFormSaveConsumer saveConsumer;
    private MenuItem startingPoint = MenuTree.ROOT;
    private Optional<Boolean> maybeRecursiveChange = Optional.empty();

    public void initialise(GlobalSettings settings, MenuTree tree, UUID boardUuid, JfxNavigationManager navMgr,
                           MenuItemStore itemStore, TcMenuFormSaveConsumer saveConsumer) {
        this.settings = settings;
        this.itemStore = itemStore;
        this.navMgr = navMgr;
        this.tree = tree;
        this.boardUuid = boardUuid;
        this.saveConsumer = saveConsumer;
        selectionList.setCellFactory(param -> new GridPositionCell());

        itemStore.changeSubStore(startingPoint.getId());
        maybeRecursiveChange.ifPresent(itemStore::setRecursive);
        maybeRecursiveChange = Optional.empty();
        rebuildColumns();
    }

    public void setStartingPoint(MenuItem where, boolean isRecursive) {
        startingPoint = where;
        maybeRecursiveChange = Optional.of(isRecursive);
    }

    public void closePressed() {
    }

    public void rebuildColumns() {
        var topLevel = itemStore.getTree().getMenuById(itemStore.getRootItemId()).orElseThrow();
        storeDetailLabel.setText(topLevel + ", " + itemStore.getTopLevelColorSet().getColorSchemeName() + ", " + itemStore.getGlobalFontInfo().toWire());

        var list = itemStore.isRecursive() ? getAllMenuItemsInOrder(topLevel, new ArrayList<>(), 0) : getItemsInSingleMenu(new ArrayList<>());
        list.add(new TextGridPositionChoice());
        list.add(new SpacingGridPositionChoice());
        selectionList.setItems(FXCollections.observableArrayList(list));

        editGrid.getColumnConstraints().clear();
        editGrid.getRowConstraints().clear();
        editGrid.getChildren().clear();
        for(int i=0; i < itemStore.getGridSize(); i++) {
            editGrid.getColumnConstraints().add(new ColumnConstraints(10, 100, 999, Priority.SOMETIMES, HPos.CENTER, true));
        }

        for(int i=0; i < itemStore.getMaximumRow(); i++) {
            editGrid.getRowConstraints().add(new RowConstraints(10, 30, 999, Priority.SOMETIMES, VPos.CENTER, true));
        }

        for(int row = 0; row < editGrid.getRowConstraints().size(); row++) {
            for(int col = 0; col < itemStore.getGridSize(); col++) {
                var formComp = new FormMenuComponent(itemStore.getFormItemAt(row, col), settings,
                        new ComponentPositioning(row, col), navMgr, itemStore);
                editGrid.add(formComp, col, row);
            }
        }

        
    }

    private ArrayList<GridPositionChoice> getItemsInSingleMenu(ArrayList<GridPositionChoice> list) {
        MenuItem currentItem = tree.getMenuById(itemStore.getRootItemId()).orElseThrow();
        list.addAll(tree.getMenuItems(currentItem).stream()
                .map(it -> new MenuItemPositionChoice(it, 0))
                .toList());
        return list;
    }

    private ArrayList<GridPositionChoice> getAllMenuItemsInOrder(MenuItem root, ArrayList<GridPositionChoice> list, int level) {
        list.add(new MenuItemPositionChoice(root, level));
        for(var it : tree.getMenuItems(root)) {
            if(it instanceof SubMenuItem) getAllMenuItemsInOrder(it, list, level + 1);
            else list.add(new MenuItemPositionChoice(it, level + 1));
        }

        return list;
    }

    public void onOnlineHelp(ActionEvent actionEvent) {
    }

    public void onColorConfig(ActionEvent actionEvent) {
        var colorPresentable = new ColorSettingsPresentable(settings, navMgr, GlobalColorCustomizable.KEY_NAME, itemStore, true);
        navMgr.pushNavigation(colorPresentable);
    }

    public void onAddNewRow(ActionEvent actionEvent) {
        editGrid.getRowConstraints().add(new RowConstraints(10, 30, 999, Priority.SOMETIMES, VPos.CENTER, true));
        for(int col = 0; col < editGrid.getColumnCount(); col++) {
            int row = editGrid.getRowConstraints().size() - 1;
            var formComp = new FormMenuComponent(itemStore.getFormItemAt(row, col), settings,
                    new ComponentPositioning(row, col), navMgr, itemStore);
            editGrid.add(formComp, col, row);
        }
    }

    public Optional<String> showFileChooser(boolean open) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose a Layout File");

        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Layouts", "*.xml"));
        File f;
        if (open) {
            f = fileChooser.showOpenDialog(getStage());
        } else {
            f = fileChooser.showSaveDialog(getStage());
        }

        return  (f != null) ? Optional.of(f.getPath()) :  Optional.empty();
    }

    public Stage getStage() {
        return (Stage)editGrid.getScene().getWindow();
    }

    public void onLoadLayout(ActionEvent actionEvent) {
        var maybeFile = showFileChooser(true);
        if(maybeFile.isEmpty()) return;
        var file = maybeFile.get();

        itemStore.loadLayout(file, boardUuid);
        rebuildColumns();
    }

    public void onSaveLayout(ActionEvent actionEvent) {
        try {
            var xml = itemStore.getLayoutXmlString(boardUuid);
            saveConsumer.formEditorClosing(xml);
        } catch (IOException e) {
            var alert = new Alert(Alert.AlertType.ERROR, "Failed to persist " + e, ButtonType.CLOSE);
            alert.showAndWait();
        }

    }

    public void onMenuChangeButton(ActionEvent actionEvent) {
        var formProperties = new FormEditorPropertiesPresentable(itemStore, this, navMgr);
        navMgr.pushNavigation(formProperties);
    }

    public class GridPositionCell extends ListCell<GridPositionChoice> {
        private static GridPositionChoice currentDragItem = null;

        public GridPositionCell() {
            setOnDragOver(event -> {
                // do nothing, this is not allowed
            });

            setOnDragDetected(event -> {
                ClipboardContent content = new ClipboardContent();
                content.putString("FormEditComponent");

                Dragboard dragboard = selectionList.startDragAndDrop(TransferMode.MOVE);
                dragboard.setContent(content);

                currentDragItem = getItem();
                event.consume();
            });

            setOnDragDropped(event -> {

            });

            setOnDragExited(event -> {

            });
        }

        public static GridPositionChoice getCurrentDragItem() {
            return currentDragItem;
        }

        public static void setCurrentDragItem(GridPositionChoice currentDragItem) {
            GridPositionCell.currentDragItem = currentDragItem;
        }

        @Override
        protected void updateItem(GridPositionChoice item, boolean empty) {
            super.updateItem(item, empty);
            if(empty) {
                setText("");
            } else {
                setText(item.getName());
            }
        }

        public String toString() {
            return "GridPos List Cell " + getItem();
        }
    }

    public interface GridPositionChoice {
        MenuFormItem createComponent(ComponentPositioning myPosition);
        String getName();
    }

    public class TextGridPositionChoice implements GridPositionChoice {
        @Override
        public MenuFormItem createComponent(ComponentPositioning myPosition) {
            return new TextFormItem("Change me", itemStore.getTopLevelColorSet(), myPosition, LEFT);
        }

        @Override
        public String getName() {
            return "Add Static Text";
        }
    }
    public class SpacingGridPositionChoice implements GridPositionChoice {
        @Override
        public MenuFormItem createComponent(ComponentPositioning myPosition) {
            return new SpaceFormItem(itemStore.getTopLevelColorSet(), myPosition, 10);
        }

        public String getName() {
            return "Add Spacing";
        }
    }

    public class MenuItemPositionChoice implements GridPositionChoice {
        private final MenuItem item;
        private final int indentLevel;

        public MenuItemPositionChoice(MenuItem item, int indentLevel) {
            this.item = item;
            this.indentLevel = indentLevel;
        }

        @Override
        public MenuFormItem createComponent(ComponentPositioning myPosition) {
            return new MenuItemFormItem(item, itemStore.getTopLevelColorSet(), myPosition);
        }

        @Override
        public String getName() {
            var itemType = item.getClass().getSimpleName().replace("MenuItem", "");
            var indentation = " ".repeat(indentLevel);
            return indentation + item + "   " + itemType;
        }

    }
}