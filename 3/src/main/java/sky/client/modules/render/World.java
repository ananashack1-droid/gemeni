package sky.client.modules.render;

import net.minecraft.client.render.FogShape;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import sky.client.events.Event;
import sky.client.events.impl.EventPacket;
import sky.client.events.impl.EventUpdate;
import sky.client.events.impl.world.EventFog;
import sky.client.manager.Manager;
import sky.client.modules.Function;
import sky.client.modules.FunctionAnnotation;
import sky.client.modules.Type;
import sky.client.modules.setting.BooleanSetting;
import sky.client.modules.setting.ModeSetting;
import sky.client.modules.setting.SliderSetting;
import sky.client.util.color.ColorUtil;

@SuppressWarnings("All")
@FunctionAnnotation(name = "World", desc = "Позволяет менять время суток, погоду и туман", type = Type.Render)
public class World extends Function {

    public final BooleanSetting timeBox = new BooleanSetting("Изменять время", true);
    public final ModeSetting timeMode = new ModeSetting(timeBox::get, "Время суток", "День", "День", "Ночь", "Утро", "Восход", "Кастомное");
    public final SliderSetting customTime = new SliderSetting("Кастомное время", 6000, 0, 24000, 100, () -> timeBox.get() && timeMode.is("Кастомное"));

    public final BooleanSetting weatherBox = new BooleanSetting("Изменять погоду", true);
    public final ModeSetting weatherMode = new ModeSetting(weatherBox::get, "Погода", "Ясно", "Ясно", "Дождь", "Гроза", "Снег");

    public final BooleanSetting fog = new BooleanSetting("Туман", false);
    public final SliderSetting fogEnd = new SliderSetting("Дальность тумана", 200, 0, 500, 1, fog::get);

    public World() {
        addSettings(timeBox, timeMode, customTime, weatherBox, weatherMode, fog, fogEnd);
    }

    @Override
    public void onEvent(Event event) {
        if (!this.state) return;
        if (mc.world == null) return;

        if (event instanceof EventPacket packet && timeBox.get()) {
            if (packet.getPacket() instanceof WorldTimeUpdateS2CPacket) {
                packet.setCancel(true);
            }
        }

        if (event instanceof EventUpdate) {
            if (timeBox.get()) {
                mc.world.setTime(resolveTime(), resolveTime(), false);
            }

            if (weatherBox.get()) {
                switch (weatherMode.get()) {
                    case "Ясно" -> {
                        mc.world.setRainGradient(0f);
                        mc.world.setThunderGradient(0f);
                    }
                    case "Дождь" -> {
                        mc.world.setRainGradient(1f);
                        mc.world.setThunderGradient(0f);
                    }
                    case "Гроза" -> {
                        mc.world.setRainGradient(1f);
                        mc.world.setThunderGradient(1f);
                    }
                    case "Снег" -> {
                        mc.world.setRainGradient(1f);
                        mc.world.setThunderGradient(0f);
                    }
                }
            }
        }

        if (event instanceof EventFog fogEvent && fog.get()) {
            int themeColor = ColorUtil.gradient(15, 360, Manager.STYLE_MANAGER.getFirstColor(), Manager.STYLE_MANAGER.getSecondColor());
            fogEvent.r = ((themeColor >> 16) & 0xFF) / 255.0f;
            fogEvent.g = ((themeColor >> 8) & 0xFF) / 255.0f;
            fogEvent.b = (themeColor & 0xFF) / 255.0f;
            fogEvent.alpha = 1.0f;
            fogEvent.start = 0.0f;
            fogEvent.end = fogEnd.get().floatValue();
            fogEvent.shape = FogShape.SPHERE;
            fogEvent.modified = true;
        }
    }

    private long resolveTime() {
        return switch (timeMode.get()) {
            case "День" -> 1000L;
            case "Ночь" -> 13000L;
            case "Утро" -> 0L;
            case "Восход" -> 23000L;
            case "Кастомное" -> (long) customTime.get().floatValue();
            default -> 6000L;
        };
    }
}