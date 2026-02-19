package sky.client.modules.render;

import sky.client.events.Event;
import sky.client.modules.Function;
import sky.client.modules.FunctionAnnotation;
import sky.client.modules.Type;
import sky.client.modules.setting.ModeSetting;

@FunctionAnnotation(name = "ItemPhysic", desc = "Красиво лежат предметы на земле", type = Type.Render)
public class ItemPhysic extends Function {

    public final ModeSetting mode = new ModeSetting("Физика","Обычная","Обычная","2D");
    public ItemPhysic() {
        addSettings(mode);
    }

    @Override
    public void onEvent(Event event) {
    }
}