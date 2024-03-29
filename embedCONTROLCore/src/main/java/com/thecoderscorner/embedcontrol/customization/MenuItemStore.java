package com.thecoderscorner.embedcontrol.customization;

import com.thecoderscorner.embedcontrol.core.controlmgr.ComponentPositioning;
import com.thecoderscorner.embedcontrol.core.controlmgr.ControlType;
import com.thecoderscorner.embedcontrol.core.controlmgr.RedrawingMode;
import com.thecoderscorner.embedcontrol.core.controlmgr.color.ControlColor;
import com.thecoderscorner.embedcontrol.core.service.GlobalSettings;
import com.thecoderscorner.embedcontrol.core.util.StringHelper;
import com.thecoderscorner.embedcontrol.customization.customdraw.BooleanCustomDrawingConfiguration;
import com.thecoderscorner.embedcontrol.customization.customdraw.CustomDrawingConfiguration;
import com.thecoderscorner.embedcontrol.customization.customdraw.NumberCustomDrawingConfiguration;
import com.thecoderscorner.embedcontrol.customization.customdraw.StringCustomDrawingConfiguration;
import com.thecoderscorner.menu.domain.state.MenuTree;
import com.thecoderscorner.menu.domain.state.PortableColor;
import com.thecoderscorner.menu.persist.XMLDOMHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static com.thecoderscorner.embedcontrol.core.controlmgr.EditorComponent.PortableAlignment;
import static com.thecoderscorner.embedcontrol.core.controlmgr.color.ConditionalColoring.ColorComponentType;
import static com.thecoderscorner.embedcontrol.core.controlmgr.color.ConditionalColoring.ColorComponentType.*;
import static com.thecoderscorner.embedcontrol.customization.MenuFormItem.*;
import static com.thecoderscorner.embedcontrol.customization.customdraw.BooleanCustomDrawingConfiguration.*;
import static com.thecoderscorner.embedcontrol.customization.customdraw.CustomDrawingConfiguration.NoOpCustomDrawingConfiguration.CUSTOM_DRAW_NONE;

public class MenuItemStore {
    private final System.Logger logger = System.getLogger(getClass().getSimpleName());
    private final MenuTree tree;
    private final int initialRows;
    private final int initalCols;
    private final boolean recursiveDefault;
    private String layoutName;
    private final Map<String, ColorCustomizable> colorSets = new HashMap<>();
    private final Map<Integer, SubMenuStore> subMenuStores = new HashMap<>();
    private final GlobalSettings settings;
    private int gridSize;
    private SubMenuStore currentSubStore;
    private Map<String, CustomDrawingConfiguration> customDrawingMap = new HashMap<>();
    private Map<String, byte[]> imageDataMap = new HashMap<>();

    /**
     * create a new item store of a given dimension and fill with empty slots
     *
     * @param rows    the number of rows
     * @param columns the number of columns
     */
    public MenuItemStore(GlobalSettings settings, MenuTree tree, String layoutName, int rows, int columns, boolean recursive) {
        this.settings = settings;
        this.gridSize = columns;
        this.initalCols = columns;
        this.tree = tree;
        this.initialRows = rows;
        this.recursiveDefault = recursive;

        reset(layoutName);
    }

    public void reset(String name) {
        this.gridSize = initalCols;

        var rootColorSet = new GlobalColorCustomizable(settings);
        colorSets.put(GlobalColorCustomizable.KEY_NAME, rootColorSet);
        customDrawingMap.put(CUSTOM_DRAW_NONE, NO_CUSTOM_DRAWING);

        subMenuStores.clear();
        var subStore = new SubMenuStore(MenuTree.ROOT.getId(), rootColorSet, FONT_100_PERCENT, recursiveDefault);
        subMenuStores.put(MenuTree.ROOT.getId(), subStore);
        currentSubStore = subStore;

        imageDataMap.clear();

        layoutName = name;
    }
    public List<String> getAllColorSetNames() {
        return colorSets.keySet().stream().toList();
    }

