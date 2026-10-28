package dev.pvpplus.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;

public final class Effects {
    public record Ring(Vec3d pos, int tick) {}

    public static final List<Ring> jumpCircles = new ArrayList<>();
    public static final List<Ring> bubbles = new ArrayList<>();
    public static final Deque<Vec3d> trail = new ArrayDeque<>();

    private static boolean wasOnGround = true;
    private static final Random RNG = new Random();

    public static boolean isCrit(PlayerEntity p) {
        return p.fallDistance > 0 && !p.isOnGround() && !p.isClimbing() && !p.isTouchingWater()
                && !p.hasStatusEffect(StatusEffects.BLINDNESS) && !p.hasVehicle() && !p.isSprinting();
    }

    private static ParticleEffect hitType(int i) {
        return switch (i) {
            case 1 -> ParticleTypes.DAMAGE_INDICATOR;
            case 2 -> ParticleTypes.HEART;
            case 3 -> ParticleTypes.FLAME;
            case 4 -> ParticleTypes.END_ROD;
            case 5 -> ParticleTypes.ENCHANTED_HIT;
            default -> ParticleTypes.CRIT;
        };
    }

    private static ParticleEffect killType(int i) {
        return switch (i) {
            case 1 -> ParticleTypes.SOUL;
            case 2 -> ParticleTypes.EXPLOSION;
            case 3 -> ParticleTypes.FIREWORK;
            default -> ParticleTypes.TOTEM_OF_UNDYING;
        };
    }

    /** Создаёт частицы с учётом уровня оптимизации и лимита на тик. */
    private static void burst(MinecraftClient mc, ParticleEffect type, double x, double y, double z, double spread, int count) {
        int n = Optimization.takeParticles(count);
        for (int i = 0; i < n; i++) {
            mc.particleManager.addParticle(type,
                    x + (RNG.nextDouble() - 0.5) * 0.6, y + RNG.nextDouble() * 0.6, z + (RNG.nextDouble() - 0.5) * 0.6,
                    (RNG.nextDouble() - 0.5) * spread, RNG.nextDouble() * spread, (RNG.nextDouble() - 0.5) * spread);
        }
    }

    public static void onHit(LivingEntity le, boolean crit) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
        double x = le.getX(), y = le.getY() + le.getHeight() * 0.6, z = le.getZ();
        if (Config.on("hitparticles")) burst(mc, hitType(Config.i("hitparticles.type")), x, y, z, 0.3, Config.i("hitparticles.count"));
        if (crit && Config.on("crit")) burst(mc, hitType(Config.i("crit.type")), x, y, z, 0.5, Config.i("crit.count"));
        if (Config.on("hitbubble")) bubbles.add(new Ring(new Vec3d(le.getX(), le.getY() + le.getHeight() * 0.5, le.getZ()), CombatTracker.tickCount));
    }

    public static void onKill(LivingEntity le) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || !Config.on("killfx")) return;
        burst(mc, killType(Config.i("killfx.type")), le.getX(), le.getY() + le.getHeight() * 0.5, le.getZ(), 0.4, Config.i("killfx.count"));
    }

    private static void prune(List<Ring> list, int now, int life) {
        for (int i = list.size() - 1; i >= 0; i--) {
            if (now - list.get(i).tick() > life) list.remove(i);
        }
    }

    public static void tick(MinecraftClient mc) {
        PlayerEntity p = mc.player;
        if (p == null || mc.world == null) {
            jumpCircles.clear();
            bubbles.clear();
            trail.clear();
            return;
        }
        int now = CombatTracker.tickCount;

        boolean onGround = p.isOnGround();
        if (Config.on("jumpcircles") && wasOnGround && !onGround && p.getVelocity().y > 0.1) {
            jumpCircles.add(new Ring(new Vec3d(p.getX(), p.getY() + 0.05, p.getZ()), now));
        }
        wasOnGround = onGround;
        if (!jumpCircles.isEmpty()) prune(jumpCircles, now, Config.i("jumpcircles.life"));
        if (!bubbles.isEmpty()) prune(bubbles, now, Config.i("hitbubble.life"));

        if (Config.on("trails")) {
            trail.addLast(new Vec3d(p.getX(), p.getY() + 0.1, p.getZ()));
            int max = Config.i("trails.life");
            while (trail.size() > max) trail.removeFirst();
        } else if (!trail.isEmpty()) {
            trail.clear();
        }

        // частицы мира: на высоких уровнях оптимизации реже
        if (Config.on("worldparticles") && (!Optimization.simple() || (now & 1) == 0)) {
            int type = Config.i("worldparticles.type");
            ParticleEffect pe = type == 1 ? ParticleTypes.END_ROD : type == 2 ? ParticleTypes.CLOUD : ParticleTypes.SNOWFLAKE;
            int n = Optimization.takeParticles(Config.i("worldparticles.density"));
            for (int i = 0; i < n; i++) {
                mc.particleManager.addParticle(pe,
                        p.getX() + (RNG.nextDouble() - 0.5) * 16, p.getY() + RNG.nextDouble() * 6, p.getZ() + (RNG.nextDouble() - 0.5) * 16,
                        0, -0.03, 0);
            }
        }
    }
}
