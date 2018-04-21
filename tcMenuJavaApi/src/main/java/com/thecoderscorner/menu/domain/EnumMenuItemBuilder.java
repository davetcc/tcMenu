package com.thecoderscorner.menu.domain;

import java.util.ArrayList;
import java.util.List;

public class EnumMenuItemBuilder extends MenuItemBuilder<EnumMenuItemBuilder> {

    private List<String> enumList = new ArrayList<>();

    @Override
    EnumMenuItemBuilder getThis() {
        return this;
    }

    public EnumMenuItemBuilder addEnumValue(String enumValue) {
        this.enumList.add(enumValue);
        return getThis();
    }

    public EnumMenuItemBuilder withEnumList(List<String> enumList) {
        this.enumList = enumList;
        return getThis();
    }

    public EnumMenuItemBuilder withExisting(EnumMenuItem item) {
        baseFromExisting(item);
        this.enumList = item.getEnumEntries();
        return getThis();
    }

    public EnumMenuItem menuItem() {
        return new EnumMenuItem(this.name, this.id, this.eepromAddr, this.functionName, this.enumList);
    }

    public static EnumMenuItemBuilder anEnumMenuItemBuilder() {
        return new EnumMenuItemBuilder();
    }
}
