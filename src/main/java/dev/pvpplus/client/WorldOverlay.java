package dev.pvpplus.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/** Рисуется поверх экрана через проекцию точек мира (без настоящей 3D-отрисовки, поэтому видно и сквозь блоки). */
public final class WorldOverlay {
    private static final int[] A = new int[2];
    private static final int[] B = new int[2];
    private static final int[][] PT = new int[8][2];
    private static final boolean[] PT_OK = new boolean[8];
    private static final int[][] EDGES = {{0, 1}, {2, 3}, {4, 5}, {6, 7}, {0, 2}, {1, 3}, {4, 6}, {5, 7}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
    private static final double[][] COS = new double[4][];
    private static final double[][] SIN = new double[4][];

    static {
        for (int l = 0; l < 4; l++) {
            int seg = Optimization.ringSegments(l);
            COS[l] = new double[seg + 1];
            SIN[l] = new double[seg + 1];
            for (int i = 0; i <= seg; i++) {
                double a = 2 * Math.PI * i / seg;
                COS[l][i] = Math.cos(a);
                SIN[l][i] = Math.sin(a);
            }
        }
    }

    public static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity p = mc.player;
        if (p == null || mc.world == null) return;
        boolean hitRange = Config.on("hitrange"), bubble = Config.on("hitbubble"), jump = Config.on("jumpcircles");
        boolean trails = Config.on("trails"), esp = Config.on("targetesp"), arrows = Config.on("arrownametag");
        boolean predict = Config.on("prediction");
        if (!(hitRange || bubble || jump || trails || esp || arrows || predict)) return;

        Projection.Frame f = Projection.begin(mc, tick.getTickProgress(false));
        int now = CombatTracker.tickCount;
        int lvl = Optimization.level();
        int stride = Optimization.lineStride();
        boolean simple = Optimization.simple();

        if (hitRange || esp) {
            LivingEntity target = CombatTracker.currentTarget(mc);
            if (target != null) {
                if (hitRange) {
                    ring(ctx, f, target.getX(), target.getY() + 0.05, target.getZ(), Config.num("hitrange.radius"),
                            Config.color("hitrange.color"), lvl, stride);
                }
                if (esp) box(ctx, f, target.getBoundingBox(), Config.color("targetesp.color"), stride);
            }
        }

        if (bubble && !Effects.bubbles.isEmpty()) {
            int life = Math.max(1, Config.i("hitbubble.life"));
            int base = Config.color("hitbubble.color");
            double maxR = Config.num("hitbubble.radius");
            for (int i = 0; i < Effects.bubbles.size(); i++) {
                Effects.Ring r = Effects.bubbles.get(i);
                double t = Math.min(1.0, (now - r.tick()) / (double) life);
                int col = simple ? Gfx.alpha(base, 200) : Gfx.alpha(base, (int) (230 * (1 - t)));
                ring(ctx, f, r.pos().x, r.pos().y, r.pos().z, 0.3 + (maxR - 0.3) * t, col, lvl, stride);
            }
        }

        if (jump && !Effects.jumpCircles.isEmpty()) {
            int life = Math.max(1, Config.i("jumpcircles.life"));
            int base = Config.color("jumpcircles.color");
            double maxR = Config.num("jumpcircles.radius");
            for (int i = 0; i < Effects.jumpCircles.size(); i++) {
                Effects.Ring r = Effects.jumpCircles.get(i);
                double t = Math.min(1.0, (now - r.tick()) / (double) life);
                int col = simple ? Gfx.alpha(base, 200) : Gfx.alpha(base, (int) (230 * (1 - t)));
                ring(ctx, f, r.pos().x, r.pos().y, r.pos().z, 0.3 + (maxR - 0.3) * t, col, lvl, stride);
            }
        }

        if (trails && Effects.trail.size() > 1) {
            int n = Effects.trail.size();
            int base = Config.color("trails.color");
            int i = 0;
            boolean have = false;
            for (Vec3d v : Effects.trail) {
                if (i % stride == 0) {
                    boolean ok = f.project(v, B);
                    if (ok && have) Gfx.line(ctx, A[0], A[1], B[0], B[1], Gfx.alpha(base, 40 + (int) (200.0 * i / n)), stride);
                    if (ok) {
                        A[0] = B[0];
                        A[1] = B[1];
                    }
                    have = ok;
                }
                i++;
            }
        }

        if (arrows) {
            double range = Config.num("arrownametag.range");
            for (Entity e : mc.world.getEntities()) {
                if (!(e instanceof PersistentProjectileEntity ar)) continue;
                if (ar.getVelocity().lengthSquared() < 0.01) continue;
                double d = p.distanceTo(ar);
                if (d > range || d < 2) continue;
                if (!f.project(ar.getX(), ar.getY() + 0.3, ar.getZ(), A)) continue;
                Entity owner = ar.getOwner();
                String text = (owner != null ? owner.getName().getString() + " " : "") + Math.round(d) + " м";
                ctx.drawCenteredTextWithShadow(mc.textRenderer, text, A[0], A[1] - 10, 0xFFFFD055);
                ctx.fill(A[0] - 1, A[1] - 1, A[0] + 2, A[1] + 2, 0xFFFFD055);
            }
        }

        if (predict) predict(ctx, f, mc, p, stride);
    }

