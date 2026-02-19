package sky.client.modules.player;

import sky.client.events.Event;
import sky.client.modules.Function;
import sky.client.modules.FunctionAnnotation;
import sky.client.modules.Type;

@FunctionAnnotation(name = "NoRayTrace",keywords = {"NoEntityTrace"}, desc = "Убирает хитбокс энтити", type = Type.Player)
public class NoRayTrace extends Function {

    @Override
    public void onEvent(Event event) {
    }
}