    public void addColorSet(ColorCustomizable colors) {
        colorSets.put(colors.getColorSchemeName(), colors);
    }

    public ColorCustomizable getColorSet(String name) {
        if (StringHelper.isStringEmptyOrNull(name)) {
            return colorSets.get(GlobalColorCustomizable.KEY_NAME);
        }
        return colorSets.get(name);
    }

    public void changeSubStore(int subId) {
        if (!subMenuStores.containsKey(subId)) {
            subMenuStores.put(subId, new SubMenuStore(subId, getColorSet(GlobalColorCustomizable.KEY_NAME),
                    FONT_100_PERCENT, currentSubStore.isRecursive()));
        }
        currentSubStore = subMenuStores.get(subId);
    }

    public ColorCustomizable getTopLevelColorSet() {
        return currentSubStore.getColorSet();
    }

    public void setTopLevelColorSet(ColorCustomizable topLevelColorSet) {
        currentSubStore.setColorSet(topLevelColorSet);
    }

    public int getRootItemId() {
        return currentSubStore.getSubId();
    }

    public Map<Integer, RowEntry> getRowEntries() {
        return currentSubStore.getRowEntries();
    }

    public boolean isRecursive() {
        return currentSubStore.isRecursive();
    }

    public void setRecursive(boolean recursive) {
        currentSubStore.setRecursive(recursive);
    }

    public String getLayoutName() {
        return layoutName;
    }

    public void setLayoutName(String layoutName) {
        this.layoutName = layoutName;
    }

    public int getGridSize() {
        return gridSize;
    }

    public void setGridSize(int gridSize) {
        this.gridSize = gridSize;
        for (var r : currentSubStore.getRowEntries().values()) {
            r.resizeTo(gridSize);
        }
    }

    public boolean hasItemAtPosition(int row, int col) {
        Map<Integer, RowEntry> rowEntries = currentSubStore.getRowEntries();
        return row < rowEntries.size() && rowEntries.get(row).getAtPosition(col) != null;
    }

    public int getMaximumRow() {
        return currentSubStore.getRowEntries().keySet().stream().max(Integer::compareTo).orElse(0) + 1;
    }

    public void setFormItemAt(int row, int col, MenuFormItem item) {
        if (col >= gridSize) throw new IllegalArgumentException("Out of bounds " + col + " >= " + gridSize);
        if (!currentSubStore.getRowEntries().containsKey(row)) {
            currentSubStore.getRowEntries().put(row, new RowEntry());
        }
        currentSubStore.getRowEntries().get(row).setAtPosition(col, item);
    }

    public MenuFormItem getFormItemAt(int row, int col) {
        if (col >= gridSize) throw new IllegalArgumentException("Out of bounds " + col + " >= " + gridSize);
        if (!currentSubStore.getRowEntries().containsKey(row)) {
            setFormItemAt(row, col, NO_FORM_ITEM);
        }
        return currentSubStore.getRowEntries().get(row).getAtPosition(col);
    }

    public Optional<MenuFormItem> getFormItemIfPresent(int row, int col) {
        if (col >= gridSize) return Optional.empty();
        var r = currentSubStore.getRowEntries().get(row);
        if (r == null) return Optional.empty();
        var mfi = r.getAtPosition(col);
        return (mfi instanceof NoFormItem) ? Optional.empty() : Optional.of(mfi);
    }

    public void removeColorSet(ColorCustomizable removal) {
        if (checkAllEntriesFor(removal)) throw new IllegalArgumentException("Entry has references");
        colorSets.remove(removal.getColorSchemeName());
    }

    private boolean checkAllEntriesFor(ColorCustomizable removal) {
        // global cannot be deleted
        if (removal.isRepresentingGlobal() || removal.getColorSchemeName().equals(GlobalColorCustomizable.KEY_NAME)) {
            return true;
        }

        // cannot delete while item references.
        for (var ent : currentSubStore.getRowEntries().values()) {
            for (var col : ent.items) {
                if (col.getSettings() != null && col.getSettings().equals(removal)) return true;
            }
        }

        // no references
        return false;
    }

