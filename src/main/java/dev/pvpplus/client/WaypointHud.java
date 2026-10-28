package dev.pvpplus.client;

import dev.pvpplus.client.WaypointManager.Waypoint;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.Vec3d;

/** Рисует метки на экране: проекция точки мира в 2D по углам камеры и FOV. */
public final class WaypointHud {
    public static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        if (!Config.on("waypoints")) return;
        if (WaypointManager.all().isEmpty()) return;

        String dim = mc.world.getRegistryKey().getValue().toString();
        float tp = tick.getTickProgress(false);
        Vec3d eye = mc.player.getLerpedPos(tp).add(0, mc.player.getEyeHeight(mc.player.getPose()), 0);

        Camera cam = mc.gameRenderer.getCamera();
        double yaw = Math.toRadians(cam.getYaw());
        double pitch = Math.toRadians(cam.getPitch());
        double sy = Math.sin(yaw), cy = Math.cos(yaw), sp = Math.sin(pitch), cp = Math.cos(pitch);
        Vec3d fwd = new Vec3d(-sy * cp, -sp, cy * cp);
        Vec3d right = new Vec3d(-cy, 0, -sy);
        Vec3d up = new Vec3d(-sy * sp, cp, cy * sp);

        int w = mc.getWindow().getScaledWidth();
        int h = mc.getWindow().getScaledHeight();
        double t = Math.tan(Math.toRadians(mc.options.getFov().getValue()) / 2.0);
        double aspect = (double) w / (double) h;
        TextRenderer tr = mc.textRenderer;

        for (Waypoint wp : WaypointManager.all()) {
            if (!dim.equals(wp.dim)) continue;
            Vec3d d = new Vec3d(wp.x + 0.5, wp.y + 0.5, wp.z + 0.5).subtract(eye);
            double dist = d.length();
            double cx = d.dotProduct(right);
            double cyv = d.dotProduct(up);
            double cz = d.dotProduct(fwd);

            double nx, ny;
            if (cz > 0.05) {
                nx = cx / (cz * t * aspect);
                ny = cyv / (cz * t);
            } else { // позади - прижимаем к краю экрана
                nx = cx >= 0 ? 10 : -10;
                ny = 0;
            }
            nx = Math.max(-0.95, Math.min(0.95, nx));
            ny = Math.max(-0.92, Math.min(0.92, ny));

            int sx = (int) ((nx * 0.5 + 0.5) * w);
            int sy2 = (int) ((0.5 - ny * 0.5) * h);

            ctx.fill(sx - 2, sy2 - 2, sx + 3, sy2 + 3, 0xFF50DCFF);
            ctx.fill(sx - 3, sy2 - 3, sx + 4, sy2 - 2, 0xFF000000);
            ctx.fill(sx - 3, sy2 + 3, sx + 4, sy2 + 4, 0xFF000000);
            ctx.fill(sx - 3, sy2 - 2, sx - 2, sy2 + 3, 0xFF000000);
            ctx.fill(sx + 3, sy2 - 2, sx + 4, sy2 + 3, 0xFF000000);
            ctx.drawCenteredTextWithShadow(tr, wp.name, sx, sy2 - 14, 0xFFFFFFFF);
            ctx.drawCenteredTextWithShadow(tr, Math.round(dist) + " м", sx, sy2 + 6, 0xFFB0B0B0);
        }
    }
}
