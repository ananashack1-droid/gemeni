package sky.client.screens.dropdown.impl;

import net.minecraft.client.gui.DrawContext;
import sky.client.manager.ClientManager;
import sky.client.manager.IMinecraft;
import sky.client.modules.setting.BindSetting;
import sky.client.screens.dropdown.SettingRenderer;
import sky.client.manager.fontManager.FontUtils;
import sky.client.util.render.RenderUtil;
import sky.client.util.render.Scissor;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class BindSettingRenderer implements SettingRenderer<BindSetting>, IMinecraft {

    private final Map<BindSetting, Float> nameScrollOffsetMap = new HashMap<>();

    @Override
    public void render(DrawContext ctx, BindSetting setting, int x, int y, int width, int height) {
        nameScrollOffsetMap.put(setting, RenderUtil.isInRegion(mc.mouse.getX() / mc.getWindow().getScaleFactor(), mc.mouse.getY() / mc.getWindow().getScaleFactor(), x, y, (x + width - Math.max(12, (int) FontUtils.durman[13].getWidth(setting.isBinding() ? "Binding" + ".".repeat((int) ((System.currentTimeMillis() / 400) % 4)) : (setting.getKey() != -1 ? ClientManager.getKey(setting.getKey()) : "NONE")) + 10) - 4) - x, 15) && (FontUtils.durman[13].getWidth(setting.getName()) - ((x + width - Math.max(12, (int) FontUtils.durman[13].getWidth(setting.isBinding() ? "Binding" + ".".repeat((int) ((System.currentTimeMillis() / 400) % 4)) : (setting.getKey() != -1 ? ClientManager.getKey(setting.getKey()) : "NONE")) + 10)) - x - 4)) > 0 ? Math.min(nameScrollOffsetMap.getOrDefault(setting, 0f) + 1f, FontUtils.durman[13].getWidth(setting.getName()) - ((x + width - Math.max(12, (int) FontUtils.durman[13].getWidth(setting.isBinding() ? "Binding" + ".".repeat((int) ((System.currentTimeMillis() / 400) % 4)) : (setting.getKey() != -1 ? ClientManager.getKey(setting.getKey()) : "NONE")) + 10)) - x - 4)) : Math.max(nameScrollOffsetMap.getOrDefault(setting, 0f) - 1f, 0));

        Scissor.push();
        Scissor.setFromComponentCoordinates(x, y + (15 - FontUtils.durman[13].getHeight()) / 2 - 1, (x + width - Math.max(12, (int) FontUtils.durman[13].getWidth(setting.isBinding() ? "Binding" + ".".repeat((int) ((System.currentTimeMillis() / 400) % 4)) : (setting.getKey() != -1 ? ClientManager.getKey(setting.getKey()) : "NONE")) + 10)) - x - 4, FontUtils.durman[13].getHeight() + 2);
        FontUtils.durman[13].drawLeftAligned(ctx.getMatrices(), setting.getName(), x - nameScrollOffsetMap.get(setting), y + (15 - FontUtils.durman[13].getHeight()) / 2, Color.WHITE.getRGB());
        Scissor.pop();

        RenderUtil.drawRoundedRect(ctx.getMatrices(), x + width - Math.max(12, (int) FontUtils.durman[13].getWidth(setting.isBinding() ? "Binding" + ".".repeat((int) ((System.currentTimeMillis() / 400) % 4)) : (setting.getKey() != -1 ? ClientManager.getKey(setting.getKey()) : "NONE")) + 10), y + (15 - 12) / 2, Math.max(12, (int) FontUtils.durman[13].getWidth(setting.isBinding() ? "Binding" + ".".repeat((int) ((System.currentTimeMillis() / 400) % 4)) : (setting.getKey() != -1 ? ClientManager.getKey(setting.getKey()) : "NONE")) + 10), 12, 3, new Color(40, 40, 40, 200).getRGB());
        FontUtils.durman[13].centeredDraw(ctx.getMatrices(), setting.isBinding() ? "Binding" + ".".repeat((int) ((System.currentTimeMillis() / 400) % 4)) : (setting.getKey() != -1 ? ClientManager.getKey(setting.getKey()) : "NONE"), (x + width - Math.max(12, (int) FontUtils.durman[13].getWidth(setting.isBinding() ? "Binding" + ".".repeat((int) ((System.currentTimeMillis() / 400) % 4)) : (setting.getKey() != -1 ? ClientManager.getKey(setting.getKey()) : "NONE")) + 10)) + Math.max(12, (int) FontUtils.durman[13].getWidth(setting.isBinding() ? "Binding" + ".".repeat((int) ((System.currentTimeMillis() / 400) % 4)) : (setting.getKey() != -1 ? ClientManager.getKey(setting.getKey()) : "NONE")) + 10) / 2f, (y + (15 - 12) / 2) + (12 - FontUtils.durman[13].getHeight()) / 2f, Color.WHITE.getRGB());
    }

    @Override
    public boolean mouseClicked(BindSetting setting, double mouseX, double mouseY, int button, int x, int y, int width, int height) {
        if (button != 0) return false;

        if (RenderUtil.isInRegion(mouseX, mouseY, x + width - Math.max(12, (int) FontUtils.durman[13].getWidth(setting.isBinding() ? "Binding" : (setting.getKey() != -1 ? ClientManager.getKey(setting.getKey()) : "NONE")) + 10), y + (15 - 12) / 2, Math.max(12, (int) FontUtils.durman[13].getWidth(setting.isBinding() ? "Binding" : (setting.getKey() != -1 ? ClientManager.getKey(setting.getKey()) : "NONE")) + 10), 12)) {
            setting.setBinding(!setting.isBinding());
            return true;
        }
        return false;
    }

    @Override
    public int getHeight() {
        return 15;
    }
}