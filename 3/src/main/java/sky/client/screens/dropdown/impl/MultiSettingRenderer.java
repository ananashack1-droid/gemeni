package sky.client.screens.dropdown.impl;

import net.minecraft.client.gui.DrawContext;
import sky.client.manager.IMinecraft;
import sky.client.manager.Manager;
import sky.client.modules.setting.MultiSetting;
import sky.client.screens.dropdown.SettingRenderer;
import sky.client.util.color.ColorUtil;
import sky.client.manager.fontManager.FontUtils;
import sky.client.util.render.RenderUtil;
import sky.client.util.render.Scissor;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiSettingRenderer implements SettingRenderer<MultiSetting>, IMinecraft {
    private final Map<String, Float> scrollOffsets = new HashMap<>();
    private final Map<String, Float> hoverProgress = new HashMap<>();

    public int getHeight(MultiSetting setting, int width) {
        AtomicInteger currentX = new AtomicInteger(0);
        AtomicInteger lines = new AtomicInteger(1);

        setting.getAvailableModes().forEach(mode -> {
            if (currentX.get() + (int) FontUtils.durman[13].getWidth(mode) + 6 > width) {
                currentX.set(0);
                lines.incrementAndGet();
            }
            currentX.addAndGet((int) FontUtils.durman[13].getWidth(mode) + 6 + 4);
        });

        return 14 + lines.get() * 14;
    }

    @Override
    public int getHeight() {
        return 14 + 14;
    }

    @Override
    public void render(DrawContext ctx, MultiSetting setting, int x, int y, int width, int height) {
        FontUtils.durman[13].drawLeftAligned(ctx.getMatrices(), setting.getName(), x, y, Color.WHITE.getRGB());

        FontUtils.durman[13].drawLeftAligned(ctx.getMatrices(), setting.getAllSelected() + "/" + setting.getAvailableModes().size(), x + width - FontUtils.durman[13].getWidth(setting.getAllSelected() + "/" + setting.getAvailableModes().size()) - 2, y, new Color(180, 180, 180).getRGB());

        int currentX = x;
        int currentY = y + 13;

        for (String mode : setting.getAvailableModes()) {
            if (currentX + (int) FontUtils.durman[13].getWidth(mode) + 6 > x + width) {
                currentX = x;
                currentY += 14;
            }

            hoverProgress.put(mode, setting.get(mode) ? 1f : hoverProgress.getOrDefault(mode, 0f) + ((RenderUtil.isInRegion(mc.mouse.getX() / mc.getWindow().getScaleFactor(), mc.mouse.getY() / mc.getWindow().getScaleFactor(), currentX, currentY, (int) FontUtils.durman[13].getWidth(mode) + 6, 12) ? 1f : 0f) - hoverProgress.getOrDefault(mode, 0f)) * 0.08f);

            RenderUtil.drawRoundedRect(ctx.getMatrices(), currentX, currentY, (int) FontUtils.durman[13].getWidth(mode) + 6, 12, 1, setting.get(mode) ? Manager.STYLE_MANAGER.getFirstColor() : new Color(30, 30, 30, 180).getRGB());

            scrollOffsets.put(mode, RenderUtil.isInRegion(mc.mouse.getX() / mc.getWindow().getScaleFactor(), mc.mouse.getY() / mc.getWindow().getScaleFactor(), currentX, currentY, (int) FontUtils.durman[13].getWidth(mode) + 6, 12) && (FontUtils.durman[13].getWidth(mode) - ((int) FontUtils.durman[13].getWidth(mode) + 6 - 6)) > 0 ? Math.min(scrollOffsets.getOrDefault(mode, 0f) + 0.5f, FontUtils.durman[13].getWidth(mode) - ((int) FontUtils.durman[13].getWidth(mode) + 6 - 6)) : Math.max(scrollOffsets.getOrDefault(mode, 0f) - 0.5f, 0));

            Scissor.push();
            Scissor.setFromComponentCoordinates(currentX + 3, currentY, (int) FontUtils.durman[13].getWidth(mode), 12);
            FontUtils.durman[13].drawLeftAligned(ctx.getMatrices(), mode, currentX + 3 - scrollOffsets.get(mode), currentY + (12 - FontUtils.durman[13].getHeight()) / 2f, ColorUtil.interpolateColor(new Color(200, 200, 200).getRGB(), Color.WHITE.getRGB(), hoverProgress.get(mode)));
            Scissor.pop();

            currentX += (int) FontUtils.durman[13].getWidth(mode) + 6 + 4;
        }
    }

    @Override
    public boolean mouseClicked(MultiSetting setting, double mouseX, double mouseY, int button, int x, int y, int width, int height) {
        if (button != 0) return false;

        int currentX = x;
        int currentY = y + 13;

        for (String mode : setting.getAvailableModes()) {
            if (currentX + (int) FontUtils.durman[13].getWidth(mode) + 6 > x + width) {
                currentX = x;
                currentY += 14;
            }

            if (RenderUtil.isInRegion(mouseX, mouseY, currentX, currentY, (int) FontUtils.durman[13].getWidth(mode) + 6, 12)) {
                setting.toggle(mode);
                return true;
            }

            currentX += (int) FontUtils.durman[13].getWidth(mode) + 6 + 4;
        }

        return false;
    }
}