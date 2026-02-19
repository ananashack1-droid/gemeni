package sky.client.screens.dropdown.impl;

import net.minecraft.client.gui.DrawContext;
import sky.client.manager.Manager;
import sky.client.modules.setting.SliderSetting;
import sky.client.screens.dropdown.SettingRenderer;
import sky.client.manager.fontManager.FontUtils;
import sky.client.util.render.RenderUtil;

import java.awt.*;
import java.util.Locale;

public class SliderSettingRenderer implements SettingRenderer<SliderSetting> {

    @Override
    public void render(DrawContext ctx, SliderSetting setting, int x, int y, int width, int height) {
        RenderUtil.drawRoundedRect(ctx.getMatrices(), x, y + height - 4 - 4, width, 4, 1, new Color(50, 50, 50, 180).getRGB());

        if (setting.circlePos == -1)
            setting.circlePos = width * ((Math.round(setting.get().doubleValue() / setting.getIncrement()) * setting.getIncrement() - setting.getMin()) / (setting.getMax() - setting.getMin()));

        setting.circlePos += ((width * ((Math.round(setting.get().doubleValue() / setting.getIncrement()) * setting.getIncrement() - setting.getMin()) / (setting.getMax() - setting.getMin()))) - setting.circlePos) * 0.2;
        RenderUtil.drawRoundedRect(ctx.getMatrices(), x, y + height - 4 - 4, (int) setting.circlePos, 4, 1, Manager.STYLE_MANAGER.getFirstColor());

        FontUtils.durman[13].drawLeftAligned(ctx.getMatrices(), setting.getName(), x, y + 2, Color.WHITE.getRGB());
        FontUtils.durman[13].drawLeftAligned(ctx.getMatrices(), String.format(Locale.US, setting.getIncrement() >= 1 ? "%.0f" : (setting.getIncrement() >= 0.1 ? "%.1f" : "%.2f"), Math.round(setting.get().doubleValue() / setting.getIncrement()) * setting.getIncrement()), x + width - FontUtils.durman[13].getWidth(String.format(Locale.US, setting.getIncrement() >= 1 ? "%.0f" : (setting.getIncrement() >= 0.1 ? "%.1f" : "%.2f"), Math.round(setting.get().doubleValue() / setting.getIncrement()) * setting.getIncrement())), y + 2, Color.WHITE.getRGB());

        setting.circleScale = Math.max(1f, Math.min(1.2f, setting.circleScale + (setting.dragging ? 0.05f : -0.05f)));

        ctx.getMatrices().push();
        ctx.getMatrices().translate(x + (float) setting.circlePos, y + height - 4 - 4 + 4 / 2f, 0);
        ctx.getMatrices().scale(setting.circleScale, setting.circleScale, 1f);
        ctx.getMatrices().translate(-(x + (float) setting.circlePos), -(y + height - 4 - 4 + 4 / 2f), 0);
        RenderUtil.drawCircle(ctx.getMatrices(), x + (float) setting.circlePos, y + height - 4 - 4 + 4 / 2f, 6f, Color.WHITE.getRGB());
        ctx.getMatrices().pop();
    }

    @Override
    public boolean mouseClicked(SliderSetting setting, double mouseX, double mouseY, int button, int x, int y, int width, int height) {
        if (button != 0) return false;

        if (RenderUtil.isInRegion(mouseX, mouseY, x, y + height - 4 - 4 - 3, width, 4 + 6)) {
            setting.set(Math.min(Math.max(Math.round((setting.getMin() + Math.min(Math.max((mouseX - x) / width, 0), 1) * (setting.getMax() - setting.getMin())) / setting.getIncrement()) * setting.getIncrement(), setting.getMin()), setting.getMax()));
            setting.dragging = true;
            return true;
        }
        return false;
    }

    public void mouseReleased(SliderSetting setting) {
        setting.dragging = false;
    }

    public void mouseDragged(SliderSetting setting, double mouseX, int x, int width) {
        if (!setting.dragging) return;

        setting.set(Math.min(Math.max(Math.round((setting.getMin() + Math.min(Math.max((mouseX - x) / width, 0), 1) * (setting.getMax() - setting.getMin())) / setting.getIncrement()) * setting.getIncrement(), setting.getMin()), setting.getMax()));
    }

    @Override
    public int getHeight() {
        return 20;
    }
}