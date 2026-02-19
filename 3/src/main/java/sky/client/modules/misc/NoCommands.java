package sky.client.modules.misc;

import sky.client.events.Event;
import sky.client.modules.Function;
import sky.client.modules.FunctionAnnotation;
import sky.client.modules.Type;

@FunctionAnnotation(name = "NoCommands", desc = "Отключение команд через точку", type = Type.Misc)
public class NoCommands extends Function {
    public NoCommands() {
    }

    @Override
    public void onEvent(Event event) {

    }
}