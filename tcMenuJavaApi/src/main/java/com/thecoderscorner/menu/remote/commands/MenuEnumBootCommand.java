/*
 * Copyright (c)  2016-2019 https://www.thecoderscorner.com (Dave Cherry).
 * This product is licensed under an Apache license, see the LICENSE file in the top-level directory.
 *
 */

package com.thecoderscorner.menu.remote.commands;

import com.thecoderscorner.menu.domain.EnumMenuItem;
import com.thecoderscorner.menu.domain.state.AnyMenuState;
import com.thecoderscorner.menu.domain.util.MenuItemHelper;
import com.thecoderscorner.menu.remote.protocol.MessageField;

public class MenuEnumBootCommand extends BootItemMenuCommand<EnumMenuItem, Integer> {

    public MenuEnumBootCommand(int subMenuId, EnumMenuItem menuItem, int currentVal) {
        super(subMenuId, menuItem, currentVal);
    }

    @Override
    public MessageField getCommandType() {
        return MenuCommandType.ENUM_BOOT_ITEM;
    }

    @Override
    public AnyMenuState internalNewMenuState(AnyMenuState oldState) {
        boolean changed = !(oldState.getValue().equals(getCurrentValue()));
        return MenuItemHelper.stateForMenuItem(getMenuItem(), getCurrentValue(), changed, oldState.isActive());
    }
}