    public void loadLayout(Path file, UUID uuid) {
        try (var input = new BufferedInputStream((new FileInputStream(file.toFile())))) {
            var doc = XMLDOMHelper.loadDocumentStream(input);
            Element root = doc.getDocumentElement();
            loadLayoutInternal(root, uuid);
        } catch (Exception e) {
            logger.log(System.Logger.Level.ERROR, "Could not load layout from " + file, e);
        }
    }

    public void loadLayout(String data, UUID uuid) {
        try {
            var doc = XMLDOMHelper.loadDocumentFromData(data);
            loadLayoutInternal(doc.getDocumentElement(), uuid);
        } catch (Exception e) {
            logger.log(System.Logger.Level.ERROR, "Could not load layout", e);
        }
    }

    public void loadLayoutInternal(Element root, UUID uuid) {
        if (!root.getNodeName().equals("EmbedControl")) throw new IllegalArgumentException("Not EmbedControl document");
        if (!XMLDOMHelper.getAttributeOrDefault(root, "boardUuid", "").equalsIgnoreCase(uuid.toString()))
            throw new IllegalArgumentException("UUID does not match");
        layoutName = XMLDOMHelper.getAttributeOrDefault(root, "layoutName", "Unknown");

        var csEle = XMLDOMHelper.elementWithName(root, "ColorSets");
        var allSets = XMLDOMHelper.getChildElementsWithName(csEle, "ColorSet");

        colorSets.clear();
        GlobalColorCustomizable globalColorSet = new GlobalColorCustomizable(settings);
        colorSets.put(GlobalColorCustomizable.KEY_NAME, globalColorSet);

        for (var set : allSets) {
            ColorCustomizable cc = new NamedColorCustomizable(set.getAttribute("name"));
            loadSingleColorValue(set, cc, "text", TEXT_FIELD);
            loadSingleColorValue(set, cc, "button", BUTTON);
            loadSingleColorValue(set, cc, "custom", CUSTOM);
            loadSingleColorValue(set, cc, "dialog", DIALOG);
            loadSingleColorValue(set, cc, "error", ERROR);
            loadSingleColorValue(set, cc, "highlight", HIGHLIGHT);
            loadSingleColorValue(set, cc, "pending", PENDING);
            colorSets.put(cc.getColorSchemeName(), cc);
        }

        customDrawingMap.clear();
        customDrawingMap.put(CUSTOM_DRAW_NONE, NO_CUSTOM_DRAWING);
        var cdEle = XMLDOMHelper.elementWithName(root, "CustomDrawings");
        if(cdEle != null) {
            var decoder = Base64.getDecoder();
            var allImages = XMLDOMHelper.getChildElementsWithName(cdEle, "ImageData");
            for (var imgData : allImages) {
                imageDataMap.put(imgData.getAttribute("name"), decoder.decode(imgData.getTextContent()));
            }

            var allCustomDraw = XMLDOMHelper.getChildElementsWithName(cdEle, "CustomDrawing");
            for (var custom : allCustomDraw) {
                var customDrawingConfiguration = processCustomDrawing(custom);
                customDrawingMap.put(customDrawingConfiguration.getName(), customDrawingConfiguration);
            }
        }
        var mlEle = XMLDOMHelper.elementWithName(root, "MenuLayouts");
        var allMenuLayouts = XMLDOMHelper.getChildElementsWithName(mlEle, "MenuLayout");

        int rowsLoaded = 0;
        subMenuStores.clear();
        for (var layout : allMenuLayouts) {
            gridSize = Integer.parseInt(XMLDOMHelper.getAttributeOrDefault(layout, "cols", "0"));
            var globalFontInfo = FontInformation.fromWire(XMLDOMHelper.getAttributeOrDefault(layout, "fontInfo", "100%"));
            var recursive = Boolean.parseBoolean(XMLDOMHelper.getAttributeOrDefault(layout, "recursive", "false"));
            var rootItemId = Integer.parseInt(XMLDOMHelper.getAttributeOrDefault(layout, "rootId", "0"));
            var topLevelColorSet = colorSets.get(XMLDOMHelper.getAttributeOrDefault(layout, "colorSet", GlobalColorCustomizable.KEY_NAME));
            subMenuStores.put(rootItemId, new SubMenuStore(rootItemId, topLevelColorSet, globalFontInfo, recursive));
            changeSubStore(rootItemId);
            readTextElements(layout);
            readSpaceElements(layout);
            readMenuElements(layout);
            rowsLoaded++;
        }

        // now always make sure there is at least on ROOT item in the set
        int rootId = MenuTree.ROOT.getId();
        currentSubStore = subMenuStores.get(rootId);
        if (currentSubStore == null) {
            currentSubStore = new SubMenuStore(rootId, globalColorSet, FONT_100_PERCENT, true);
            subMenuStores.put(rootId, currentSubStore);
            gridSize = 4;
        }
    }

