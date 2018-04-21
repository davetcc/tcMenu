package com.thecoderscorner.menu.domain.util;

import com.thecoderscorner.menu.domain.*;
import org.junit.Test;

import static com.thecoderscorner.menu.domain.BooleanMenuItem.*;
import static com.thecoderscorner.menu.domain.DomainFixtures.*;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class AbstractMenuItemVisitorTest {

    @Test
    public void testAnyItemCall() {
        SubMenuItem subItem = aSubMenu("123", 1);

        AbstractMenuItemVisitor<Integer> visitor = new AbstractMenuItemVisitor<Integer>() {
            @Override
            public void anyItem(MenuItem item) {
                setResult(1);
            }
        };
        subItem.accept(visitor);
        assertThat(visitor.getResult().orElse(0), is(1));
    }

    @Test
    public void testAllOutrightCall() {
        AbstractMenuItemVisitor<String> visitor = new AbstractMenuItemVisitor<String>() {
            @Override
            public void visit(SubMenuItem item) {
                setResult(getResult().orElse("") + "1");
            }

            @Override
            public void visit(AnalogMenuItem item) {
                setResult(getResult().orElse("") + "2");
            }

            @Override
            public void visit(EnumMenuItem item) {
                setResult(getResult().orElse("") + "3");
            }

            @Override
            public void visit(BooleanMenuItem item) {
                setResult(getResult().orElse("") + "4");
            }
        };

        AnalogMenuItem analog = anAnalogItem("123", 1);
        SubMenuItem subItem = aSubMenu("321", 2);
        EnumMenuItem enumItem = anEnumItem("111", 3);
        BooleanMenuItem boolItem = aBooleanMenu("222", 2, BooleanNaming.TRUE_FALSE);

        subItem.accept(visitor);
        analog.accept(visitor);
        enumItem.accept(visitor);
        boolItem.accept(visitor);

        assertThat(visitor.getResult().orElse(""), is("1234"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testExceptionOnUnimplementedCase() {
        AbstractMenuItemVisitor<String> visitor = new AbstractMenuItemVisitor<String>() {
            // intentionally empty
        };
        SubMenuItem item = aSubMenu("123", 1);
        item.accept(visitor);
    }
}