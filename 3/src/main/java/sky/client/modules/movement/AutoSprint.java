package sky.client.modules.movement;

import sky.client.events.Event;
import sky.client.events.impl.EventUpdate;
import sky.client.modules.Function;
import sky.client.modules.FunctionAnnotation;
import sky.client.modules.Type;

@FunctionAnnotation(name = "AutoSprint", desc = "Автоматически включает бег", type = Type.Move)
public class AutoSprint extends Function {
    @Override
    public void onEvent(Event event) {
        if (event instanceof EventUpdate) {
            mc.options.sprintKey.setPressed(true);
        }
    }

    @Override
    protected void onDisable() {
        mc.options.sprintKey.setPressed(false);
        super.onDisable();
    }
}