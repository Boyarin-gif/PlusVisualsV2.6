package dev.pvpplus.client;

import java.util.List;

import dev.pvpplus.client.WaypointManager.Waypoint;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

/** Быстрая метка: по клавише (по умолчанию средняя кнопка мыши) ставит метку в точку взгляда. */
public final class QuickMarker {
    public static void tick(MinecraftClient mc, KeyBinding key) {
        while (key.wasPressed()) {
            if (mc.currentScreen != null || mc.player == null || mc.world == null) continue;
            if (!Config.on("quickmarker")) continue;
            place(mc);
        }
    }

    private static void place(MinecraftClient mc) {
        double range = Config.num("quickmarker.range");
        HitResult hr = mc.player.raycast(range, 1.0f, false);
        if (hr.getType() != HitResult.Type.BLOCK) {
            Notifier.push("Метка: нет цели в " + (int) range + " блоках");
            return;
        }
        BlockPos bp = ((BlockHitResult) hr).getBlockPos();
        String dim = mc.world.getRegistryKey().getValue().toString();
        List<Waypoint> list = WaypointManager.all();

        // повторная метка рядом с существующей быстрой меткой удаляет её
        for (int i = 0; i < list.size(); i++) {
            Waypoint w = list.get(i);
            if (!w.quick || !dim.equals(w.dim)) continue;
            double dx = w.x - bp.getX(), dy = w.y - bp.getY(), dz = w.z - bp.getZ();
            if (dx * dx + dy * dy + dz * dz < 9.0) {
                list.remove(i);
                WaypointManager.save();
                Notifier.push("Метка удалена");
                return;
            }
        }

        // иначе заменяем прежнюю быструю метку новой
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).quick) list.remove(i);
        }
        Waypoint wp = new Waypoint();
        wp.name = "Метка";
        wp.x = bp.getX();
        wp.y = bp.getY();
        wp.z = bp.getZ();
        wp.dim = dim;
        wp.quick = true;
        list.add(wp);
        WaypointManager.save();
        Notifier.push("Метка: " + bp.getX() + " " + bp.getY() + " " + bp.getZ());
    }
}
