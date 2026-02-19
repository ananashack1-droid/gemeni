package sky.client.screens.dropdown.impl;

import net.minecraft.client.gui.DrawContext;
import sky.client.manager.ClientManager;
import sky.client.manager.IMinecraft;
import sky.client.manager.Manager;
import sky.client.modules.setting.BindBooleanSetting;
import sky.client.screens.dropdown.DescriptionRenderQueue;
import sky.client.screens.dropdown.SettingRenderer;
import sky.client.util.color.ColorUtil;
import sky.client.manager.fontManager.FontUtils;
import sky.client.util.render.RenderUtil;
import sky.client.util.render.Scissor;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class BindBooleanSettingRenderer implements SettingRenderer<BindBooleanSetting>, IMinecraft {

    private final Map<BindBooleanSetting, Float> toggleProgressMap = new HashMap<>();
    private final Map<BindBooleanSetting, Float> scrollOffsetMap = new HashMap<>();
    private final Map<BindBooleanSetting, Float> bindBoxProgressMap = new HashMap<>();

    @Override
    public int getHeight() {
        return 16;
    }

    @Override
    public void render(DrawContext ctx, BindBooleanSetting setting, int x, int y, int width, int height) {
        toggleProgressMap.put(setting, toggleProgressMap.getOrDefault(setting, setting.get() ? 1f : 0f) + ((setting.get() ? 1f : 0f) - toggleProgressMap.getOrDefault(setting, setting.get() ? 1f : 0f)) * 0.15f);

        if (setting.expanded) {
            bindBoxProgressMap.put(setting, Math.min(1f, bindBoxProgressMap.getOrDefault(setting, 0f) + (1f - bindBoxProgressMap.getOrDefault(setting, 0f)) * 0.2f));

            if (System.currentTimeMillis() - setting.lastDotUpdate >= 400) {
                setting.dotState = (setting.dotState + 1) % 4;
                setting.lastDotUpdate = System.currentTimeMillis();
            }

            RenderUtil.drawRoundedRect(ctx.getMatrices(), (int) ((x - 2) - ((10 + FontUtils.durman[13].getWidth(setting.isListeningForBind() ? "Binding" + ".".repeat(setting.dotState) : (setting.getBindKey() != -1 ? ClientManager.getKey(setting.getBindKey()) : "NONE"))) + 10) * (1 - bindBoxProgressMap.get(setting))), y + (16 - 14) / 2, (int) (10 + FontUtils.durman[13].getWidth(setting.isListeningForBind() ? "Binding" + ".".repeat(setting.dotState) : (setting.getBindKey() != -1 ? ClientManager.getKey(setting.getBindKey()) : "NONE"))), 14, 3, new Color(40, 40, 40, 200).getRGB());

            scrollOffsetMap.put(setting, RenderUtil.isInRegion(mc.mouse.getX() / mc.getWindow().getScaleFactor(), mc.mouse.getY() / mc.getWindow().getScaleFactor(), (int) ((x - 2) - ((10 + FontUtils.durman[13].getWidth(setting.isListeningForBind() ? "Binding" + ".".repeat(setting.dotState) : (setting.getBindKey() != -1 ? ClientManager.getKey(setting.getBindKey()) : "NONE"))) + 10) * (1 - bindBoxProgressMap.get(setting))), y + (16 - 14) / 2, (int) (10 + FontUtils.durman[13].getWidth(setting.isListeningForBind() ? "Binding" + ".".repeat(setting.dotState) : (setting.getBindKey() != -1 ? ClientManager.getKey(setting.getBindKey()) : "NONE"))), 14) && (FontUtils.durman[13].getWidth(setting.isListeningForBind() ? "Binding" + ".".repeat(setting.dotState) : (setting.getBindKey() != -1 ? ClientManager.getKey(setting.getBindKey()) : "NONE")) - ((int) (10 + FontUtils.durman[13].getWidth(setting.isListeningForBind() ? "Binding" + ".".repeat(setting.dotState) : (setting.getBindKey() != -1 ? ClientManager.getKey(setting.getBindKey()) : "NONE"))) - 10)) > 0 ? Math.min(scrollOffsetMap.getOrDefault(setting, 0f) + 0.1f, FontUtils.durman[13].getWidth(setting.isListeningForBind() ? "Binding" + ".".repeat(setting.dotState) : (setting.getBindKey() != -1 ? ClientManager.getKey(setting.getBindKey()) : "NONE")) - ((int) (10 + FontUtils.durman[13].getWidth(setting.isListeningForBind() ? "Binding" + ".".repeat(setting.dotState) : (setting.getBindKey() != -1 ? ClientManager.getKey(setting.getBindKey()) : "NONE"))) - 10)) : Math.max(scrollOffsetMap.getOrDefault(setting, 0f) - 0.1f, 0));
            FontUtils.durman[13].drawLeftAligned(ctx.getMatrices(), setting.isListeningForBind() ? "Binding" + ".".repeat(setting.dotState) : (setting.getBindKey() != -1 ? ClientManager.getKey(setting.getBindKey()) : "NONE"), (int) ((x - 2) - ((10 + FontUtils.durman[13].getWidth(setting.isListeningForBind() ? "Binding" + ".".repeat(setting.dotState) : (setting.getBindKey() != -1 ? ClientManager.getKey(setting.getBindKey()) : "NONE"))) + 10) * (1 - bindBoxProgressMap.get(setting))) + 5 - scrollOffsetMap.get(setting), (y + (16 - 14) / 2) + (14 - FontUtils.durman[13].getHeight()) / 2f, Color.WHITE.getRGB());
            RenderUtil.drawTexture(ctx.getMatrices(), "images/gui/fl.png", x + width - 12 - 8 + 10, (y + (16 - 14) / 2) + (14 - 12) / 2 - 1, 12, 12, 0, Color.RED.getRGB());
            return;
        } else {
            bindBoxProgressMap.put(setting, Math.max(0f, bindBoxProgressMap.getOrDefault(setting, 0f) + (0f - bindBoxProgressMap.getOrDefault(setting, 0f)) * 0.2f));
        }

        RenderUtil.drawRoundedRect(ctx.getMatrices(), x + width - 22 + 4, y + (16 - 12) / 2 - 2, 22, 12, 5, ColorUtil.interpolateColor(new Color(50, 50, 50, 200).getRGB(), Manager.STYLE_MANAGER.getFirstColor(), toggleProgressMap.get(setting)));
        RenderUtil.drawCircle(ctx.getMatrices(), (x + width - 22 + 4) + 3 + (22 - 8 - 5) * toggleProgressMap.get(setting) + 8 / 2f, (y + (16 - 12) / 2 - 2) + (12 - 8) / 2f + 8 / 2f, 8, Color.WHITE.getRGB());

        scrollOffsetMap.put(setting, RenderUtil.isInRegion(mc.mouse.getX() / mc.getWindow().getScaleFactor(), mc.mouse.getY() / mc.getWindow().getScaleFactor(), x, y, width, height) && (FontUtils.durman[13].getWidth(setting.getName()) - (x + width - 22 + 4 - x - 15)) > 0 ? Math.min(scrollOffsetMap.getOrDefault(setting, 0f) + 0.5f, FontUtils.durman[13].getWidth(setting.getName()) - (x + width - 22 + 4 - x - 15)) : Math.max(scrollOffsetMap.getOrDefault(setting, 0f) - 0.5f, 0));

        Scissor.push();
        Scissor.setFromComponentCoordinates(x, (int) (y + (16 - FontUtils.durman[13].getHeight()) / 2) - 3, (int) (x + width - 22 + 4 - x - 15), FontUtils.durman[13].getHeight() + 2);
        FontUtils.durman[13].drawLeftAligned(ctx.getMatrices(), setting.getName(), x - scrollOffsetMap.get(setting), (int) (y + (16 - FontUtils.durman[13].getHeight()) / 2) - 2, Color.WHITE.getRGB());
        Scissor.pop();

        FontUtils.iconsWex[24].centeredDraw(ctx.getMatrices(), "H", x + width - 25, y + 16 / 2f - 6, Color.WHITE.getRGB());

        if (RenderUtil.isInRegion(mc.mouse.getX() / mc.getWindow().getScaleFactor(), mc.mouse.getY() / mc.getWindow().getScaleFactor(), x + width - 22 + 4, y + (16 - 12) / 2 - 2, 22, 12) && setting.getDesc() != null && !setting.getDesc().isEmpty()) {
            DescriptionRenderQueue.add(setting.getDesc(), (float) (mc.mouse.getX() / mc.getWindow().getScaleFactor()) + 6, (float) (mc.mouse.getY() / mc.getWindow().getScaleFactor()) + 6);
        }
    }

    @Override
    public boolean mouseClicked(BindBooleanSetting setting, double mouseX, double mouseY, int button, int x, int y, int width, int height) {
        if (button != 0) return false;

        if (!setting.expanded && RenderUtil.isInRegion(mouseX, mouseY, x + width - 22 + 4, y + (16 - 12) / 2 - 2, 22, 12)) {
            setting.set(!setting.get());
            return true;
        }

        if (RenderUtil.isInRegion(mouseX, mouseY, x + width - 25 - 16 / 2, y + (16 / 2) - 16 / 2 - 2, 16, 16)) {
            setting.expanded = true;
            setting.setListeningForBind(false);
            return true;
        }

        if (setting.expanded) {
            if (RenderUtil.isInRegion(mouseX, mouseY, (int) (x - 2 - ((int) (10 + FontUtils.durman[13].getWidth(setting.isListeningForBind() ? "Binding" : (setting.getBindKey() != -1 ? ClientManager.getKey(setting.getBindKey()) : "NONE"))) + 10) * (1 - bindBoxProgressMap.getOrDefault(setting, 1f))), y + (16 - 14) / 2 - 2, (int) (10 + FontUtils.durman[13].getWidth(setting.isListeningForBind() ? "Binding" : (setting.getBindKey() != -1 ? ClientManager.getKey(setting.getBindKey()) : "NONE"))), 14)) {
                setting.setListeningForBind(true);
                return true;
            }

            if (RenderUtil.isInRegion(mouseX, mouseY, x + width - 12 - 8 + 10, y + (16 - 14) / 2 - 2 + (14 - 12) / 2 - 2, 12, 12)) {
                setting.expanded = false;
                setting.setListeningForBind(false);
                return true;
            }
        }

        return false;
    }
}