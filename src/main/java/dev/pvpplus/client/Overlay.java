package dev.pvpplus.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.Vec3d;

public final class Overlay {
    private static final int[] OUT = new int[2];

    /** Свой прицел (заменяет ванильный, если включён). */
    public static void crosshair(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || !mc.options.getPerspective().isFirstPerson()) return;
        int cx = mc.getWindow().getScaledWidth() / 2;
        int cy = mc.getWindow().getScaledHeight() / 2;
        int s = Config.i("crosshair.size"), g = Config.i("crosshair.gap"), t = Config.i("crosshair.thick");
        int color = Config.color("crosshair.color");
        int half = t / 2;

        ctx.fill(cx - g - s, cy - half, cx - g, cy - half + t, color);
        ctx.fill(cx + 1 + g, cy - half, cx + 1 + g + s, cy - half + t, color);
        ctx.fill(cx - half, cy - g - s, cx - half + t, cy - g, color);
        ctx.fill(cx - half, cy + 1 + g, cx - half + t, cy + 1 + g + s, color);
        if (Config.b("crosshair.dot")) ctx.fill(cx, cy, cx + 1, cy + 1, color);
    }

    /** Хитмаркер, индикатор направления удара и числа урона. */
    public static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        int w = mc.getWindow().getScaledWidth();
        int h = mc.getWindow().getScaledHeight();
        int cx = w / 2, cy = h / 2;
        int now = CombatTracker.tickCount;

        if (Config.on("hitmarker")) {
            int age = now - CombatTracker.hitmarkerTick;
            if (age >= 0 && age <= 6) {
                int col = CombatTracker.hitmarkerKill ? 0xFFFF4040 : Config.color("hitmarker.color");
                int len = Config.i("hitmarker.size");
                for (int k = 0; k < len; k++) {
                    int o = 3 + k;
                    Gfx.px(ctx, cx + o, cy + o, col);
                    Gfx.px(ctx, cx - o, cy + o, col);
                    Gfx.px(ctx, cx + o, cy - o, col);
                    Gfx.px(ctx, cx - o, cy - o, col);
                }
            }
        }

        if (Config.on("hitdir")) {
            int age = now - CombatTracker.hitDirTick;
            if (age >= 0 && age <= 30) {
                double yaw = Math.toRadians(mc.gameRenderer.getCamera().getYaw());
                double dx = CombatTracker.hitDx, dz = CombatTracker.hitDz;
                double rx = dx * (-Math.cos(yaw)) + dz * (-Math.sin(yaw));
                double fz = dx * (-Math.sin(yaw)) + dz * Math.cos(yaw);
                double angle = Math.atan2(rx, fz);
                int alpha = Math.max(30, (int) (255 * (1.0 - age / 30.0)));
                int col = (alpha << 24) | 0xFF3030;
                for (int r = 34; r <= 46; r += 4) {
                    int sx = cx + (int) Math.round(Math.sin(angle) * r);
                    int sy = cy - (int) Math.round(Math.cos(angle) * r);
                    ctx.fill(sx - 2, sy - 2, sx + 3, sy + 3, col);
                }
            }
        }

        if (Config.on("damage") && !CombatTracker.numbers.isEmpty()) {
            Projection.Frame f = Projection.begin(mc, tick.getTickProgress(false));
            TextRenderer tr = mc.textRenderer;
            for (int i = 0; i < CombatTracker.numbers.size(); i++) {
                CombatTracker.DamageNumber n = CombatTracker.numbers.get(i);
                int age = now - n.tick();
                Vec3d pos = n.pos();
                if (!f.project(pos.x, pos.y + age * 0.03, pos.z, OUT)) continue;
                int alpha = age < 15 ? 255 : Math.max(30, (int) (255 * (1.0 - (age - 15) / 10.0)));
                ctx.drawCenteredTextWithShadow(tr, n.text(), OUT[0], OUT[1], (alpha << 24) | 0xFF5555);
            }
        }
    }
}
