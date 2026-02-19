package sky.client.modules.player;

import sky.client.modules.setting.MultiSetting;
import sky.client.events.Event;
import sky.client.modules.Function;
import sky.client.modules.FunctionAnnotation;
import sky.client.modules.Type;
import java.util.Arrays;

@FunctionAnnotation(name = "NoPush", desc = "Убивает коллизию от разных типов", type = Type.Player)
public class NoPush extends Function {
    public MultiSetting mods = new MultiSetting(
            "Типы",
            Arrays.asList("Игроки", "Блоки"),
            new String[]{"Вода", "Игроки", "Блоки"}
    );
    public NoPush() {
        addSettings(mods);
    }

    @Override
    public void onEvent(Event event) {
    }
}
