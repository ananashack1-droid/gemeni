package sky.client.screens.mainmenu;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import sky.client.Main;
import sky.client.manager.IMinecraft;
import sky.client.manager.Manager;
import sky.client.manager.fontManager.FontUtils;
import sky.client.screens.altmanager.AltManager;
import sky.client.util.animations.Animation;
import sky.client.util.animations.impl.DecelerateAnimation;
import sky.client.util.color.ColorUtil;
import sky.client.util.render.RenderUtil;
import sky.client.util.render.providers.ResourceProvider;

import java.time.Year;

public class MainMenu extends Screen implements IMinecraft {
    private static final String[] BTNS = {"Multiplayer", "Singleplayer", "Alt Manager", "Settings", "Quit"};
    private static final String IC_USER = "\uF004", IC_QUESTION = "\uF006", IC_UP = "\uF008", IC_DOWN = "\uF007";
    private static final String IC_GEAR = "\uF009", IC_RIGHT = "\uF00B", IC_BRACKET = "\uF00A", IC_DISCORD = "\uF000", IC_YOUTUBE = "\uF001";

    private final Animation[] hoverAnims = new Animation[BTNS.length];
    private final Animation panelAnim = new DecelerateAnimation(400, 1, Direction.AxisDirection.POSITIVE);
    private final float[] socialHover = new float[2];
    private float gearRot, px, py, pw, ph;

    public MainMenu() {
        super(Text.literal("Main Menu"));
        for (int i = 0; i < hoverAnims.length; i++) hoverAnims[i] = new DecelerateAnimation(200, 1, Direction.AxisDirection.NEGATIVE);
    }

    @Override protected void init() { panelAnim.reset(); for (Animation a : hoverAnims) a.setDirection(Direction.AxisDirection.NEGATIVE); }

    private float padX() { return Math.max(30, pw * .06f); }
    private float padY() { return Math.max(25, ph * .08f); }
    private int al(float a, int v) { return (int)(v * a); }
    private int col(int v, float a) { return ColorUtil.rgba(v, v, v + 5, al(a, 255)); }
    private boolean in(double mx, double my, float x, float y, float w, float h) { return mx >= x && mx <= x + w && my >= y && my <= y + h; }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        float a = (float) panelAnim.getOutput();
        pw = width * .72f; ph = height * .7f; px = (width - pw) / 2; py = (height - ph) / 2;

        RenderUtil.drawTexture(ctx.getMatrices(), ResourceProvider.menubg, 0, 0, width, height, 0, -1);
        RenderUtil.drawBlur(ctx.getMatrices(), px, py, pw, ph, 20, 14 * a, ColorUtil.rgba(255, 255, 255, al(a, 255)));
        RenderUtil.drawRoundedRect(ctx.getMatrices(), px, py, pw, ph, 20, ColorUtil.rgba(14, 14, 18, al(a, 230)));
        RenderUtil.drawRoundedBorder(ctx.getMatrices(), px, py, pw, ph, 20, 1, ColorUtil.rgba(255, 255, 255, al(a, 18)));
        RenderUtil.drawRoundedRect(ctx.getMatrices(), px + 30, py + 1, pw - 60, 1, 0, ColorUtil.rgba(255, 255, 255, al(a, 35)));

        //FontUtils.sf_medium[12].centeredDraw(ctx.getMatrices(), "www." + Main.getInstance().name.toLowerCase().replaceAll("\\s+", "") + ".com", px + pw / 2, py + 18, col(150, a));

