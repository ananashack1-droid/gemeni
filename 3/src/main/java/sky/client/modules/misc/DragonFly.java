package sky.client.modules.misc;

import sky.client.events.Event;
import sky.client.events.impl.move.EventMotion;
import sky.client.modules.Function;
import sky.client.modules.FunctionAnnotation;
import sky.client.modules.Type;
import sky.client.util.move.MoveUtil;

@SuppressWarnings("All")
@FunctionAnnotation(name = "DragonFly", desc = "", type = Type.Misc)
public class DragonFly extends Function {

    @Override
    public void onEvent(Event event) {
        if (!(event instanceof EventMotion)) return;
        if (!mc.player.getAbilities().flying) return;
        MoveUtil.setSpeed(1);

        float y = 0;

        boolean noForward = mc.player.forwardSpeed == 0 && !mc.options.leftKey.isPressed() && !mc.options.rightKey.isPressed();

        if (mc.options.jumpKey.isPressed()) y = noForward ? 0.5F : 0.25F;
        else if (mc.options.sneakKey.isPressed()) y = noForward ? -0.5F : -0.25F;

        mc.player.setVelocity(mc.player.getVelocity().x, y, mc.player.getVelocity().z);
    }
}