    private static void ring(DrawContext ctx, Projection.Frame f, double cx, double cy, double cz, double r, int color, int lvl, int stride) {
        double[] cs = COS[lvl], sn = SIN[lvl];
        boolean have = false;
        for (int i = 0; i < cs.length; i++) {
            boolean ok = f.project(cx + cs[i] * r, cy, cz + sn[i] * r, B);
            if (ok && have) Gfx.line(ctx, A[0], A[1], B[0], B[1], color, stride);
            if (ok) {
                A[0] = B[0];
                A[1] = B[1];
            }
            have = ok;
        }
    }

    private static void box(DrawContext ctx, Projection.Frame f, Box bb, int color, int stride) {
        for (int i = 0; i < 8; i++) {
            PT_OK[i] = f.project((i & 1) == 0 ? bb.minX : bb.maxX, (i & 2) == 0 ? bb.minY : bb.maxY, (i & 4) == 0 ? bb.minZ : bb.maxZ, PT[i]);
        }
        for (int[] e : EDGES) {
            if (PT_OK[e[0]] && PT_OK[e[1]]) Gfx.line(ctx, PT[e[0]][0], PT[e[0]][1], PT[e[1]][0], PT[e[1]][1], color, stride);
        }
    }

    private static void predict(DrawContext ctx, Projection.Frame f, MinecraftClient mc, PlayerEntity p, int stride) {
        ItemStack st = p.getMainHandStack();
        double speed, grav;
        if (st.isOf(Items.BOW) && p.isUsingItem() && p.getActiveItem().isOf(Items.BOW)) {
            float t = p.getItemUseTime() / 20.0f;
            float power = Math.min(1.0f, (t * t + t * 2.0f) / 3.0f);
            if (power < 0.1f) return;
            speed = power * 3.0;
            grav = 0.05;
        } else if (st.isOf(Items.ENDER_PEARL) || st.isOf(Items.SNOWBALL) || st.isOf(Items.EGG)) {
            speed = 1.5;
            grav = 0.03;
        } else {
            return;
        }

        int color = Config.color("prediction.color");
        Vec3d pos = new Vec3d(f.ex, f.ey - 0.1, f.ez);
        Vec3d vel = new Vec3d(f.fx * speed, f.fy * speed, f.fz * speed);
        boolean have = f.project(pos, A);
        int steps = Optimization.predictSteps();
        for (int i = 0; i < steps; i++) {
            Vec3d next = pos.add(vel);
            BlockHitResult hit = mc.world.raycast(new RaycastContext(pos, next,
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
            boolean stop = hit.getType() != HitResult.Type.MISS;
            Vec3d end = stop ? hit.getPos() : next;
            boolean ok = f.project(end, B);
            if (have && ok) Gfx.line(ctx, A[0], A[1], B[0], B[1], color, stride);
            if (ok) {
                A[0] = B[0];
                A[1] = B[1];
            }
            have = ok;
            if (stop) {
                if (ok) ctx.fill(B[0] - 2, B[1] - 2, B[0] + 3, B[1] + 3, color);
                break;
            }
            pos = next;
            vel = vel.multiply(0.99).add(0, -grav, 0);
        }
    }
}
