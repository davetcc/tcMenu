package com.thecoderscorner.menu.editorui.controller;

import com.thecoderscorner.menu.editorui.dialog.AppInformationPanel;
import com.thecoderscorner.menu.editorui.dialog.SelectUnicodeRangesDialog;
import com.thecoderscorner.menu.editorui.generator.core.VariableNameGenerator;
import com.thecoderscorner.menu.editorui.generator.font.*;
import com.thecoderscorner.menu.editorui.generator.font.TcUnicodeFontExporter.TcUnicodeFontBlock;
import com.thecoderscorner.menu.editorui.uimodel.CurrentProjectEditorUI;
import com.thecoderscorner.menu.editorui.util.SafeNavigator;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import javax.swing.*;
import java.awt.*;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.*;

import static com.thecoderscorner.menu.editorui.generator.font.AwtLoadedFont.*;
import static com.thecoderscorner.menu.editorui.generator.font.TcUnicodeFontExporter.FontFormat;
import static com.thecoderscorner.menu.editorui.generator.font.TcUnicodeFontExporter.TcUnicodeFontGlyph;
import static com.thecoderscorner.menu.editorui.util.AlertUtil.showAlertAndWait;

public class CreateFontUtilityController {
    public static final long APPROX_ADA_SIZE = 8;
    public static final long ADA_OVERHEAD = 16;
    public static final long APPROX_TCUNICODE_SIZE = 10;
    public static final long TC_UNI_OVERHEAD = 16; // for each block

    public final System.Logger logger = System.getLogger(getClass().getSimpleName());

    public TextField fontFileField;
    public Spinner<Integer> pixelSizeSpinner;
    public ComboBox<FontStyle> fontStyleCombo;
    public TextField outputStructNameField;
    public GridPane fontRenderArea;
    public Menu loadedFontsMenu;
    public Label fontSizeField;
    public Button generateAdafruitBtn;
    public Button generateTcUnicodeBtn;
    public Button chooseRangesButton;
    public ComboBox<AntiAliasMode> antiAliasModeCombo;
    private CurrentProjectEditorUI editorUI;
    private String homeDirectory;
    private Path currentDir;
    private LoadedFont loadedFont = NO_LOADED_FONT;
    private Set<UnicodeBlockMapping> blockMappings = Set.of();
    private final Map<UnicodeBlockMapping,List<ToggleButton>> controlsByBlock = new HashMap<>();
    private final Map<Integer, Boolean> currentlySelected = new HashMap<>();