    private CustomDrawingConfiguration processCustomDrawing(Element custom) {
        String ty = custom.getAttribute("type");
        if(ty.equals("number")) {
            var ranges = XMLDOMHelper.transformElements(custom, "NumRange", element -> new NumericColorRange(
                    Double.parseDouble(element.getAttribute("start")), Double.parseDouble(element.getAttribute("end")),
                    new PortableColor(element.getAttribute("fg")), new PortableColor(element.getAttribute("bg")))
            );
            return new NumberCustomDrawingConfiguration(ranges, custom.getAttribute("name"));
        } else if(ty.equals("string")) {
            var map = new HashMap<String, ControlColor>();
            var strMap = XMLDOMHelper.getChildElementsWithName(custom, "StrMapping");
            for(var ele : strMap) {
                map.put(ele.getAttribute("value"), new ControlColor(
                        new PortableColor(ele.getAttribute("fg")), new PortableColor(ele.getAttribute("bg"))
                ));
            }
            return new StringCustomDrawingConfiguration(map, custom.getAttribute("name"));
        } else if(ty.equals("boolean")) {
            return new BooleanCustomDrawingConfiguration(
                    custom.getAttribute("name"),
                    new ControlColor(new PortableColor(custom.getAttribute("yesFgColor")), new PortableColor(custom.getAttribute("yesBgColor"))),
                    new ControlColor(new PortableColor(custom.getAttribute("noFgColor")), new PortableColor(custom.getAttribute("noBgColor")))
            );
        } else if(ty.equals("boolImg")) {
            return new BooleanCustomDrawingConfiguration(
                    custom.getAttribute("name"),
                    new ControlColor(new PortableColor(custom.getAttribute("yesFgColor")), new PortableColor(custom.getAttribute("yesBgColor"))),
                    new ControlColor(new PortableColor(custom.getAttribute("noFgColor")), new PortableColor(custom.getAttribute("noBgColor"))),
                    processImage(XMLDOMHelper.elementWithName(custom, "yesImg")),
                    processImage(XMLDOMHelper.elementWithName(custom, "noImg"))
            );
        } else return NO_CUSTOM_DRAWING;
    }

    private ImageDefinition processImage(Element imgEle) {
        if(imgEle == null) return new ImageDefinition(ImageLocation.NO_IMAGE, "");
        if(imgEle.getAttribute("type").equals("net")) {
            return new ImageDefinition(ImageLocation.NETWORK_URL, imgEle.getAttribute("ref"));
        } else if(imgEle.getAttribute("type").equals("png")) {
            return new ImageDefinition(ImageLocation.PNG_EMBEDDED_BASE64, imgEle.getAttribute("ref"));
        } else if(imgEle.getAttribute("type").equals("svg")) {
            return new ImageDefinition(ImageLocation.SVG_EMBEDDED_BASE64, imgEle.getAttribute("ref"));
        } else {
            return new ImageDefinition(ImageLocation.NO_IMAGE, "");
        }
    }

    private byte[] getEmbeddedImageData(ImageDefinition definition) {
        if(definition.imageType() == ImageLocation.NETWORK_URL) throw new IllegalArgumentException("wrong image type");
        return imageDataMap.get(definition.urlOrName());
    }

