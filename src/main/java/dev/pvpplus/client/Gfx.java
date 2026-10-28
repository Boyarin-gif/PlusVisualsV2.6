package dev.pvpplus.client;

import net.minecraft.client.gui.DrawContext;

public final class Gfx {
    public static void border(DrawContext c, int x, int y, int w, int h, int color) {
        c.fill(x, y, x + w, y + 1, color);
        c.fill(x, y + h - 1, x + w, y + h, color);
        c.fill(x, y + 1, x + 1, y + h - 1, color);
        c.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    public static void panel(DrawContext c, int x, int y, int w, int h, int bgAlpha) {
        c.fill(x, y, x + w, y + h, (bgAlpha << 24) | 0x0E0E14);
        border(c, x, y, w, h, 0x46FFFFFF);
    }

    public static void px(DrawContext c, int x, int y, int color) {
        c.fill(x, y, x + 1, y + 1, color);
    }

    public static int alpha(int color, int a) {
        return (Math.max(0, Math.min(255, a)) << 24) | (color & 0xFFFFFF);
    }

    public static void line(DrawContext c, int x0, int y0, int x1, int y1, int color) {
        line(c, x0, y0, x1, y1, color, 1);
    }

    /** Линия из точек; stride > 1 даёт пунктир (дешевле при высоком уровне оптимизации). */
    public static void line(DrawContext c, int x0, int y0, int x1, int y1, int color, int stride) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        if (steps > 800) return;
        if (steps == 0) {
            px(c, x0, y0, color);
            return;
        }
        int st = Math.max(1, stride);
        for (int i = 0; i <= steps; i += st) {
            int x = x0 + Math.round((x1 - x0) * (float) i / steps);
            int y = y0 + Math.round((y1 - y0) * (float) i / steps);
            px(c, x, y, color);
        }
    }
}
