/*package sky.client.modules.combat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import sky.client.events.Event;
import sky.client.events.impl.EventUpdate;
import sky.client.events.impl.player.EventAttack;
import sky.client.manager.Manager;
import sky.client.modules.Function;
import sky.client.modules.FunctionAnnotation;
import sky.client.modules.Type;
//import sky.client.modules.combat.rotation.RotationAI;

@FunctionAnnotation(name = "RotRecorder", desc = "Записывает твои ротации для AI", type = Type.Combat)
public class RotationRecorder extends Function {

    private LivingEntity target;

    @Override
    public void onEvent(Event event) {
        if (mc.player == null || mc.world == null) return;

        RotationAI ai = Manager.FUNCTION_MANAGER.attackAura.neuralAI;

        if (!ai.isRecording()) return;

        if (event instanceof EventAttack e) {
            if (e.getAttacker() == mc.player && e.getTarget() instanceof LivingEntity living) {
                target = living;
                ai.onAttack(target);
                System.out.println("[RotRecorder] Атака! Цель: " + target.getName().getString());
            }
        }

        if (event instanceof EventUpdate) {
            if (target != null && !target.isDead() && mc.player.distanceTo(target) < 6) {
                ai.recordTick(target);
            }
        }
    }

    @Override
    protected void onDisable() {
        target = null;
    }
}*/