    private Collection<String> getAllEmbeddedImageNames() {
        return imageDataMap.keySet();
    }

    private void readTextElements(Element layout) {
        for (var text : XMLDOMHelper.getChildElementsWithName(layout, "StaticText")) {
            var textForm = new TextFormItem(
                    text.getTextContent().trim(),
                    colorSets.get(XMLDOMHelper.getAttributeOrDefault(text, "colorSet", GlobalColorCustomizable.KEY_NAME)),
                    ComponentPositioning.fromWire(XMLDOMHelper.getAttributeOrDefault(text, "position", "0,0")),
                    PortableAlignment.valueOf(XMLDOMHelper.getAttributeOrDefault(text, "alignment", "LEFT"))
            );
            textForm.setFontInfo(FontInformation.fromWire(XMLDOMHelper.getAttributeOrDefault(text, "fontInfo", "100%")));
            setFormItemAt(textForm.getPositioning().getRow(), textForm.getPositioning().getCol(), textForm);
        }
    }

    private void readSpaceElements(Element layout) {
        for (var text : XMLDOMHelper.getChildElementsWithName(layout, "VertSpace")) {
            var spaceForm = new SpaceFormItem(
                    colorSets.get(XMLDOMHelper.getAttributeOrDefault(text, "colorSet", GlobalColorCustomizable.KEY_NAME)),
                    ComponentPositioning.fromWire(XMLDOMHelper.getAttributeOrDefault(text, "position", "0,0")),
                    Integer.parseInt(XMLDOMHelper.getAttributeOrDefault(text, "height", "10"))
            );

            setFormItemAt(spaceForm.getPositioning().getRow(), spaceForm.getPositioning().getCol(), spaceForm);
        }
    }

    private void readMenuElements(Element layout) {
        for (var text : XMLDOMHelper.getChildElementsWithName(layout, "MenuElement")) {
            var menuForm = new MenuItemFormItem(
                    tree.getMenuById(Integer.parseInt(XMLDOMHelper.getAttributeOrDefault(text, "menuId", "0"))).orElseThrow(),
                    colorSets.get(XMLDOMHelper.getAttributeOrDefault(text, "colorSet", GlobalColorCustomizable.KEY_NAME)),
                    ComponentPositioning.fromWire(XMLDOMHelper.getAttributeOrDefault(text, "position", "0,0")),
                    ControlType.valueOf(XMLDOMHelper.getAttributeOrDefault(text, "controlType", "TEXT_CONTROL")),
                    PortableAlignment.valueOf(XMLDOMHelper.getAttributeOrDefault(text, "alignment", "LEFT")),
                    RedrawingMode.valueOf(XMLDOMHelper.getAttributeOrDefault(text, "drawMode", "SHOW_NAME_VALUE")),
                    customDrawingMap.get(XMLDOMHelper.getAttributeOrDefault(text, "customDraw", "none"))
            );

            menuForm.setFontInfo(FontInformation.fromWire(XMLDOMHelper.getAttributeOrDefault(text, "fontInfo", "100%")));
            setFormItemAt(menuForm.getPositioning().getRow(), menuForm.getPositioning().getCol(), menuForm);
        }
    }

    private static void loadSingleColorValue(Element eleParent, ColorCustomizable cc, String elementName, ColorComponentType compTy) {
        var ce = XMLDOMHelper.elementWithName(eleParent, elementName);
        if (ce != null && Boolean.valueOf(ce.getAttribute("isPresent"))) {
            cc.setColorFor(compTy, new ControlColor(
                    new PortableColor(ce.getAttribute("fg")),
                    new PortableColor(ce.getAttribute("bg"))
            ));
        }
    }

