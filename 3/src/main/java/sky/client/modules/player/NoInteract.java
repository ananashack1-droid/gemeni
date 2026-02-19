package sky.client.modules.player;

import sky.client.modules.setting.BooleanSetting;
import sky.client.events.Event;
import sky.client.modules.Function;
import sky.client.modules.FunctionAnnotation;
import sky.client.modules.Type;

@FunctionAnnotation(name = "NoInteract", desc = "Не даст вам открыть контейнер по нажатию на ПКМ", type = Type.Player)
public class NoInteract extends Function {
    public final BooleanSetting onlyAura = new BooleanSetting("Только с AttackAura",false);

    public NoInteract() {
        addSettings(onlyAura);
    }
    @Override
    public void onEvent(Event event) {

    }
}