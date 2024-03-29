package com.thecoderscorner.menu.domain.state;

import java.util.Objects;

/**
 * A portable color that represents a color in the RGBA space with single byte values for each entry (between 0..255).
 * It can convert to and from web color format strings.
 */
public class PortableColor {
    public static final PortableColor BLACK = new PortableColor(0, 0, 0);
    public static final PortableColor WHITE = new PortableColor(255, 255, 255);
    private final int red;
    private final int green;
    private final int blue;
    private final int alpha;

    /**
     * Create a color from RGB with alpha set to full (255), each value from 0 to 255
     *
     * @param red   the red component
     * @param green the green component
     * @param blue  the blue component
     */
    public PortableColor(int red, int green, int blue) {
        this(red, green, blue, 255);
    }

    /**
     * Create a color from RGBA using values from 0 to 255
     *
     * @param red   the red component
     * @param green the green component
     * @param blue  the blue component
     * @param alpha the alpha
     */
    public PortableColor(int red, int green, int blue, int alpha) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }

    /**
     * Create a color object from a web color code such as #FFFFFF
     *
     * @param htmlCode the html code
     */
    public PortableColor(String htmlCode) {
        if(htmlCode == null) {
            red = green = blue = alpha = 0;
            return;
        }
        if (htmlCode.startsWith("#") && htmlCode.length() == 4) {
            red = (parseHex(htmlCode.charAt(1)) << 4);
            green = (parseHex(htmlCode.charAt(2)) << 4);
            blue = (parseHex(htmlCode.charAt(3)) << 4);
            alpha = 255;
            return;
        }
        if (htmlCode.startsWith("#") && htmlCode.length() >= 7) {
            red = ((parseHex(htmlCode.charAt(1)) << 4) + parseHex(htmlCode.charAt(2)));
            green = ((parseHex(htmlCode.charAt(3)) << 4) + parseHex(htmlCode.charAt(4)));
            blue = ((parseHex(htmlCode.charAt(5)) << 4) + parseHex(htmlCode.charAt(6)));
            if (htmlCode.length() == 9) {
                alpha = ((parseHex(htmlCode.charAt(7)) << 4) + parseHex(htmlCode.charAt(8)));
            } else alpha = 255;
            return;
        }

        red = green = blue = 0;
        alpha = 255;
    }

    private static int parseHex(char val) {
        if (val >= '0' && val <= '9') return (short) (val - '0');
        val = Character.toUpperCase(val);
        if (val >= 'A' && val <= 'F') return (short) (val - ('A' - 10));
        return 0;
    }

    /**
     * @return red component between 0 and 255
     */
    public int getRed() {
        return red;
    }

    /**
     * @return green component between 0 and 255
     */
    public int getGreen() {
        return green;
    }

    /**
     * @return blue component between 0 and 255
     */
    public int getBlue() {
        return blue;
    }

    /**
     * @return alpha component between 0 and 255
     */
    public int getAlpha() {
        return alpha;
    }

    @Override
    public String toString() {
        return String.format("#%02X%02X%02X%02X", red, green, blue, alpha);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PortableColor that = (PortableColor) o;
        return red == that.red &&
                green == that.green &&
                blue == that.blue &&
                alpha == that.alpha;
    }

    public int asArgb() {
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    @Override
    public int hashCode() {
        return Objects.hash(red, green, blue, alpha);
    }

    public String toHtml() {
        return String.format("#%02X%02X%02X", red, green, blue);
    }

    public static PortableColor asPortableColor(int argb) {
        int b = (argb & 0xff);
        int g = ((argb) >>> 8) & 0xFF;
        int r = ((argb) >>> 16) & 0xFF;
        int a = ((argb) >>> 24) & 0xFF;
        return new PortableColor(r, g, b, a);
    }

    public PortableColor applyAlphaChannel() {
        double al = alpha / 255.0;
        double r = (red / 255.0) * al;
        double g = (green / 255.0) * al;
        double b = (blue / 255.0) * al;

        return new PortableColor((int) (r * 255), (int) (g * 255), (int) (b * 255), 255);
    }
}
