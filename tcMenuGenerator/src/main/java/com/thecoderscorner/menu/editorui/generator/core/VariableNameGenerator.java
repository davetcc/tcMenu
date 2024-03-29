/*
 * Copyright (c)  2016-2021 https://www.thecoderscorner.com (Dave Cherry).
 * This product is licensed under an Apache license, see the LICENSE file in the top-level directory.
 *
 */

package com.thecoderscorner.menu.editorui.generator.core;

import com.thecoderscorner.menu.domain.MenuItem;
import com.thecoderscorner.menu.domain.state.MenuTree;
import com.thecoderscorner.menu.editorui.generator.arduino.MenuItemToEmbeddedGenerator;
import com.thecoderscorner.menu.editorui.util.StringHelper;

import java.util.*;
import java.util.stream.Collectors;

public class VariableNameGenerator {
    private final MenuTree menuTree;
    private final boolean recursiveNaming;
    private final Set<Integer> uncommittedItems;

    public VariableNameGenerator(MenuTree menuTree, boolean recursiveNaming) {
        this.menuTree = menuTree;
        this.recursiveNaming = recursiveNaming;
        this.uncommittedItems = Set.of();
    }

    public VariableNameGenerator(MenuTree menuTree, boolean recursiveNaming, Set<Integer> uncommittedItems) {
        this.menuTree = menuTree;
        this.recursiveNaming = recursiveNaming;
        this.uncommittedItems = uncommittedItems;
    }

    public Set<Integer> getUncommittedItems() {
        return uncommittedItems;
    }

    public String makeNameToVar(MenuItem item) {
        return makeNameToVar(item, null);
    }

    public String makeNameToVar(MenuItem item, String newName) {
        // shortcut for null..
        if (item == null) return MenuItemToEmbeddedGenerator.CPP_NULL_PTR;
        if (newName == null && !StringHelper.isStringEmptyOrNull(item.getVariableName())) return item.getVariableName();
        var name = makeNameFromVariable((newName != null) ? newName : item.getName());

        // Either non-recursive names, or ROOT level.
        var parent = menuTree.findParent(item);
        if (!recursiveNaming || parent == null || parent.equals(MenuTree.ROOT)) {
            return makeNameFromVariable(name);
        }

        // if the parent name is already overridden, then the name is parentvariable + itemName
        if(!StringHelper.isStringEmptyOrNull(parent.getVariableName())) {
            return parent.getVariableName() + name;
        }

        // otherwise we need to do it the hard way, be recursively applying.
        var items = new ArrayList<String>();
        var par = item;
        while (par != null && !par.equals(MenuTree.ROOT)) {
            items.add(name);
            par = menuTree.findParent(par);
            if(par != null) {
                name = makeNameFromVariable(StringHelper.isStringEmptyOrNull(par.getVariableName()) ? par.getName() : par.getVariableName());
            }
        }

        // reverse and then join.
        Collections.reverse(items);
        return String.join("", items);

    }

    public String makeRtFunctionName(MenuItem item) {
        return "fn" + makeNameToVar(item) + "RtCall";
    }

    public static String makeNameFromVariable(String name) {
        Collection<String> parts = Arrays.asList(name.split("[\\p{P}\\p{Z}\\t\\r\\n\\v\\f^]+"));
        return parts.stream().map(StringHelper::capitaliseFirst).collect(Collectors.joining());
    }

}
