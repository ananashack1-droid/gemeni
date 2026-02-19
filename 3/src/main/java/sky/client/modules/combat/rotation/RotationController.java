package sky.client.modules.combat.rotation;

import net.minecraft.util.math.MathHelper;
import org.joml.Vector2f;
import sky.client.manager.IMinecraft;
import sky.client.util.math.MathUtil;
import sky.client.util.player.GCDUtil;

public final class RotationController implements IMinecraft {

    private static final RotationController I = new RotationController();
    public static RotationController get() { return I; }

    private final Vector2f rot = new Vector2f();
    private float startYaw, startPitch, targetYaw, targetPitch;
    private long resetStart = -1, resetDur;
    private boolean resetting;

    private RotationController() {}

    public float getYaw() { return rot.x; }
    public float getPitch() { return rot.y; }

    public void set(float yaw, float pitch) {
        rot.x = yaw;
        rot.y = pitch;
    }

    public void setSmooth(float tgtYaw, float tgtPitch, float smooth, float maxYaw, float maxPitch, boolean gcd) {
        float dY = MathHelper.wrapDegrees(tgtYaw - rot.x);
        float dP = tgtPitch - rot.y;

        float sY = MathHelper.clamp(Math.abs(dY), 1f, maxYaw) * smooth;
        float sP = MathHelper.clamp(Math.abs(dP), 1f, maxPitch) * smooth;

        float ny = rot.x + (dY > 0 ? sY : -sY);
        float np = MathHelper.clamp(rot.y + (dP > 0 ? sP : -sP), -89.9f, 89.9f);

        if (gcd) {
            float g = GCDUtil.getGCDValue();
            ny -= (ny - rot.x) % g;
            np -= (np - rot.y) % g;
        }

        rot.x = ny;
        rot.y = np;
    }

    public void smoothReturn(long ms) {
        if (mc.player == null) return;
        resetStart = System.currentTimeMillis();
        resetDur = ms;
        startYaw = rot.x;
        startPitch = rot.y;
        targetYaw = mc.player.getYaw();
        targetPitch = mc.player.getPitch();
        resetting = true;
    }

    public void onUpdate() {
        if (!resetting) return;

        float progress = Math.min(1f, (System.currentTimeMillis() - resetStart) / (float) resetDur);
        float eased = 1f - (float) Math.pow(1 - progress, 3);

        rot.x = MathUtil.interpolateAngle(startYaw, targetYaw, eased);
        rot.y = MathUtil.interpolateAngle(startPitch, targetPitch, eased);

        if (progress >= 1f) {
            resetting = false;
            resetStart = -1;
        }
    }

    public boolean isControlling() { return resetting; }

    public void updateIfFree(float yaw, float pitch) {
        if (!resetting) set(yaw, pitch);
    }
}