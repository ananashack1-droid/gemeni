package sky.client.modules.combat;

import sky.client.modules.setting.SliderSetting;
import sky.client.events.Event;
import sky.client.modules.Function;
import sky.client.modules.FunctionAnnotation;
import sky.client.modules.Type;

@FunctionAnnotation(name = "HitBox", type = Type.Combat, desc = "Позволяет увеличивать хит-бокс игроков")
public class HitBox extends Function {

    public SliderSetting size = new SliderSetting("Размер", 0.4f, 0.1f, 5.5f, 0.1f);

    public HitBox() {
        addSettings(size);
    }
    @Override
    public void onEvent(Event event) {
    }

}