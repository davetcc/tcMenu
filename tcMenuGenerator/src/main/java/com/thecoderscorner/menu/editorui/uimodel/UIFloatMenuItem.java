/*
 * Copyright (c)  2016-2019 https://www.thecoderscorner.com (Dave Cherry).
 * This product is licensed under an Apache license, see the LICENSE file in the top-level directory.
 *
 */

package com.thecoderscorner.menu.editorui.uimodel;

import com.thecoderscorner.menu.domain.FloatMenuItem;
import com.thecoderscorner.menu.domain.FloatMenuItemBuilder;
import com.thecoderscorner.menu.domain.MenuItem;
import com.thecoderscorner.menu.domain.util.MenuItemHelper;
import com.thecoderscorner.menu.editorui.generator.core.VariableNameGenerator;
import com.thecoderscorner.menu.editorui.project.MenuIdChooser;
import com.thecoderscorner.menu.editorui.util.StringHelper;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

public class UIFloatMenuItem extends UIMenuItem<FloatMenuItem> {

    private TextField decimalPlaces;
    private TextField defaultValueField;

    public UIFloatMenuItem(FloatMenuItem menuItem, MenuIdChooser chooser, VariableNameGenerator gen, BiConsumer<MenuItem, MenuItem> changeConsumer) {
        super(menuItem, chooser, gen, changeConsumer, UrlsForDocumentation.FLOAT_URL);
    }

    @Override
    protected Optional<FloatMenuItem> getChangedMenuItem() {
        List<FieldError> errors = new ArrayList<>();

        int dp = safeIntFromProperty(decimalPlaces.textProperty(), "Decimal Places", errors, 1, 6);
        FloatMenuItemBuilder builder = FloatMenuItemBuilder.aFloatMenuItemBuilder()
                .withExisting(getMenuItem())
                .withDecimalPlaces(dp);
        getChangedDefaults(builder, errors);

        try {
            String text = defaultValueField.getText();
            var value = StringHelper.isStringEmptyOrNull(text) ? 0 : Float.parseFloat(text);
            if (Float.isNaN(value)) {
                errors.add(new FieldError("Value can't be NaN", "DefaultValue"));
            } else {
                MenuItemHelper.setMenuState(getMenuItem(), value, menuTree);
            }
        } catch(Exception ex) {
            errors.add(new FieldError("Value could not be parsed " + ex.getClass().getSimpleName() + " " + ex.getMessage(), "DefaultValue"));
        }

        return getItemOrReportError(builder.menuItem(), errors);
    }

    @Override
    protected int internalInitPanel(GridPane grid, int idx) {
        idx++;
        grid.add(new Label("Decimal Places"), 0, idx);
        decimalPlaces = new TextField(String.valueOf(getMenuItem().getNumDecimalPlaces()));
        decimalPlaces.textProperty().addListener(this::coreValueChanged);
        decimalPlaces.setId("decimalPlacesField");
        TextFormatterUtils.applyIntegerFormatToField(decimalPlaces);
        grid.add(decimalPlaces, 1, idx);

        idx++;
        grid.add(new Label("Default value"), 0, idx);
        var value = MenuItemHelper.getValueFor(getMenuItem(), menuTree, 0.0F);
        defaultValueField = new TextField(Float.toString(value));
        defaultValueField.textProperty().addListener(e -> callChangeConsumer());
        TextFormatterUtils.applyFormatToField(defaultValueField, TextFormatterUtils.FLOAT_MATCH);
        grid.add(defaultValueField, 1, idx);
        return idx;
    }
}