    public String getLayoutXmlString(UUID uuid) throws IOException {
        try (var output = new ByteArrayOutputStream()) {
            Document doc = XMLDOMHelper.newDocumentRoot("EmbedControl");
            doc.getDocumentElement().setAttribute("boardUuid", String.valueOf(uuid));
            doc.getDocumentElement().setAttribute("layoutName", layoutName);

            var subsSettings = XMLDOMHelper.appendElementWithNameValue(doc.getDocumentElement(), "MenuLayouts", null);

            for (var subStore : subMenuStores.values()) {
                var subSetting = XMLDOMHelper.appendElementWithNameValue(subsSettings, "MenuLayout", null);

                subSetting.setAttribute("rootId", String.valueOf(subStore.getSubId()));
                subSetting.setAttribute("recursive", String.valueOf(subStore.isRecursive()));
                subSetting.setAttribute("cols", String.valueOf(gridSize));
                subSetting.setAttribute("fontInfo", subStore.getFontInfo().toWire());
                for (var mfi : subStore.allFormEntries()) {
                    if (mfi instanceof MenuItemFormItem menuFormItem) {
                        serializeMenuItem(subSetting, menuFormItem);
                    } else if (mfi instanceof TextFormItem textFormItem) {
                        serializeTextItem(subSetting, textFormItem);
                    } else if (mfi instanceof SpaceFormItem spaceItem) {
                        var itemEle = XMLDOMHelper.appendElementWithNameValue(subSetting, "VertSpace", null);
                        itemEle.setAttribute("height", Integer.toString(spaceItem.getVerticalSpace()));
                        itemEle.setAttribute("position", spaceItem.getPositioning().toWire());
                    }
                }
            }

            var colEles = XMLDOMHelper.appendElementWithNameValue(doc.getDocumentElement(), "ColorSets", null);
            for (var colorSet : colorSets.values()) {
                // skip the global color scheme.
                if (colorSet.getColorSchemeName().equals(GlobalColorCustomizable.KEY_NAME)) continue;

                var colEle = XMLDOMHelper.appendElementWithNameValue(colEles, "ColorSet", null);
                colEle.setAttribute("name", colorSet.getColorSchemeName());
                saveControlColor(colEle, "text", colorSet, TEXT_FIELD);
                saveControlColor(colEle, "button", colorSet, BUTTON);
                saveControlColor(colEle, "highlight", colorSet, HIGHLIGHT);
                saveControlColor(colEle, "error", colorSet, ERROR);
                saveControlColor(colEle, "custom", colorSet, CUSTOM);
                saveControlColor(colEle, "dialog", colorSet, DIALOG);
                saveControlColor(colEle, "pending", colorSet, PENDING);
            }

            var customEles = XMLDOMHelper.appendElementWithNameValue(doc.getDocumentElement(), "CustomDrawings", null);
            for(var custom : customDrawingMap.values()) {
                if(custom instanceof NoOpCustomDrawingConfiguration) continue; // do not save NoOp entry.
                saveCustomDrawing(custom, customEles);
            }

            var encoder = Base64.getEncoder();
            for(var imgData : imageDataMap.entrySet()) {
                var img = XMLDOMHelper.appendElementWithNameValue(customEles, "CustomDrawings", encoder.encode(imgData.getValue()));
                img.setAttribute("name", imgData.getKey());
            }

            XMLDOMHelper.writeXml(doc, output, true);
            return output.toString();
        } catch (Exception e) {
            logger.log(System.Logger.Level.ERROR, "Unable to save state", e);
            throw new IOException("Failed to write XML structure", e);
        }
    }

