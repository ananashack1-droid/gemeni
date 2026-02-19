package sky.client.modules.player;

import sky.client.events.Event;
import sky.client.modules.Function;
import sky.client.modules.FunctionAnnotation;
import sky.client.modules.Type;
import sky.client.modules.setting.ModeSetting;

@FunctionAnnotation(
        name = "ClickAction",
        keywords = "SwapAction",
        desc = "Ставит свапы под выбранный тип сервера (чтобы не забанило)",
        type = Type.Player)
public class ClickAction extends Function {

    public final ModeSetting type =
            new ModeSetting("Тип", "ReallyWorld", "ReallyWorld", "FunTime", "HollyWorld");

    @Override
    public void onEvent(Event event) {
    }

    public final boolean nonBatch() {
        return type.is("ReallyWorld");
    }

    public final boolean batch() {
        return type.is("FunTime") || type.is("HollyWorld");
    }
}
