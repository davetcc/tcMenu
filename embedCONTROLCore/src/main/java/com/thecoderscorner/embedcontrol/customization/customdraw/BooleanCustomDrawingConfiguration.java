package com.thecoderscorner.embedcontrol.customization.customdraw;

import com.thecoderscorner.embedcontrol.core.controlmgr.color.ControlColor;
import com.thecoderscorner.menu.domain.ActionMenuItem;
import com.thecoderscorner.menu.domain.BooleanMenuItem;
import com.thecoderscorner.menu.domain.MenuItem;
import com.thecoderscorner.menu.domain.SubMenuItem;

import java.util.Optional;

public class BooleanCustomDrawingConfiguration implements CustomDrawingConfiguration {
    private String name;
    private ControlColor yesColor;
    private ControlColor noColor;
    private ImageDefinition yesImage;
    private ImageDefinition noImage;

    public BooleanCustomDrawingConfiguration(String name, ControlColor yesColor, ControlColor noColor) {
        this.name = name;
        this.yesColor = yesColor;
        this.noColor = noColor;
        this.yesImage = ImageDefinition.NO_IMAGE;
        this.noImage = ImageDefinition.NO_IMAGE;
    }

    public BooleanCustomDrawingConfiguration(String name, ControlColor yesColor, ControlColor noColor, ImageDefinition yesImage, ImageDefinition noImage) {
        this.name = name;
        this.yesColor = yesColor;
        this.noColor = noColor;
        this.yesImage = yesImage != null ? yesImage : ImageDefinition.NO_IMAGE;
        this.noImage = noImage != null ? noImage : ImageDefinition.NO_IMAGE;
    }

    @Override
    public boolean isSupportedFor(MenuItem item) {
        return item instanceof BooleanMenuItem || item instanceof SubMenuItem || item instanceof ActionMenuItem;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Optional<ControlColor> getColorFor(Object val) {
        if(!(val instanceof Boolean boolVal)) return Optional.empty();
        return Optional.ofNullable(boolVal ? yesColor : noColor);
    }

    public ImageDefinition getImageFor(boolean val) {
        return val ? yesImage : noImage;
    }

    public boolean hasImages() {
        return yesImage != null || noImage != null;
    }

    @Override
    public String toString() {
        return name + " for boolean";
    }

    public enum ImageLocation { NO_IMAGE, SVG_EMBEDDED_BASE64, PNG_EMBEDDED_BASE64, NETWORK_URL}
    public record ImageDefinition(ImageLocation imageType, String urlOrName) {
        public static final ImageDefinition NO_IMAGE = new ImageDefinition(ImageLocation.NO_IMAGE, "");
    }
}
