package sky.client.modules.render;

import sky.client.events.Event;
import sky.client.modules.Function;
import sky.client.modules.FunctionAnnotation;
import sky.client.modules.Type;

@FunctionAnnotation(name = "ExtraTab", desc = "Количество игроков табе больше", type = Type.Render)
public class ExtraTab extends Function {

    @Override
    public void onEvent(Event event) {

    }
}