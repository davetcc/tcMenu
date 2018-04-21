package com.thecoderscorner.menu.editorui.uimodel;

import com.thecoderscorner.menu.domain.*;
import com.thecoderscorner.menu.domain.state.MenuTree;
import com.thecoderscorner.menu.domain.util.AbstractMenuItemVisitor;
import com.thecoderscorner.menu.domain.util.MenuItemHelper;
import com.thecoderscorner.menu.editorui.project.MenuIdChooser;
import com.thecoderscorner.menu.editorui.project.MenuIdChooserImpl;

import java.util.Optional;
import java.util.function.BiConsumer;

public class UIEditorFactory {
    public static Optional<UIMenuItem> createPanelForMenuItem(MenuItem menuItem, MenuTree tree, BiConsumer<MenuItem, MenuItem> changeConsumer) {
        RenderingChooserVisitor renderingChooserVisitor = new RenderingChooserVisitor(changeConsumer, tree);
        return MenuItemHelper.visitWithResult(menuItem, renderingChooserVisitor);
    }

    static class RenderingChooserVisitor extends AbstractMenuItemVisitor<UIMenuItem> {

        private final BiConsumer<MenuItem,MenuItem> changeConsumer;
        private final MenuIdChooser menuIdChooser;

        RenderingChooserVisitor(BiConsumer<MenuItem, MenuItem> changeConsumer, MenuTree tree) {
            this.changeConsumer = changeConsumer;
            this.menuIdChooser = new MenuIdChooserImpl(tree);
        }

        @Override
        public void visit(AnalogMenuItem item) {
            setResult(new UIAnalogMenuItem(item, menuIdChooser, changeConsumer));
        }

        @Override
        public void visit(EnumMenuItem item) {
            setResult(new UIEnumMenuItem(item, menuIdChooser, changeConsumer));
        }

        @Override
        public void visit(BooleanMenuItem item) {
            setResult(new UIBooleanMenuItem(item, menuIdChooser, changeConsumer));
        }

        @Override
        public void visit(SubMenuItem item) {
            if(!MenuTree.ROOT.equals(item)) {
                setResult(new UISubMenuItem(item, menuIdChooser, changeConsumer));
            }
        }
    }
}