    private void saveCustomDrawing(CustomDrawingConfiguration custom, Element el) {
        var cd = XMLDOMHelper.appendElementWithNameValue(el, "CustomDrawing", null);
        cd.setAttribute("name", custom.getName());
        if(custom instanceof StringCustomDrawingConfiguration strCustom) {
            cd.setAttribute("type", "string");
            for(var mapping : strCustom.getAllMappings().entrySet()) {
                var sr = XMLDOMHelper.appendElementWithNameValue(cd, "StrMapping", null);
                sr.setAttribute("value", mapping.getKey());
                sr.setAttribute("fg", mapping.getValue().getFg().toString());
                sr.setAttribute("bg", mapping.getValue().getBg().toString());
            }
        } else if(custom instanceof NumberCustomDrawingConfiguration numCustom) {
            cd.setAttribute("type", "number");
            for(var ranges : numCustom.getColorRanges()) {
                var sr = XMLDOMHelper.appendElementWithNameValue(cd, "NumRange", null);
                sr.setAttribute("start", String.valueOf(ranges.start()));
                sr.setAttribute("end", String.valueOf(ranges.end()));
                sr.setAttribute("fg", ranges.fg().toString());
                sr.setAttribute("bg", ranges.bg().toString());
            }
        } else if(custom instanceof BooleanCustomDrawingConfiguration boolCustom) {
            var yesCol = boolCustom.getColorFor(true).orElseThrow();
            var noCol = boolCustom.getColorFor(false).orElseThrow();
            cd.setAttribute("type", boolCustom.hasImages() ? "boolImg" : "boolean");
            cd.setAttribute("yesFgColor", yesCol.getFg().toString());
            cd.setAttribute("yesBgColor", yesCol.getBg().toString());
            cd.setAttribute("noFgColor", noCol.getFg().toString());
            cd.setAttribute("noBgColor", noCol.getBg().toString());

            var yesImg = boolCustom.getImageFor(true);
            if(yesImg.imageType() != ImageLocation.NO_IMAGE) {
                var cdy = XMLDOMHelper.appendElementWithNameValue(cd, "yesImg", null);
                cdy.setAttribute("type", toImgTypeWire(yesImg));
                cdy.setAttribute("ref", yesImg.urlOrName());
            }
            var noImg = boolCustom.getImageFor(false);
            if(noImg.imageType() != ImageLocation.NO_IMAGE) {
                var cdn = XMLDOMHelper.appendElementWithNameValue(cd, "noImg", null);
                cdn.setAttribute("type", toImgTypeWire(yesImg));
                cdn.setAttribute("ref", yesImg.urlOrName());
            }
        }
    }

    private String toImgTypeWire(ImageDefinition yesImg) {
        return switch(yesImg.imageType()) {
            case NO_IMAGE -> throw new UnsupportedOperationException("NO_IMAGE cannot be written");
            case NETWORK_URL -> "url";
            case PNG_EMBEDDED_BASE64 -> "svg";
            case SVG_EMBEDDED_BASE64 -> "png";
        };
    }


    private void serializeTextItem(Element subSetting, TextFormItem textFormItem) {
        var itemEle = XMLDOMHelper.appendElementWithNameValue(subSetting, "StaticText", textFormItem.getText());
        itemEle.setAttribute("position", String.valueOf(textFormItem.getPositioning().toWire()));
        itemEle.setAttribute("alignment", String.valueOf(textFormItem.getAlignment()));
        itemEle.setAttribute("colorSet", String.valueOf(textFormItem.getSettings().getColorSchemeName()));
        itemEle.setAttribute("fontInfo", textFormItem.getFontInfo().toWire());
    }

    private void saveControlColor(Element ele, String colorName, ColorCustomizable colorSet, ColorComponentType colType) {
        var colorElement = XMLDOMHelper.appendElementWithNameValue(ele, colorName, null);
        if (colorSet.getColorStatus(colType) == ColorCustomizable.ColorStatus.AVAILABLE) {
            colorElement.setAttribute("isPresent", "true");
            colorElement.setAttribute("fg", colorSet.getColorFor(colType).getFg().toString());
            colorElement.setAttribute("bg", colorSet.getColorFor(colType).getBg().toString());
        } else {
            colorElement.setAttribute("isPresent", "false");
        }
    }

    void serializeMenuItem(Element element, MenuItemFormItem menuFormItem) {
        logger.log(System.Logger.Level.INFO, "Persisting settings for " + menuFormItem.getDescription());
        var itemEle = XMLDOMHelper.appendElementWithNameValue(element, "MenuElement", null);
        itemEle.setAttribute("menuId", String.valueOf(menuFormItem.getItem().getId()));
        itemEle.setAttribute("position", String.valueOf(menuFormItem.getPositioning().toWire()));
        itemEle.setAttribute("alignment", menuFormItem.getAlignment().toString());
        itemEle.setAttribute("drawMode", menuFormItem.getRedrawingMode().toString());
        itemEle.setAttribute("controlType", menuFormItem.getControlType().toString());
        itemEle.setAttribute("fontInfo", menuFormItem.getFontInfo().toWire());
        itemEle.setAttribute("colorSet", String.valueOf(menuFormItem.getSettings().getColorSchemeName()));
        itemEle.setAttribute("customDraw", String.valueOf(menuFormItem.getCustomDrawing().getName()));
    }

