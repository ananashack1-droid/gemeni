package sky.client.modules.movement;

import net.minecraft.client.option.Perspective;
import sky.client.modules.setting.BindSetting;
import sky.client.events.Event;
import sky.client.events.impl.input.EventKey;
import sky.client.manager.Manager;
import sky.client.modules.Function;
import sky.client.modules.FunctionAnnotation;
import sky.client.modules.Type;
import sky.client.modules.movement.freelook.FreeLookState;
import sky.client.manager.ClientManager;

@FunctionAnnotation(name = "FreeLook", desc = "Позволит вращать камеру, при этом не меняя направления движения", type = Type.Move)
public class FreeLook extends Function {
    private final BindSetting bind = new BindSetting("Кнопка", 0);
    private Perspective previousPerspective;

    public FreeLook() {
        addSettings(bind);
    }

    @Override
    public void onEvent(Event event) {
        if (!(event instanceof EventKey keyEvent)) return;
        if (keyEvent.key != bind.getKey()) return;
        if (mc == null || mc.options == null) return;
        var attackAura = Manager.FUNCTION_MANAGER.attackAura;
        if (attackAura != null && attackAura.state && attackAura.target != null) {
            ClientManager.message("Нельзя использовать с " + attackAura.name);
            return;
        }

        FreeLookState.active = !FreeLookState.active;

        if (FreeLookState.active) {
            previousPerspective = mc.options.getPerspective();
            if (previousPerspective != Perspective.THIRD_PERSON_FRONT) {
                mc.options.setPerspective(Perspective.THIRD_PERSON_FRONT);
            }
        } else {
            mc.options.setPerspective(previousPerspective != null ? previousPerspective : Perspective.FIRST_PERSON);
        }
    }
    @Override
    public void onDisable() {
        FreeLookState.active = false;
        if (mc != null && mc.options != null) {
            mc.options.setPerspective(previousPerspective != null ? previousPerspective : Perspective.FIRST_PERSON);
        }
        previousPerspective = null;
    }

}