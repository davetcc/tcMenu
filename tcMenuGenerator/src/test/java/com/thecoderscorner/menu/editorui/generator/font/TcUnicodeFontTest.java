package com.thecoderscorner.menu.editorui.generator.font;

import com.thecoderscorner.menu.editorui.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static com.thecoderscorner.menu.editorui.generator.font.TcUnicodeFontExporter.*;

class TcUnicodeFontTest {
    private TcUnicodeFontExporter font;

    @BeforeEach
    void setUp() {
        List<TcUnicodeFontGlyph> fontItems1 = List.of(
                new TcUnicodeFontGlyph(100, new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}, 16, 12, 16, 0, -18),
                new TcUnicodeFontGlyph(101, new byte[]{10, 11, 12, 13, 14, 15, 16, 17, 18, 19}, 15, 12, 14, 1, -12),
                new TcUnicodeFontGlyph(102, new byte[]{20, 21, 22, 23, 24, 25, 26, 27, 28, 29}, 16, 11, 13, 2, -1)
        );
        List<TcUnicodeFontGlyph> fontItems2 = List.of(
                new TcUnicodeFontGlyph(103, new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}, 10, 11, 16, 0, -10),
                new TcUnicodeFontGlyph(104, new byte[]{10, 21, 32, 43, 54, 65, 76, 87, 98, 90}, 4, 11, 16, 1, -11)
        );
        List<TcUnicodeFontBlock> fontBlocks = List.of(
                new TcUnicodeFontBlock(UnicodeBlockMapping.BASIC_LATIN, fontItems1),
                new TcUnicodeFontBlock(UnicodeBlockMapping.LATIN_EXTENDED_G, fontItems2)
        );
        font = new TcUnicodeFontExporter("myFont123", fontBlocks, 12);
    }

    @Test
    public void testUnicodeToAdafruit() {
        var os = new ByteArrayOutputStream();
        font.encodeFontToStream(os, FontFormat.ADAFRUIT);
        TestUtils.assertEqualsIgnoringCRLF("""
                // Font file generated by theCodersCorner.com Font Generator
                // Approximate font size: 106 bytes
                                
                const uint8_t myFont123Bitmaps[] PROGMEM = {
                0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x0a,0x0b,0x0c,0x0d,0x0e,0x0f,0x10,0x11,0x12,0x13,
                0x14,0x15,0x16,0x17,0x18,0x19,0x1a,0x1b,0x1c,0x1d,0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,
                0x0a,0x15,0x20,0x2b,0x36,0x41,0x4c,0x57,0x62,0x5a
                };
                                
                const GFXglyph myFont123Glyphs[] PROGMEM = {
                    { 0, 16, 12, 16, 0, -18 } /* [d] 100 */,
                    { 10, 15, 12, 14, 1, -12 } /* [e] 101 */,
                    { 20, 16, 11, 13, 2, -1 } /* [f] 102 */,
                    { 30, 10, 11, 16, 0, -10 } /* [g] 103 */,
                    { 40, 4, 11, 16, 1, -11 } /* [h] 104 */
                };
                                
                const GFXfont myFont123 PROGMEM = {
                    (uint8_t*)myFont123Bitmaps,
                    (GFXglyph*)myFont123Glyphs,
                    100, 104,
                    12
                };
                                
                """, os.toString());
    }

    @Test
    public void testUnicodeToTcUnicode() {
        var os = new ByteArrayOutputStream();
        font.encodeFontToStream(os, FontFormat.TC_UNICODE);
        TestUtils.assertEqualsIgnoringCRLF("""
                // Font file generated by theCodersCorner.com Font Generator
                // Approximate font size: 142 bytes
                                
                #include <UnicodeFontDefs.h>
                                
                // Bitmaps for Latin Extended-G
                const uint8_t myFont123Bitmaps_282[] PROGMEM = {
                0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x0a,0x15,0x20,0x2b,0x36,0x41,0x4c,0x57,0x62,0x5a
                                
                };
                                
                // Glyphs for Latin Extended-G
                const UnicodeFontGlyph myFont123Glyphs_282[] PROGMEM = {
                    { -122521, 0, 10, 11, 16, 0, -10} /* [g] 103*/ ,
                    { -122520, 10, 4, 11, 16, 1, -11} /* [h] 104*/\s
                };
                                
                // Bitmaps for Basic Latin
                const uint8_t myFont123Bitmaps_0[] PROGMEM = {
                0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x0a,0x0b,0x0c,0x0d,0x0e,0x0f,0x10,0x11,0x12,0x13,
                0x14,0x15,0x16,0x17,0x18,0x19,0x1a,0x1b,0x1c,0x1d
                };
                                
                // Glyphs for Basic Latin
                const UnicodeFontGlyph myFont123Glyphs_0[] PROGMEM = {
                    { 100, 0, 16, 12, 16, 0, -18} /* [d] 100*/ ,
                    { 101, 10, 15, 12, 14, 1, -12} /* [e] 101*/ ,
                    { 102, 20, 16, 11, 13, 2, -1} /* [f] 102*/\s
                };
                                
                const UnicodeFontBlock myFont123Blocks[] PROGMEM = {
                    {122624, myFont123Bitmaps_282, myFont123Glyphs_282, 255} /* Latin Extended-G */,
                    {0, myFont123Bitmaps_0, myFont123Glyphs_0, 127} /* Basic Latin */
                };
                                
                const UnicodeFont myFont123[] PROGMEM = {myFont123Blocks, 2, 12};
                                
                """, os.toString());
    }
}