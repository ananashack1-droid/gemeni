package sky.client.modules.combat;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import sky.client.events.Event;
import sky.client.events.impl.move.EventMotion;
import sky.client.events.impl.render.EventRender3D;
import sky.client.modules.Function;
import sky.client.modules.FunctionAnnotation;
import sky.client.modules.Type;
import sky.client.modules.setting.BooleanSetting;
import sky.client.modules.setting.SliderSetting;
import sky.client.util.color.ColorUtil;
import sky.client.util.render.Render3DUtil;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@FunctionAnnotation(name = "BackTrack", type = Type.Combat, desc = "Задерживает хитбокс игроков для увеличения дальности удара")
public class BackTrack extends Function {

    private final SliderSetting time = new SliderSetting("Время (мс)", 600, 100, 1000, 50);
    public final BooleanSetting render = new BooleanSetting("Отображать", true);
    private final SliderSetting lineWidth = new SliderSetting("Толщина линий", 1.5f, 0.5f, 3f, 0.1f, () -> render.get());

    private final Map<PlayerEntity, List<Pos>> positions = new ConcurrentHashMap<>();

    public BackTrack() {
        addSettings(time, render, lineWidth);
    }

    @Override
    public void onEvent(Event event) {
        if (mc.player == null) return;

        if (event instanceof EventMotion) {
            long max = time.get().longValue(), now = System.currentTimeMillis();

            for (PlayerEntity p : mc.world.getPlayers()) {
                if (p == mc.player || !p.isAlive()) continue;

                var list = positions.computeIfAbsent(p, k -> new CopyOnWriteArrayList<>());
                Vec3d cur = p.getPos();

                if (!list.isEmpty() && list.get(list.size() - 1).pos.squaredDistanceTo(cur) < 0.0001) {
                    list.removeIf(pos -> now - pos.time > max);
                    continue;
                }

                list.add(new Pos(cur, now));
                list.removeIf(pos -> now - pos.time > max);
            }
            positions.entrySet().removeIf(e -> !e.getKey().isAlive() || e.getKey().isRemoved());
        }

        if (event instanceof EventRender3D && render.get()) {
            long max = time.get().longValue(), now = System.currentTimeMillis();
            float lw = lineWidth.get().floatValue();

            for (var e : positions.entrySet()) {
                PlayerEntity p = e.getKey();
                List<Pos> pts = e.getValue();
                if (pts.isEmpty() || p.getPos().squaredDistanceTo(pts.get(0).pos) < 0.01) continue;

                float w = p.getWidth(), h = p.getHeight();
                for (Pos pos : pts) {
                    float alpha = Math.max(0, 1f - (now - pos.time) / (float) max);
                    Render3DUtil.drawBox(box(pos.pos, w, h), ColorUtil.reAlphaInt(ColorUtil.getColorStyle(180), (int)(alpha * 255)), lw, true, true, true);
                }
            }
        }
    }

    private double edgeDist(Vec3d from, Vec3d pos, PlayerEntity p) {
        return Math.max(0, from.distanceTo(pos.add(0, p.getHeight() / 2, 0)) - p.getWidth() / 2.0);
    }

    public Pos getOptimal(PlayerEntity p, double maxRange) {
        if (!state) return null;
        var list = positions.get(p);
        if (list == null || list.isEmpty()) return null;

        Vec3d eye = mc.player.getEyePos();
        Vec3d target = p.getPos().add(0, p.getHeight() / 2, 0);
        Pos best = null;
        double bestDist = Double.MAX_VALUE;

        for (Pos pos : list) {
            if (edgeDist(eye, pos.pos, p) > maxRange) continue;
            double d = pos.pos.add(0, p.getHeight() / 2, 0).distanceTo(target);
            if (d < bestDist) { bestDist = d; best = pos; }
        }
        return best;
    }

    public Pos getClosest(PlayerEntity p, double maxRange) {
        if (!state) return null;
        var list = positions.get(p);
        if (list == null || list.isEmpty()) return null;

        Vec3d eye = mc.player.getEyePos();
        Pos best = null;
        double bestDist = Double.MAX_VALUE;

        for (Pos pos : list) {
            double d = edgeDist(eye, pos.pos, p);
            if (d <= maxRange && d < bestDist) { bestDist = d; best = pos; }
        }
        return best;
    }

    public boolean hasReachable(PlayerEntity p, double range) { return getOptimal(p, range) != null; }
    public double getEdgeDist(Pos pos, PlayerEntity p) { return pos != null ? edgeDist(mc.player.getEyePos(), pos.pos, p) : Double.MAX_VALUE; }
    public double getRealEdgeDist(PlayerEntity p) { return p != null ? edgeDist(mc.player.getEyePos(), p.getPos(), p) : Double.MAX_VALUE; }

    public Box getHitBox(Pos pos, PlayerEntity p) { return pos != null ? box(pos.pos, p.getWidth(), p.getHeight()) : null; }
    public Box getBox(PlayerEntity p) { Pos pos = getOldest(p); return pos != null ? box(pos.pos, p.getWidth(), p.getHeight()) : null; }
    public Vec3d getPos(PlayerEntity p) { Pos pos = getOldest(p); return pos != null ? pos.pos : null; }

    public Pos getOldest(PlayerEntity p) {
        if (!state) return null;
        var list = positions.get(p);
        return list != null && !list.isEmpty() ? list.get(0) : null;
    }

    private Box box(Vec3d pos, float w, float h) {
        double hw = w / 2.0;
        return new Box(pos.x - hw, pos.y, pos.z - hw, pos.x + hw, pos.y + h, pos.z + hw);
    }

    @Override
    protected void onEnable() { positions.clear(); }

    @Override
    protected void onDisable() { positions.clear(); }

    public record Pos(Vec3d pos, long time) {}
}