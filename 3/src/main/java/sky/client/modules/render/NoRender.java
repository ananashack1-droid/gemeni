package sky.client.modules.render;

import sky.client.modules.setting.MultiSetting;
import sky.client.events.Event;
import sky.client.modules.Function;
import sky.client.modules.FunctionAnnotation;
import sky.client.modules.Type;

import java.util.Arrays;

@FunctionAnnotation(name = "NoRender", type = Type.Render, desc = "Убирает разные типы на экране")
public class NoRender extends Function {
    public MultiSetting mods = new MultiSetting(
            "Убрать",
            Arrays.asList("Тряска камеры", "Огонь на экране", "Вода на экране","Удушье","Плохие эффекты"),
            new String[]{"Тряска камеры", "Огонь на экране", "Вода на экране", "Удушье", "Скорборд","Плохие эффекты"}
    );

    public NoRender() {
        addSettings(mods);
    }
    @Override
    public void onEvent(Event event) {

    }
}