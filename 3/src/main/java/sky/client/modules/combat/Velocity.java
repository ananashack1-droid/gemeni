package sky.client.modules.combat;

import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import sky.client.events.Event;
import sky.client.events.impl.EventPacket;
import sky.client.events.impl.move.EventMotion;
import sky.client.modules.Function;
import sky.client.modules.FunctionAnnotation;
import sky.client.modules.Type;
import sky.client.modules.setting.BooleanSetting;
import sky.client.modules.setting.ModeSetting;
import sky.client.modules.setting.SliderSetting;

@FunctionAnnotation(name = "Velocity", keywords = {"AKB", "AntiKnockBack"}, type = Type.Combat, desc = "Отключает отбрасывание")
public class Velocity extends Function {

    private final ModeSetting mode = new ModeSetting("Режим", "Cancel", "Cancel", "Grim");
    private final BooleanSetting countHits = new BooleanSetting("Счётчик ударов", false, () -> mode.is("Grim"));
    private final SliderSetting untilCount = new SliderSetting("До счётчика", 4, 1, 10, 1, () -> mode.is("Grim") && countHits.get());
    private final SliderSetting afterCount = new SliderSetting("После счётчика", 2, 1, 10, 1, () -> mode.is("Grim") && countHits.get());
    private final BooleanSetting onlyNetherite = new BooleanSetting("Только в незерите", false, () -> mode.is("Grim"));

    private int hits;
    private Vec3d kb = Vec3d.ZERO;

    public Velocity() {
        addSettings(mode, countHits, untilCount, afterCount, onlyNetherite);
    }

    @Override
    public void onEvent(Event event) {
        if (mc.player == null) return;

        if (event instanceof EventPacket e && e.getPacket() instanceof EntityVelocityUpdateS2CPacket v) {
            if (v.getEntityId() != mc.player.getId()) return;

            if (mode.is("Cancel")) {
                e.setCancel(true);
            } else {
                kb = new Vec3d(v.getVelocityX(), v.getVelocityY(), v.getVelocityZ()).multiply(1 / 8000.0);
                e.setCancel(true);
            }
        }

        if (event instanceof EventMotion && mode.is("Grim")) {
            if (mc.player.hurtTime > 0 && canApply()) {
                Vec3d pos = mc.player.getPos();
                float yawDiff = MathHelper.wrapDegrees(mc.player.getYaw() - yawTo(pos.add(kb.multiply(1.5))));
                Vec3d vel = mc.player.getVelocity();

                double mul = Math.abs(yawDiff) <= 60 ? -1.2 : (Math.abs(yawDiff) > 120 ? 1.3 : -0.8);
                mc.player.setVelocity(vel.x + kb.x * mul, vel.y, vel.z + kb.z * mul);

                if (mul == -1.2 && mc.player.isOnGround()) mc.player.jump();
            }

            if (mc.player.hurtTime == 9) {
                hits++;
                int max = untilCount.get().intValue() + afterCount.get().intValue();
                if (countHits.get() && hits > max) hits = 0;
            }
        }
    }

    private boolean canApply() {
        if (countHits.get() && hits > untilCount.get().intValue()) return false;
        if (onlyNetherite.get() && !hasNetherite()) return false;
        if (mc.player.isSubmergedInWater() || mc.player.isInLava() || mc.player.isGliding()) return false;
        return kb.lengthSquared() > 0.01;
    }

    private boolean hasNetherite() {
        for (var s : mc.player.getArmorItems()) {
            if (!s.isEmpty() && s.getItem().toString().contains("netherite")) return true;
        }
        return false;
    }

    private float yawTo(Vec3d to) {
        Vec3d d = to.subtract(mc.player.getPos());
        return (float) Math.toDegrees(Math.atan2(d.z, d.x)) - 90f;
    }

    @Override
    protected void onDisable() {
        hits = 0;
        kb = Vec3d.ZERO;
    }
}