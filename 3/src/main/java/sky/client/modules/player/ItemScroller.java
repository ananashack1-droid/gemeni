package sky.client.modules.player;

import sky.client.modules.setting.SliderSetting;
import sky.client.events.Event;
import sky.client.modules.Function;
import sky.client.modules.FunctionAnnotation;
import sky.client.modules.Type;

@FunctionAnnotation(name = "ItemScroll", desc = "Быстрое перемещение", type = Type.Player)
public class ItemScroller extends Function {
    public SliderSetting scroll = new SliderSetting("Задержка", 100f, 1f, 100f,1f);

    public ItemScroller() {
        addSettings(scroll);
    }

    @Override
    public void onEvent(Event event) {

    }
}