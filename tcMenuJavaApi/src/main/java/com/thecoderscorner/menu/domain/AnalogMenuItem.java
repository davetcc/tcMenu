/*
 * Copyright (c)  2016-2019 https://www.thecoderscorner.com (Dave Cherry).
 * This product is licensed under an Apache license, see the LICENSE file in the top-level directory.
 *
 */

package com.thecoderscorner.menu.domain;

import com.thecoderscorner.menu.domain.util.MenuItemVisitor;

import java.util.Objects;

/**
 * Represents an analog (numeric) menu item, it is always a zero based integer when retrieved from storage, but it can
 * have an offset and divisor, so therefore is able to represent decimal values. The offset can also be negative. Step
 * allows the rate of change to be greater than 1 unit, but must be an exact divisor of the maximum value.
 * Rather than directly constructing an item of this type, you can use the AnalogMenuItemBuilder.
 */
public class AnalogMenuItem extends MenuItem {
    private final int maxValue;
    private final int offset;
    private final int divisor;
    private final String unitName;
    private final int step;

    public AnalogMenuItem() {
        super("", null, -1, -1, null, false, false, true, false);
        // needed for serialisation
        this.maxValue = -1;
        this.offset = -1;
        this.divisor = -1;
        this.step = 1;
        this.unitName = "";
    }

    public AnalogMenuItem(String name, String variableName, int id, int eepromAddress, String functionName, int maxValue,
                          int offset, int divisor, int step, String unitName, boolean readOnly, boolean localOnly, boolean visible, boolean staticInRAM) {
        super(name, variableName, id, eepromAddress, functionName, readOnly, localOnly, visible, staticInRAM);
        this.maxValue = maxValue;
        this.offset = offset;
        this.divisor = divisor;
        this.step = step;
        this.unitName = unitName != null ? unitName : "";
    }

    /**
     * The maximum value (0 based integer) that this item can represent
     * @return max value
     */
    public int getMaxValue() {
        return maxValue;
    }

    /**
     * The offset from 0 that is used when displaying the item, can be negative
     * @return the offset
     */
    public int getOffset() {
        return offset;
    }

    /**
     * The divisor used when displaying the item, for example value 50 with a divisor of 10 is 5.0
     * @return the divisor used
     */
    public int getDivisor() {
        return divisor;
    }

    /**
     * The step is the amount by which each increment should increase the value, it must be exactly divisible by
     * the maximum value. Default is 1 and the value can never be lower than 1.
     * @return the current step
     */
    public int getStep() {
        return Math.max(1, step);
    }

    /**
     * The unit name to appear directly after the value, for example a temperature item may be "oC"
     * where as a volume control could be "dB"
     * @return the name of the unit (if any)
     */
    public String getUnitName() {
        return unitName;
    }

    /**
     * See the MenuItemVisitor for more info.
     * @param visitor the item to be visited.
     */
    @Override
    public void accept(MenuItemVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnalogMenuItem that = (AnalogMenuItem) o;
        return getMaxValue() == that.getMaxValue() &&
                getOffset() == that.getOffset() &&
                getDivisor() == that.getDivisor() &&
                getStep() == that.getStep() &&
                Objects.equals(getName(), that.getName()) &&
                getId() == that.getId() &&
                getEepromAddress() == that.getEepromAddress() &&
                isReadOnly() == that.isReadOnly() &&
                isVisible() == that.isVisible() &&
                isLocalOnly() == that.isLocalOnly() &&
                isStaticDataInRAM() == that.isStaticDataInRAM() &&
                Objects.equals(getUnitName(), that.getUnitName()) &&
                Objects.equals(getFunctionName(), that.getFunctionName()) &&
                Objects.equals(getVariableName(), that.getVariableName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMaxValue(), getOffset(), getDivisor(), getStep(), getVariableName(), getUnitName(),
                            getId(), getEepromAddress(), getFunctionName(), isReadOnly(), isStaticDataInRAM());
    }
}