    public FontInformation getGlobalFontInfo() {
        return currentSubStore.getFontInfo();
    }

    public void setGlobalFontInfo(FontInformation globalFontInfo) {
        this.currentSubStore.setFontInfo(globalFontInfo);
    }

    public boolean hasSubConfiguration(int id) {
        if (!subMenuStores.containsKey(id)) return false;
        return (subMenuStores.get(id).allFormEntries().stream().anyMatch(mfi -> !(mfi instanceof NoFormItem)));
    }

    public List<MenuFormItem> allRowEntries() {
        return currentSubStore.allFormEntries();
    }

    public Collection<CustomDrawingConfiguration> getCustomDrawingElements() {
        return customDrawingMap.values();
    }

    public void addUpdateCustomDrawing(CustomDrawingConfiguration customDrawingConfiguration) {
        customDrawingMap.put(customDrawingConfiguration.getName(), customDrawingConfiguration);
    }

    protected class RowEntry {
        private MenuFormItem[] items = new MenuFormItem[gridSize];

        public RowEntry() {
            Arrays.fill(items, NO_FORM_ITEM);
        }

        void resizeTo(int cols) {
            // keep a copy of old data to copy as much as we can into new array
            var oldData = items;
            int oldCols = items.length;
            int toFill = Math.min(oldCols, cols);

            // create an empty array the right size
            items = new MenuFormItem[cols];
            Arrays.fill(items, NO_FORM_ITEM);

            // and copy over as much as we can without overflowing either array
            for (int i = 0; i < toFill; i++) {
                items[i] = oldData[i];
            }
        }

        void setAtPosition(int pos, MenuFormItem item) {
            items[pos] = item;
        }

        MenuFormItem getAtPosition(int i) {
            return items[i];
        }
    }

    public MenuTree getTree() {
        return tree;
    }

    class SubMenuStore {
        private final int subId;
        private final HashMap<Integer, RowEntry> rowEntries = new HashMap<>(128);
        private ColorCustomizable colorSet;
        private FontInformation fontInfo;
        private boolean recursive;

        public SubMenuStore(int subId, ColorCustomizable initialColorSet, FontInformation fontInfo, boolean recursive) {
            this.subId = subId;
            this.fontInfo = fontInfo;
            this.colorSet = initialColorSet;
            this.recursive = recursive;
            for (int r = 0; r < initialRows; r++) {
                rowEntries.put(r, new RowEntry());
            }
        }

        public ColorCustomizable getColorSet() {
            return colorSet;
        }

        public void setColorSet(ColorCustomizable colorSet) {
            this.colorSet = colorSet;
        }

        public FontInformation getFontInfo() {
            return fontInfo;
        }

        public void setFontInfo(FontInformation fontInfo) {
            this.fontInfo = fontInfo;
        }

        public int getSubId() {
            return subId;
        }

        public HashMap<Integer, RowEntry> getRowEntries() {
            return rowEntries;
        }

        public boolean isRecursive() {
            return recursive;
        }

        public void setRecursive(boolean recursive) {
            this.recursive = recursive;
        }

        public List<MenuFormItem> allFormEntries() {
            var l = new ArrayList<MenuFormItem>(128);
            for (var ent : rowEntries.values()) {
                for (var it : ent.items) {
                    if (it != null && !(it instanceof NoFormItem)) {
                        l.add(it);
                    }
                }
            }
            return l.stream().sorted(Comparator.comparingInt(menuFormItem -> menuFormItem.getPositioning().getRow())).toList();
        }

    }
}
