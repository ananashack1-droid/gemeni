package sky.client.modules.render;

import sky.client.events.Event;
import sky.client.events.impl.render.EventRender3D;
import sky.client.manager.Manager;
import sky.client.modules.Function;
import sky.client.modules.FunctionAnnotation;
import sky.client.modules.Type;
import sky.client.modules.setting.BooleanSetting;
import sky.client.modules.setting.ModeSetting;
import sky.client.modules.setting.SliderSetting;

import sky.client.util.shader.impl.FogBlurRender;

@FunctionAnnotation(name = "FogBlur", desc = "Добавляет эффект размытого тумана на расстоянии", type = Type.Render)
public class FogBlur extends Function {

    private final SliderSetting fogStrength = new SliderSetting("Сила размытия", 6f, 1f, 20f, 0.5f);
    private final SliderSetting fogDistance = new SliderSetting("Дистанция тумана", 50f, 0f, 200f, 5f);
    private final BooleanSetting linearSampling = new BooleanSetting("Линейная выборка", true, "Включает линейную выборку для более качественного размытия");

    private final BooleanSetting colorEffect = new BooleanSetting("Цветовой эффект", false, "Добавляет цветовой эффект к туману");
    private final ModeSetting colorMode = new ModeSetting(() -> colorEffect.get(), "Режим цвета", "Тема", "Тема", "RGB Puke");

    private final SliderSetting themeOpacity = new SliderSetting("Прозрачность темы", 30f, 1f, 100f, 1f,
            () -> colorEffect.get() && colorMode.is("Тема"));

    private final SliderSetting pukeOpacity = new SliderSetting("RGB прозрачность", 30f, 1f, 100f, 1f,
            () -> colorEffect.get() && colorMode.is("RGB Puke"));
    private final SliderSetting pukeSaturation = new SliderSetting("RGB насыщенность", 70f, 0f, 100f, 1f,
            () -> colorEffect.get() && colorMode.is("RGB Puke"));
    private final SliderSetting pukeBrightness = new SliderSetting("RGB яркость", 100f, 0f, 100f, 1f,
            () -> colorEffect.get() && colorMode.is("RGB Puke"));

    public FogBlur() {
        addSettings(
                fogStrength,
                fogDistance,
                linearSampling,
                colorEffect,
                colorMode,
                themeOpacity,
                pukeOpacity,
                pukeSaturation,
                pukeBrightness
        );
    }

    @Override
    public void onEvent(Event event) {
        if (!(event instanceof EventRender3D)) return;
        if (mc.player == null || mc.world == null) return;
        if (mc.gameRenderer.isRenderingPanorama()) return;

        if (colorEffect.get() && colorMode.is("Тема")) {
            int color1 = Manager.STYLE_MANAGER.getFirstColor();
            int color2 = Manager.STYLE_MANAGER.getSecondColor();

            FogBlurRender.applyFogBlur(
                    fogStrength.get().floatValue(),
                    fogDistance.get().floatValue(),
                    linearSampling.get(),
                    true,
                    themeOpacity.get().floatValue() / 100.0f,
                    color1,
                    color2
            );
        } else if (colorEffect.get() && colorMode.is("RGB Puke")) {
            FogBlurRender.applyFogBlur(
                    fogStrength.get().floatValue(),
                    fogDistance.get().floatValue(),
                    linearSampling.get(),
                    true,
                    pukeOpacity.get().floatValue() / 100.0f,
                    pukeSaturation.get().floatValue() / 100.0f,
                    pukeBrightness.get().floatValue() / 100.0f
            );
        } else {
            FogBlurRender.applyFogBlur(
                    fogStrength.get().floatValue(),
                    fogDistance.get().floatValue(),
                    linearSampling.get(),
                    false,
                    0f,
                    0f,
                    0f
            );
        }
    }
}