    public void initialise(CurrentProjectEditorUI editorUI, String homeDirectory) {
        this.editorUI = editorUI;
        this.homeDirectory = homeDirectory;
        var fileName = editorUI.getCurrentProject().getFileName();
        currentDir = (fileName.equals("New")) ? Path.of(homeDirectory) : Path.of(fileName).getParent();
        blockMappings = Set.of(UnicodeBlockMapping.BASIC_LATIN, UnicodeBlockMapping.LATIN1_SUPPLEMENT);

        pixelSizeSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 255, 12));
        pixelSizeSpinner.getValueFactory().valueProperty().addListener((observable, oldValue, newValue) -> {
            changeNameField();
            recalcFont();
        });

        this.fontStyleCombo.setItems(FXCollections.observableArrayList(FontStyle.values()));
        this.fontStyleCombo.getSelectionModel().select(0);

        this.antiAliasModeCombo.setItems(FXCollections.observableArrayList(AntiAliasMode.values()));
        this.antiAliasModeCombo.getSelectionModel().select(0);

        SwingUtilities.invokeLater(() -> {
            var ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            for(var f : ge.getAllFonts()) {
                MenuItem item = new MenuItem(f.getName() + " " + f.getFamily());
                item.setOnAction(event -> {
                    fontFileField.setText("OS " + f.getName() + " " + f.getFamily());
                    loadedFont = new AwtLoadedFont(f, fontStyleCombo.getValue(), pixelSizeSpinner.getValue(), blockMappings, antiAliasModeCombo.getValue());
                    changeNameField();
                    recalcFont();
                });
                loadedFontsMenu.getItems().add(item);
            }
        });

        checkButtons();
    }

    @SuppressWarnings("unused")
    public void onChooseFont(ActionEvent actionEvent) {
        var fileChoice = editorUI.findFileNameFromUser(Optional.of(currentDir), true, "Fonts|*.ttf");
        fileChoice.ifPresent(file -> {
            fontFileField.setText(file);
            loadedFont = new AwtLoadedFont(file, fontStyleCombo.getValue(), pixelSizeSpinner.getValue(), blockMappings, antiAliasModeCombo.getValue());
            changeNameField();
            recalcFont();
        });
    }

    private void recalcFont() {
        fontRenderArea.getChildren().clear();
        controlsByBlock.clear();
        loadedFont.deriveFont(fontStyleCombo.getValue(), pixelSizeSpinner.getValue(), blockMappings, antiAliasModeCombo.getValue());
        int gridRow = 0;
        for(var blockRange : UnicodeBlockMapping.values()) {
            if(!blockMappings.contains(blockRange)) continue;

            fontRenderArea.add(new Label(blockRange.toString()), 0, gridRow, 5, 1);
            CheckBox selAllCheck = new CheckBox("Select/Clear All");
            selAllCheck.setOnAction(event -> {
                var allItems = controlsByBlock.get(blockRange);
                if(allItems != null && !allItems.isEmpty()) {
                    for(var item : allItems) {
                        item.setSelected(selAllCheck.isSelected());
                    }
                    for(int i=blockRange.getStartingCode(); i<=blockRange.getEndingCode();i++) {
                        currentlySelected.put(i, selAllCheck.isSelected());
                    }
                }
                recalcSize();
            });
            fontRenderArea.add(selAllCheck, 6, gridRow, 3, 1);

            gridRow++;

            var allButtons = new ArrayList<ToggleButton>();
            int gridCol = 0;
            for(int i=blockRange.getStartingCode(); i<blockRange.getEndingCode();i++) {
                var maybeGlyph = loadedFont.getConvertedGlyph(i);
                if (maybeGlyph.isPresent()) {
                    var glyph = maybeGlyph.get();
                    Image img = fromGlyphToImg(glyph);
                    var toggleButton = new ToggleButton("U" + glyph.code());
                    ImageView fontImg = new ImageView(img);
                    fontImg.fitHeightProperty().bind(fontRenderArea.getScene().widthProperty().multiply(.05));
                    fontImg.fitWidthProperty().bind(fontRenderArea.getScene().widthProperty().multiply(.05));
                    toggleButton.setGraphic(fontImg);
                    toggleButton.setContentDisplay(ContentDisplay.TOP);
                    toggleButton.setTooltip(new Tooltip(String.format("W=%d, H=%d", glyph.calculatedWidth(), glyph.toBaseLine() + glyph.belowBaseline())));
                    var selected = currentlySelected.get(glyph.code());
                    toggleButton.setSelected(selected != null && selected);
                    toggleButton.setOnAction(event -> {
                        currentlySelected.put(glyph.code(), toggleButton.isSelected());
                        recalcSize();
                    });
                    fontRenderArea.add(toggleButton, gridCol, gridRow);
                    allButtons.add(toggleButton);
                    GridPane.setMargin(toggleButton, new Insets(4));
                    gridCol++;
                    if(gridCol > 9) {
                        gridCol = 0;
                        gridRow++;
                    }
                }
            }
            gridRow++;
            controlsByBlock.put(blockRange, allButtons);
        }
        recalcSize();
    }

    private Image fromGlyphToImg(ConvertedFontGlyph glyph) {
        WritableImage img = new WritableImage(glyph.calculatedWidth() + 1, glyph.belowBaseline() + glyph.toBaseLine() + 2);
        var writer = img.getPixelWriter();
        int bitOffset = 0;
        for(int y=glyph.fontDims().startY();y<glyph.fontDims().lastY(); y++) {
            for(int x=glyph.fontDims().startX();x<glyph.fontDims().lastX(); x++) {
                int byteOffset = bitOffset / 8;
                if(byteOffset >= glyph.data().length) break;
                int d = glyph.data()[byteOffset];
                boolean on = (d & (1<<(7 - (bitOffset % 8)))) != 0;
                if(on) {
                    writer.setColor(x, y, Color.WHITE);
                }
                bitOffset++;
            }
        }
        return img;
    }

    @SuppressWarnings("unused")
    public void onFontStyleChanged(ActionEvent actionEvent) {
        changeNameField();
        recalcFont();
    }

    private void changeNameField() {
        var file = Path.of(fontFileField.getText()).getFileName().toString();
        file = file.replace(".ttf", "");
        var outputName = file + " " + pixelSizeSpinner.getValue() + toSimpleStyle(fontStyleCombo.getValue());
        outputStructNameField.setText(VariableNameGenerator.makeNameFromVariable(outputName));
    }

    private String toSimpleStyle(FontStyle value) {
        return switch (value) {
            case PLAIN -> "";
            case BOLD -> "b";
            case ITALICS -> "i";
            case BOLD_ITALICS -> "bi";
        };
    }

    @SuppressWarnings("unused")
    public void onChooseUnicodeRanges(ActionEvent actionEvent) {
        if(loadedFont instanceof NoLoadedFont) {
            showAlertAndWait(Alert.AlertType.ERROR, "No Font Selected", "Please select a font before choosing unicode ranges", ButtonType.CLOSE);
        } else {
            Stage mainStage = (Stage) outputStructNameField.getScene().getWindow();
            var dlg = new SelectUnicodeRangesDialog(mainStage, loadedFont, blockMappings);
            dlg.getBlockMappings().ifPresent(unicodeBlockMappings -> {
                blockMappings = unicodeBlockMappings;
                recalcFont();
            });

        }
    }

    @SuppressWarnings("unused")
    public void onGenerateAdafruit(ActionEvent actionEvent) {
        internalGenerate(FontFormat.ADAFRUIT);
    }

    @SuppressWarnings("unused")
    public void onGenerateUnicode(ActionEvent actionEvent) {
        internalGenerate(FontFormat.TC_UNICODE);
    }

    private void internalGenerate(FontEncoder.FontFormat format) {
        logger.log(System.Logger.Level.INFO, "Show font conversion save dialog");
        var fileName = editorUI.getCurrentProject().getFileName();
        var dir = (fileName.equals("New")) ? Path.of(homeDirectory) : Path.of(fileName).getParent();
        var maybeOutFile = editorUI.findFileNameFromUser(Optional.of(dir), false, "*.h");
        if(maybeOutFile.isEmpty()) return;
        String outputFile = maybeOutFile.get();
        logger.log(System.Logger.Level.INFO, "Convert font " + format + ", name " + outputFile);
        try(var outStream = new FileOutputStream(outputFile)) {
            var blocks = new ArrayList<TcUnicodeFontBlock>();
            int maxY = 0;

            for(var blockMapping : blockMappings) {
                var glyphsInBlock = new ArrayList<TcUnicodeFontGlyph>();
                for(int i = blockMapping.getStartingCode(); i <= blockMapping.getEndingCode(); i++) {
                    if(!currentlySelected.containsKey(i)) continue;
                    var maybeRawGlyph = loadedFont.getConvertedGlyph(i);
                    if(maybeRawGlyph.isEmpty()) continue;
                    var rawGlyph = maybeRawGlyph.get();

                    int totalHeight = rawGlyph.toBaseLine();
                    glyphsInBlock.add(new TcUnicodeFontGlyph(i, rawGlyph.data(), rawGlyph.fontDims().width(),
                            rawGlyph.fontDims().height(), rawGlyph.totalWidth(),
                            rawGlyph.fontDims().startX(), -(totalHeight - rawGlyph.fontDims().startY())));
                    if(totalHeight > maxY) {
                        maxY = totalHeight;
                    }
                }

                blocks.add(new TcUnicodeFontBlock(blockMapping, glyphsInBlock));
            }

            logger.log(System.Logger.Level.INFO, "Writing to file");
            TcUnicodeFontExporter exporter = new TcUnicodeFontExporter(outputStructNameField.getText(), blocks, maxY);
            exporter.encodeFontToStream(outStream, format);
            logger.log(System.Logger.Level.INFO, "Finished write to file");
            showAlertAndWait(Alert.AlertType.INFORMATION, "Font Export Successful",
                    "Font '" +  outputStructNameField.getText() + "'  exported successfully to '" + outputFile + "' in format " + fontStyleCombo.getValue(),
                    ButtonType.CLOSE);
        } catch (Exception ex) {
            editorUI.alertOnError("Font not converted", "The font was not converted due to the following. " + ex.getMessage());
            logger.log(System.Logger.Level.ERROR, "Unable to convert font to " + format, ex);
        }
    }

    private void recalcSize() {
        long count = currentlySelected.values().stream().filter(e -> e).count();

        long byteSize = currentlySelected.entrySet().stream().filter(Map.Entry::getValue)
                .map(e-> loadedFont.getConvertedGlyph(e.getKey()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(g -> g.data().length)
                .reduce(0, Integer::sum);

        long min = currentlySelected.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .min(Integer::compareTo)
                .orElse(0);
        long max = currentlySelected.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .max(Integer::compareTo)
                .orElse(0);

        var txt = String.format("Choose Characters below, selected = %d, approx size Adafruit %d, TcUnicode %d",
                count, ((max-min) * APPROX_ADA_SIZE) + ADA_OVERHEAD + byteSize,
                (count * APPROX_TCUNICODE_SIZE) + (blockMappings.size() + TC_UNI_OVERHEAD) + byteSize);
        fontSizeField.setText(txt);

        checkButtons();
    }

    private void checkButtons() {
        long count = currentlySelected.values().stream().filter(e -> e).count();
        generateAdafruitBtn.setDisable(count == 0);
        generateTcUnicodeBtn.setDisable(count == 0);
        boolean isFontLoaded = loadedFont instanceof NoLoadedFont;
        pixelSizeSpinner.setDisable(isFontLoaded);
        fontStyleCombo.setDisable(isFontLoaded);
        chooseRangesButton.setDisable(isFontLoaded);
        outputStructNameField.setDisable(isFontLoaded);
    }

    @SuppressWarnings("unused")
    public void onOnlineHelp(ActionEvent actionEvent) {
        SafeNavigator.safeNavigateTo(AppInformationPanel.FONTS_GUIDE_URL);
    }
}
