package sky.client.modules.render;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import sky.client.events.Event;
import sky.client.events.impl.EventUpdate;
import sky.client.modules.Function;
import sky.client.modules.FunctionAnnotation;
import sky.client.modules.Type;

@FunctionAnnotation(name = "FullBright", desc = "Освещает местность", type = Type.Render)
public class FullBright extends Function {
    private final StatusEffectInstance nightVisionEffect = new StatusEffectInstance(
            StatusEffects.NIGHT_VISION,
            -1,
            255,
            false,
            false,
            true
    );
    @Override
    public void onEvent(Event event) {
        if (event instanceof EventUpdate) {
          mc.player.addStatusEffect(nightVisionEffect,mc.player);
        }
    }

    @Override
    public void onDisable() {
        mc.player.removeStatusEffect(nightVisionEffect.getEffectType());
        super.onDisable();
    }
}