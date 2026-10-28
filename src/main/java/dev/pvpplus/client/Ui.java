package dev.pvpplus.client;

import net.minecraft.client.gui.DrawContext;

/** Оформление меню в стиле тёмной фиолетовой панели. */
public final class Ui {
    public static final int PANEL = 0xE0141226;
    public static final int ROW = 0xFF1C1A32;
    public static final int ROW_SEL = 0xFF2A2650;
    public static final int ACCENT = 0xFF6C5CE7;
    public static final int TEXT = 0xFFE8E8F4;
    public static final int DIM = 0xFF8C8AA6;
    public static final int OFF = 0xFF3A3852;
    public static final int EDGE = 0x40A898FF;

    public static void rrect(DrawContext c, int x, int y, int w, int h, int col) {
        c.fill(x + 1, y, x + w - 1, y + 1, col);
        c.fill(x, y + 1, x + w, y + h - 1, col);
        c.fill(x + 1, y + h - 1, x + w - 1, y + h, col);
    }

    public static void panel(DrawContext c, int x, int y, int w, int h) {
        rrect(c, x - 1, y - 1, w + 2, h + 2, EDGE);
        rrect(c, x, y, w, h, PANEL);
    }

    public static void toggle(DrawContext c, int x, int y, boolean on) {
        rrect(c, x, y, 20, 10, on ? ACCENT : OFF);
        rrect(c, on ? x + 11 : x + 1, y + 1, 8, 8, 0xFFFFFFFF);
    }

    /** Значок "плюс" рядом с названием (11x11). */
    public static void logo(DrawContext c, int x, int y, int color) {
        rrect(c, x, y + 4, 11, 3, color);
        rrect(c, x + 4, y, 3, 11, color);
    }
}