        renderLeft(ctx, a);
        renderMenu(ctx, mx, my, a, delta);
        renderSocial(ctx, mx, my, a);
    }

    private void renderLeft(DrawContext ctx, float a) {
        float x = px + padX(), y = py + padY() + ph * .15f;
        FontUtils.gilroy_bold[52].renderAnimatedGradientText(ctx.getMatrices(), Main.getInstance().name, x, y, ColorUtil.getColorStyle(0), ColorUtil.getColorStyle(180), (System.currentTimeMillis() % 3000L) / 3000f);
        FontUtils.sf_medium[20].drawLeftAligned(ctx.getMatrices(), "Next generation cheat client.", x, y + 45, col(180, a));

        float footY = py + ph - padY() - 10;
        String role = Manager.USER_PROFILE.getRole();
        float roleW = FontUtils.sf_medium[11].getWidth(role) + 16;
        RenderUtil.drawRoundedRect(ctx.getMatrices(), x, footY - 5, roleW, 18, 5, ColorUtil.rgba(255, 255, 255, al(a, 25)));
        FontUtils.sf_medium[11].centeredDraw(ctx.getMatrices(), role, x + roleW / 2, footY + 4 - FontUtils.sf_medium[11].getHeight() / 2, ColorUtil.rgba(255, 255, 255, al(a, 240)));
        FontUtils.sf_medium[12].drawLeftAligned(ctx.getMatrices(), "© " + Year.now().getValue() + " " + Main.getInstance().name + ". All rights reserved.", x + roleW + 10, footY + 4 - FontUtils.sf_medium[12].getHeight() / 2, col(150, a));
    }

    private void renderMenu(DrawContext ctx, int mx, int my, float a, float delta) {
        float rx = px + pw - padX(), sy = py + ph * .26f;
        for (int i = 0; i < BTNS.length; i++) {
            float[] b = bounds(i, rx, sy);
            boolean hov = in(mx, my, b[0], b[1], b[2], b[3]);
            hoverAnims[i].setDirection(hov ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
            float p = (float) hoverAnims[i].getOutput();

            if (p > .01f) RenderUtil.drawRoundedRect(ctx.getMatrices(), b[0], b[1], b[2], b[3], 8, ColorUtil.rgba(255, 255, 255, (int)(18 * p * a)));

            float cy = b[1] + b[3] / 2, ix = b[0] + 28;
            renderIcon(ctx, i, ix, cy, p, a, delta);
            FontUtils.sf_medium[16].drawLeftAligned(ctx.getMatrices(), BTNS[i], ix + 20 - 3 * p, cy - FontUtils.sf_medium[16].getHeight() / 2, ColorUtil.blendColorsInt(col(180, a), ColorUtil.rgba(255, 255, 255, al(a, 255)), p));
        }
    }

    private void renderIcon(DrawContext ctx, int i, float cx, float cy, float p, float a, float delta) {
        if (p < .01f) return;
        int al = (int)(255 * p * a), c = ColorUtil.rgba(255, 255, 255, al);
        float h = FontUtils.icomoon[16].getHeight() / 2;

        switch (i) {
            case 0 -> { int sa = (int)(180 * p * a); FontUtils.icomoon[16].centeredDraw(ctx.getMatrices(), IC_USER, cx - 5 * p, cy - h, ColorUtil.rgba(255, 255, 255, sa)); FontUtils.icomoon[16].centeredDraw(ctx.getMatrices(), IC_USER, cx + 5 * p, cy - h, ColorUtil.rgba(255, 255, 255, sa)); FontUtils.icomoon[16].centeredDraw(ctx.getMatrices(), IC_USER, cx, cy - h, c); }
            case 1 -> { FontUtils.icomoon[16].centeredDraw(ctx.getMatrices(), IC_USER, cx, cy - h, c); FontUtils.icomoon[9].centeredDraw(ctx.getMatrices(), IC_QUESTION, cx + 5, cy - h - 6 - 2 * p, ColorUtil.reAlphaInt(ColorUtil.getColorStyle(0), al)); }
            case 2 -> { float off = 2 - 3 * p; FontUtils.icomoon[16].centeredDraw(ctx.getMatrices(), IC_DOWN, cx - off, cy - h - off, ColorUtil.rgba(255, 255, 255, (int)(al * .7f))); FontUtils.icomoon[16].centeredDraw(ctx.getMatrices(), IC_UP, cx - off, cy - h + off, c); }
            case 3 -> { gearRot += p * delta * 6; if (gearRot > 360) gearRot -= 360; ctx.getMatrices().push(); ctx.getMatrices().translate(cx, cy, 0); ctx.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(gearRot)); FontUtils.icomoon[16].centeredDraw(ctx.getMatrices(), IC_GEAR, 0, -h, c); ctx.getMatrices().pop(); }
            case 4 -> { FontUtils.icomoon[16].centeredDraw(ctx.getMatrices(), IC_BRACKET, cx, cy - h, c); FontUtils.icomoon[11].centeredDraw(ctx.getMatrices(), IC_RIGHT, cx + 2 + 3 * p, cy - h + 1, c); }
        }
    }

    private void renderSocial(DrawContext ctx, int mx, int my, float a) {
        String[] icons = {IC_DISCORD, IC_YOUTUBE};
        float x = px + pw - padX() - 10, y = py + ph - padY() - 5;
        for (int i = 0; i < 2; i++) {
            boolean hov = in(mx, my, x - 12, y - 12, 24, 24);
            socialHover[i] = Math.max(0, Math.min(1, socialHover[i] + (hov ? .12f : -.08f)));
            int c = ColorUtil.blendColorsInt(col(150, a), ColorUtil.rgba(255, 255, 255, al(a, 255)), socialHover[i]);
            ctx.getMatrices().push();
            ctx.getMatrices().translate(x, y - socialHover[i] * 3, 0);
            ctx.getMatrices().scale(1 + socialHover[i] * .12f, 1 + socialHover[i] * .12f, 1);
            FontUtils.icomoon[14].centeredDraw(ctx.getMatrices(), icons[i], 0, -FontUtils.icomoon[14].getHeight() / 2, c);
            ctx.getMatrices().pop();
            x -= 26;
        }
    }

    private float[] bounds(int i, float rx, float sy) {
        float w = 70 + FontUtils.sf_medium[16].getWidth(BTNS[i]), h = 32;
        return new float[]{rx - w + 16, sy + i * 36 - 12, w, h};
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX, my = (int) mouseY;
        float rx = px + pw - padX(), sy = py + ph * .26f;
        for (int i = 0; i < BTNS.length; i++) {
            float[] b = bounds(i, rx, sy);
            if (in(mx, my, b[0], b[1], b[2], b[3])) {
                switch (i) {
                    case 0 -> client.setScreen(new MultiplayerScreen(this));
                    case 1 -> client.setScreen(new SelectWorldScreen(this));
                    case 2 -> client.setScreen(new AltManager(this));
                    case 3 -> client.setScreen(new OptionsScreen(this, client.options));
                    case 4 -> client.scheduleStop();
                }
                return true;
            }
        }
        float sx = px + pw - padX() - 10, ssy = py + ph - padY() - 5;
        for (int i = 0; i < 2; i++) { if (in(mx, my, sx - 12, ssy - 12, 24, 24)) return true; sx -= 26; }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
}