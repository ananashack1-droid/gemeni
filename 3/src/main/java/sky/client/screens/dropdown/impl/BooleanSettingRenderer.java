package sky.client.screens.dropdown.impl;

import net.minecraft.client.gui.DrawContext;
import sky.client.manager.IMinecraft;
import sky.client.manager.Manager;
import sky.client.modules.setting.BooleanSetting;
import sky.client.screens.dropdown.DescriptionRenderQueue;
import sky.client.screens.dropdown.SettingRenderer;
import sky.client.manager.fontManager.FontUtils;
import sky.client.util.color.ColorUtil;
import sky.client.util.render.RenderUtil;
import sky.client.util.render.Scissor;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class BooleanSettingRenderer implements SettingRenderer<BooleanSetting>, IMinecraft {

    private final Map<BooleanSetting, Float> toggleMap = new HashMap<>();
    private final Map<BooleanSetting, Float> scrollMap = new HashMap<>();

    @Override
    public int getHeight() {
        return 16;
    }

    @Override
    public void render(DrawContext ctx, BooleanSetting setting, int x, int y, int width, int height) {
        toggleMap.put(setting, toggleMap.getOrDefault(setting, setting.get() ? 1f : 0f) + ((setting.get() ? 1f : 0f) - toggleMap.getOrDefault(setting, setting.get() ? 1f : 0f)) * 0.15f);

        RenderUtil.drawRoundedRect(ctx.getMatrices(), x + width - 22 + 4, y + (16 - 12) / 2 - 2, 22, 12, 5, ColorUtil.interpolateColor(new Color(50, 50, 50, 200).getRGB(), Manager.STYLE_MANAGER.getFirstColor(), toggleMap.get(setting)));
        RenderUtil.drawCircle(ctx.getMatrices(), (x + width - 22 + 4) + 3 + (22 - 8 - 5) * toggleMap.get(setting) + 8 / 2f, (y + (16 - 12) / 2 - 2) + (12 - 8) / 2f + 8 / 2f, 8, Color.WHITE.getRGB());

        scrollMap.put(setting, RenderUtil.isHovered((int) (mc.mouse.getX() / mc.getWindow().getScaleFactor()), (int) (mc.mouse.getY() / mc.getWindow().getScaleFactor()), x, (int) (y + (16 - FontUtils.durman[13].getHeight()) / 2) - 3, (x + width - 22 + 4) - x - 4, FontUtils.durman[13].getHeight() + 2) && (FontUtils.durman[13].getWidth(setting.getName()) - ((x + width - 22 + 4) - x - 4)) > 0 ? Math.min(scrollMap.getOrDefault(setting, 0f) + 0.5f, FontUtils.durman[13].getWidth(setting.getName()) - ((x + width - 22 + 4) - x - 4)) : Math.max(scrollMap.getOrDefault(setting, 0f) - 0.5f, 0));

        Scissor.push();
        Scissor.setFromComponentCoordinates(x, (int) (y + (16 - FontUtils.durman[13].getHeight()) / 2) - 3, (int) ((x + width - 22 + 4) - x - 4), FontUtils.durman[13].getHeight() + 2);
        FontUtils.durman[13].drawLeftAligned(ctx.getMatrices(), setting.getName(), x - scrollMap.get(setting), (int) (y + (16 - FontUtils.durman[13].getHeight()) / 2) - 2, Color.WHITE.getRGB());
        Scissor.pop();

        if (mc.mouse.getX() / mc.getWindow().getScaleFactor() >= x + width - 22 + 4 && mc.mouse.getX() / mc.getWindow().getScaleFactor() <= x + width - 22 + 4 + 22 && mc.mouse.getY() / mc.getWindow().getScaleFactor() >= y + (16 - 12) / 2 - 2 && mc.mouse.getY() / mc.getWindow().getScaleFactor() <= y + (16 - 12) / 2 - 2 + 12 && setting.getDesc() != null && !setting.getDesc().isEmpty()) {
            DescriptionRenderQueue.add(setting.getDesc(), (float) (mc.mouse.getX() / mc.getWindow().getScaleFactor()) + 6, (float) (mc.mouse.getY() / mc.getWindow().getScaleFactor()) + 6);
        }
    }

    @Override
    public boolean mouseClicked(BooleanSetting setting, double mouseX, double mouseY, int button, int x, int y, int width, int height) {
        if (button != 0) return false;

        if (mouseX >= x + width - 22 + 4 && mouseX <= x + width - 22 + 4 + 22
                && mouseY >= y + (16 - 12) / 2 - 2 && mouseY <= y + (16 - 12) / 2 - 2 + 12) {
            setting.set(!setting.get());
            return true;
        }
        return false;
    }
}