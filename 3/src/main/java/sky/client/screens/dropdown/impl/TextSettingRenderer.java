package sky.client.screens.dropdown.impl;

import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import sky.client.modules.setting.TextSetting;
import sky.client.screens.dropdown.SettingRenderer;
import sky.client.manager.fontManager.FontUtils;
import sky.client.util.render.RenderUtil;
import sky.client.util.render.Scissor;

import java.awt.*;

public class TextSettingRenderer implements SettingRenderer<TextSetting> {

    private long lastBlinkTime = 0;
    private float scrollOffset = 0;

    @Override
    public void render(DrawContext ctx, TextSetting setting, int x, int y, int width, int height) {
        FontUtils.durman[13].drawLeftAligned(ctx.getMatrices(), setting.getName(), x, y, Color.WHITE.getRGB());
        RenderUtil.drawRoundedRect(ctx.getMatrices(), x, y + (int) FontUtils.durman[13].getHeight() + 3, Math.max(60, Math.min((int) FontUtils.durman[13].getWidth(setting.getValue()) + 12, 105)), (int) (FontUtils.durman[13].getHeight() + 6) - 1, 3, setting.isFocused() ? new Color(60, 60, 60, 200).getRGB() : new Color(40, 40, 40, 200).getRGB());

        scrollOffset += ((FontUtils.durman[13].getWidth(setting.getValue().substring(0, setting.getCursorPosition())) + 6 > Math.max(60, Math.min((int) FontUtils.durman[13].getWidth(setting.getValue()) + 12, 105)) ? Math.max(60, Math.min((int) FontUtils.durman[13].getWidth(setting.getValue()) + 12, 105)) - (FontUtils.durman[13].getWidth(setting.getValue().substring(0, setting.getCursorPosition())) + 6) : (FontUtils.durman[13].getWidth(setting.getValue().substring(0, setting.getCursorPosition())) + 6 < 0 ? -(FontUtils.durman[13].getWidth(setting.getValue().substring(0, setting.getCursorPosition())) + 6) : 0)) - scrollOffset) * 0.2f;

        Scissor.push();
        Scissor.setFromComponentCoordinates(x + 6, y + (int) FontUtils.durman[13].getHeight() + 3, Math.max(60, Math.min((int) FontUtils.durman[13].getWidth(setting.getValue()) + 12, 105)) - 12, (int) (FontUtils.durman[13].getHeight() + 6));

        FontUtils.durman[13].drawLeftAligned(ctx.getMatrices(), setting.getValue(), x + 6 + scrollOffset, y + (int) FontUtils.durman[13].getHeight() + 3 + 3 - 0.6f, Color.WHITE.getRGB());
        Scissor.pop();

        if (setting.isFocused()) {
            if (System.currentTimeMillis() - lastBlinkTime > 500) {
                setting.cursorVisible = !setting.cursorVisible;
                lastBlinkTime = System.currentTimeMillis();
            }
            if (setting.cursorVisible) {
                RenderUtil.drawRoundedRect(ctx.getMatrices(), x + 6 + FontUtils.durman[13].getWidth(setting.getValue().substring(0, setting.getCursorPosition())) + scrollOffset, y + (int) FontUtils.durman[13].getHeight() + 3 + 3, 1, FontUtils.durman[13].getHeight(), 0, Color.WHITE.getRGB());
            }
        }
    }

    @Override
    public boolean mouseClicked(TextSetting setting, double mouseX, double mouseY, int button, int x, int y, int width, int height) {
        if (button != 0) return false;

        setting.setFocused(mouseX >= x && mouseX <= x + Math.max(60, Math.min((int) FontUtils.durman[13].getWidth(setting.getValue()) + 12, 105)) && mouseY >= y + (int) FontUtils.durman[13].getHeight() + 2 && mouseY <= y + (int) FontUtils.durman[13].getHeight() + 2 + (int) (FontUtils.durman[13].getHeight() + 6));

        if (setting.isFocused()) {
            setting.setCursorPosition(setting.getValue().length());
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(TextSetting setting, int keyCode, int scanCode, int modifiers) {
        if (!setting.isFocused()) return false;

        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            setting.setFocused(false);
        } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE && setting.getCursorPosition() > 0) {
            setting.setValue(setting.getValue().substring(0, setting.getCursorPosition() - 1) + setting.getValue().substring(setting.getCursorPosition()));
            setting.setCursorPosition(setting.getCursorPosition() - 1);
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_DELETE && setting.getCursorPosition() < setting.getValue().length()) {
            setting.setValue(setting.getValue().substring(0, setting.getCursorPosition()) + setting.getValue().substring(setting.getCursorPosition() + 1));
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_LEFT) {
            setting.setCursorPosition(Math.max(0, setting.getCursorPosition() - 1));
        } else if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            setting.setCursorPosition(Math.min(setting.getValue().length(), setting.getCursorPosition() + 1));
        } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            setting.setFocused(false);
        }
        return true;
    }

    @Override
    public boolean charTyped(TextSetting setting, char c, int modifiers) {
        if (!setting.isFocused() || Character.isISOControl(c)) return false;

        setting.setValue(setting.getValue().substring(0, setting.getCursorPosition()) + c + setting.getValue().substring(setting.getCursorPosition()));
        setting.setCursorPosition(setting.getCursorPosition() + 1);
        return true;
    }

    @Override
    public int getHeight() {
        return (int) FontUtils.durman[13].getHeight() + 2 + (int) (FontUtils.durman[13].getHeight() + 10.5f);
    }
}