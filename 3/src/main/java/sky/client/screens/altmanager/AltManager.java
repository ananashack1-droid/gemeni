package sky.client.screens.altmanager;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import org.lwjgl.glfw.GLFW;
import sky.client.Main;
import sky.client.manager.ClientManager;
import sky.client.manager.IMinecraft;
import sky.client.manager.Manager;
import sky.client.manager.fontManager.FontUtils;
import sky.client.util.animations.Animation;
import sky.client.util.animations.impl.DecelerateAnimation;
import sky.client.util.color.ColorUtil;
import sky.client.util.render.RenderUtil;
import sky.client.util.render.Scissor;
import sky.client.util.render.providers.ResourceProvider;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("All")
public class AltManager extends Screen implements IMinecraft {
    private final Screen parent;
    private final List<String> accounts = Manager.ACCOUNT_MANAGER.getAccounts();
    private List<String> filteredAccounts = new ArrayList<>(accounts);

    private static final String[] ACTIONS = {"Create", "Random", "Clear All"};
    private static final String IC_USER = "\uF004", IC_ARROW = "\uF00B", IC_PLUS = "\uF00D", IC_TRASH = "\uF00C", IC_SHUFFLE = "\uF00F", IC_SEARCH = "\uF010";

    private final Animation panelAnim = new DecelerateAnimation(400, 1, Direction.AxisDirection.POSITIVE);
    private final Animation[] actionAnims = new Animation[ACTIONS.length];
    private final Animation backAnim = new DecelerateAnimation(200, 1, Direction.AxisDirection.NEGATIVE);
    private final Animation createDlgAnim = new DecelerateAnimation(200, 1, Direction.AxisDirection.NEGATIVE);
    private final Animation clearDlgAnim = new DecelerateAnimation(200, 1, Direction.AxisDirection.NEGATIVE);
    private final Animation createConfirmAnim = new DecelerateAnimation(150, 1, Direction.AxisDirection.NEGATIVE);
    private final Animation createCancelAnim = new DecelerateAnimation(150, 1, Direction.AxisDirection.NEGATIVE);
    private final Animation clearConfirmAnim = new DecelerateAnimation(150, 1, Direction.AxisDirection.NEGATIVE);
    private final Animation clearCancelAnim = new DecelerateAnimation(150, 1, Direction.AxisDirection.NEGATIVE);

    private Animation[] accAnims, selAnims, delAnims;
    private float px, py, pw, ph, scrollOff, scrollTgt;
    private boolean searchFocused, showCreate, showClear;
    private final StringBuilder searchText = new StringBuilder(), createText = new StringBuilder();

    public AltManager(Screen parent) {
        super(Text.literal("Alt Manager"));
        this.parent = parent;
        for (int i = 0; i < actionAnims.length; i++) actionAnims[i] = new DecelerateAnimation(250, 1, Direction.AxisDirection.NEGATIVE);
        initAnims();
    }

    private void initAnims() {
        int n = Math.max(filteredAccounts.size(), 1);
        accAnims = new Animation[n]; selAnims = new Animation[n]; delAnims = new Animation[n];
        for (int i = 0; i < n; i++) {
            accAnims[i] = new DecelerateAnimation(200, 1, Direction.AxisDirection.NEGATIVE);
            selAnims[i] = new DecelerateAnimation(150, 1, Direction.AxisDirection.NEGATIVE);
            delAnims[i] = new DecelerateAnimation(150, 1, Direction.AxisDirection.NEGATIVE);
        }
    }

    private void filter() {
        String q = searchText.toString().toLowerCase();
        filteredAccounts.clear();
        for (String a : accounts) if (q.isEmpty() || a.toLowerCase().contains(q)) filteredAccounts.add(a);
        initAnims();
        clampScroll();
    }

    @Override protected void init() { panelAnim.reset(); for (Animation a : actionAnims) a.setDirection(Direction.AxisDirection.NEGATIVE); filter(); }

    private float padX() { return Math.max(30, pw * .05f); }
    private float padY() { return Math.max(25, ph * .06f); }
    private int al(float a, int b) { return (int)(b * a); }
    private int col(int v, float a) { return ColorUtil.rgba(v, v, v + 5, al(a, 255)); }
    private boolean in(double mx, double my, float x, float y, float w, float h) { return mx >= x && mx <= x + w && my >= y && my <= y + h; }
    private float lerp(float a, float b, float t) { return a + (b - a) * Math.min(1, t); }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        float a = (float) panelAnim.getOutput();
        scrollOff = lerp(scrollOff, scrollTgt, delta * 12);
        pw = width * .55f; ph = height * .75f; px = (width - pw) / 2; py = (height - ph) / 2;

        RenderUtil.drawTexture(ctx.getMatrices(), ResourceProvider.menubg, 0, 0, width, height, 0, -1);
        RenderUtil.drawBlur(ctx.getMatrices(), px, py, pw, ph, 20, 12 * a, ColorUtil.rgba(255, 255, 255, al(a, 255)));
        RenderUtil.drawRoundedRect(ctx.getMatrices(), px, py, pw, ph, 20, ColorUtil.rgba(18, 18, 22, al(a, 230)));
        RenderUtil.drawRoundedBorder(ctx.getMatrices(), px, py, pw, ph, 20, 1, ColorUtil.rgba(255, 255, 255, al(a, 15)));
        RenderUtil.drawRoundedRect(ctx.getMatrices(), px + 30, py + 1, pw - 60, 1, 0, ColorUtil.rgba(255, 255, 255, al(a, 30)));

        renderHeader(ctx, mx, my, a);
        renderSearch(ctx, mx, my, a);
        renderList(ctx, mx, my, a);
        renderActions(ctx, mx, my, a, delta);
        renderFooter(ctx, a);

        createDlgAnim.setDirection(showCreate ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
        clearDlgAnim.setDirection(showClear ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
        renderCreateDlg(ctx, mx, my);
        renderClearDlg(ctx, mx, my);
    }

    private void renderHeader(DrawContext ctx, int mx, int my, float a) {
        float x = px + padX(), y = py + padY(), bx = px + pw - 40, by = py + 18;
        boolean bh = in(mx, my, bx - 4, by - 4, 28, 28);
        backAnim.setDirection(bh ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
        float bp = (float) backAnim.getOutput();
        FontUtils.icomoon[16].centeredDraw(ctx.getMatrices(), IC_ARROW, bx + 10, by + 10 - FontUtils.icomoon[16].getHeight() / 2, ColorUtil.blendColorsInt(col(180, a), ColorUtil.rgba(255, 255, 255, al(a, 255)), bp));
        FontUtils.gilroy_bold[36].renderAnimatedGradientText(ctx.getMatrices(), "Alt Manager", x, y, ColorUtil.getColorStyle(0), ColorUtil.getColorStyle(180), (System.currentTimeMillis() % 3000L) / 3000f);
        FontUtils.sf_medium[14].drawLeftAligned(ctx.getMatrices(), "Manage your offline accounts", x, y + 32, col(180, a));
    }

    private void renderSearch(DrawContext ctx, int mx, int my, float a) {
        float x = px + padX(), y = py + padY() + 60, w = pw - padX() * 2, h = 36;
        RenderUtil.drawRoundedRect(ctx.getMatrices(), x, y, w, h, 10, ColorUtil.rgba(22, 22, 26, al(a, 240)));
        float iy = y + h / 2 - FontUtils.icomoon[14].getHeight() / 2;
        FontUtils.icomoon[14].centeredDraw(ctx.getMatrices(), IC_SEARCH, x + 16, iy, col(180, a));
        float ty = y + h / 2 - 6;
        if (searchText.isEmpty() && !searchFocused) FontUtils.sf_medium[14].drawLeftAligned(ctx.getMatrices(), "Search accounts...", x + 38, ty, col(180, a));
        else {
            String d = searchText.toString() + (searchFocused && System.currentTimeMillis() / 500 % 2 == 0 ? "|" : "");
            FontUtils.sf_medium[14].drawLeftAligned(ctx.getMatrices(), d, x + 38, ty, col(230, a));
        }
    }

    private void renderList(DrawContext ctx, int mx, int my, float a) {
        if (accAnims == null || accAnims.length < filteredAccounts.size()) initAnims();
        float x = px + padX(), y = py + padY() + 105, w = pw - padX() * 2, h = ph - 230;
        RenderUtil.drawRoundedRect(ctx.getMatrices(), x, y, w, h, 12, ColorUtil.rgba(22, 22, 26, al(a, 240)));

        Scissor.push();
        Scissor.setFromComponentCoordinates(x, y, w, h);
        float itemH = 48, startY = y + 8;
        for (int i = 0; i < filteredAccounts.size(); i++) {
            float iy = startY + i * itemH - scrollOff;
            if (iy + itemH < y || iy > y + h) continue;
            renderItem(ctx, mx, my, i, x + 8, iy, w - 16, itemH - 4, a);
        }
        Scissor.unset();
        Scissor.pop();

        if (filteredAccounts.isEmpty()) {
            String msg = accounts.isEmpty() ? "No accounts yet" : "No results found";
            FontUtils.sf_medium[14].centeredDraw(ctx.getMatrices(), msg, x + w / 2, y + h / 2 - 6, col(140, a));
        }
    }

    private void renderItem(DrawContext ctx, int mx, int my, int i, float x, float y, float w, float h, float a) {
        String nick = filteredAccounts.get(i);
        boolean sel = nick.equals(Manager.ACCOUNT_MANAGER.getLastSelectedAccount());
        boolean hov = in(mx, my, x, y, w, h);
        accAnims[i].setDirection(hov ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
        float hp = (float) accAnims[i].getOutput();

        int bgA = sel ? 50 : (int)(25 + 20 * hp);
        RenderUtil.drawRoundedRect(ctx.getMatrices(), x, y, w, h, 8, ColorUtil.rgba(35, 35, 42, al(a, bgA)));
        if (sel) RenderUtil.drawRoundedBorder(ctx.getMatrices(), x, y, w, h, 8, 1, ColorUtil.reAlphaInt(ColorUtil.getColorStyle(0), al(a, 100)));

        int ic = sel ? ColorUtil.reAlphaInt(ColorUtil.getColorStyle(90), al(a, 255)) : col(180, a);
        FontUtils.icomoon[16].centeredDraw(ctx.getMatrices(), IC_USER, x + 20, y + h / 2 - FontUtils.icomoon[16].getHeight() / 2, ic);
        FontUtils.sf_medium[14].drawLeftAligned(ctx.getMatrices(), nick, x + 44, y + h / 2 - FontUtils.sf_medium[14].getHeight() / 2, ColorUtil.blendColorsInt(col(200, a), ColorUtil.rgba(255, 255, 255, al(a, 255)), hp));

        float btnW = 60, btnH = 26, delX = x + w - btnW - 12, selX = delX - btnW - 8, btnY = y + (h - btnH) / 2;
        renderBtn(ctx, mx, my, "Select", selX, btnY, btnW, btnH, selAnims[i], a, false);
        renderBtn(ctx, mx, my, "Delete", delX, btnY, btnW, btnH, delAnims[i], a, true);
    }

    private void renderBtn(DrawContext ctx, int mx, int my, String text, float x, float y, float w, float h, Animation anim, float a, boolean danger) {
        boolean hov = in(mx, my, x, y, w, h);
        anim.setDirection(hov ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
        float p = (float) anim.getOutput();
        int bg = danger ? ColorUtil.blendColorsInt(ColorUtil.rgba(55, 30, 30, al(a, 255)), ColorUtil.rgba(95, 40, 40, al(a, 255)), p)
                : ColorUtil.blendColorsInt(ColorUtil.rgba(45, 45, 52, al(a, 255)), ColorUtil.rgba(65, 65, 72, al(a, 255)), p);
        RenderUtil.drawRoundedRect(ctx.getMatrices(), x, y, w, h, 6, bg);
        FontUtils.sf_medium[12].centeredDraw(ctx.getMatrices(), text, x + w / 2, y + h / 2 - FontUtils.sf_medium[12].getHeight() / 2, ColorUtil.blendColorsInt(col(210, a), ColorUtil.rgba(255, 255, 255, al(a, 255)), p));
    }

    private void renderActions(DrawContext ctx, int mx, int my, float a, float delta) {
        float y = py + ph - padY() - 58, btnH = 32, gap = 12;
        float[] ws = new float[ACTIONS.length];
        float tw = 0;
        for (int i = 0; i < ACTIONS.length; i++) { ws[i] = FontUtils.sf_medium[14].getWidth(ACTIONS[i]) + 48; tw += ws[i]; }
        tw += gap * (ACTIONS.length - 1);
        float sx = px + (pw - tw) / 2;

        for (int i = 0; i < ACTIONS.length; i++) {
            boolean hov = in(mx, my, sx, y, ws[i], btnH);
            actionAnims[i].setDirection(hov ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
            float p = (float) actionAnims[i].getOutput();
            if (p > .01f) RenderUtil.drawRoundedRect(ctx.getMatrices(), sx, y, ws[i], btnH, 8, ColorUtil.rgba(255, 255, 255, (int)(18 * p * a)));
            renderIcon(ctx, i, sx + 18, y + btnH / 2, p, a);
            FontUtils.sf_medium[14].drawLeftAligned(ctx.getMatrices(), ACTIONS[i], sx + 34 - 2 * p, y + btnH / 2 - FontUtils.sf_medium[14].getHeight() / 2, ColorUtil.blendColorsInt(col(180, a), ColorUtil.rgba(255, 255, 255, al(a, 255)), p));
            sx += ws[i] + gap;
        }
    }

    private void renderIcon(DrawContext ctx, int i, float cx, float cy, float p, float a) {
        if (p < .01f) return;
        int al = (int)(255 * p * a), c = ColorUtil.rgba(255, 255, 255, al);
        float h = FontUtils.icomoon[14].getHeight() / 2;
        switch (i) {
            case 0 -> { FontUtils.icomoon[14].centeredDraw(ctx.getMatrices(), IC_PLUS, cx, cy - h - 2 * p, c);
                if (p > .3f) FontUtils.icomoon[10].centeredDraw(ctx.getMatrices(), IC_USER, cx + 7, cy - FontUtils.icomoon[10].getHeight() / 2 + 2, ColorUtil.rgba(255, 255, 255, (int)(180 * (p - .3f) / .7f * a))); }
            case 1 -> { ctx.getMatrices().push(); ctx.getMatrices().translate(cx, cy, 0); ctx.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(p * 360)); FontUtils.icomoon[14].centeredDraw(ctx.getMatrices(), IC_SHUFFLE, 0, -h, c); ctx.getMatrices().pop(); }
            case 2 -> FontUtils.icomoon[14].centeredDraw(ctx.getMatrices(), IC_TRASH, cx + (float)Math.sin(System.currentTimeMillis() / 50.0) * 2.5f * p, cy - h, c);
        }
    }

    private void renderFooter(DrawContext ctx, float a) {
        float y = py + ph - padY() - 14;
        FontUtils.sf_medium[12].drawLeftAligned(ctx.getMatrices(), "Current: " + mc.getSession().getUsername(), px + padX(), y, col(160, a));
        FontUtils.sf_medium[12].drawRightAligned(ctx.getMatrices(), "Total: " + accounts.size(), px + pw - padX(), y, col(160, a));
    }

    private void renderCreateDlg(DrawContext ctx, int mx, int my) {
        float da = (float) createDlgAnim.getOutput();
        if (da < .01f) return;
        RenderUtil.drawRoundedRect(ctx.getMatrices(), 0, 0, width, height, 0, ColorUtil.rgba(0, 0, 0, (int)(140 * da)));
        float dW = 280, dH = 140, dX = (width - dW) / 2, dY = (height - dH) / 2;
        RenderUtil.drawBlur(ctx.getMatrices(), dX, dY, dW, dH, 14, 10 * da, ColorUtil.rgba(180, 180, 200, (int)(180 * da)));
        RenderUtil.drawRoundedRect(ctx.getMatrices(), dX, dY, dW, dH, 14, ColorUtil.rgba(20, 20, 24, (int)(250 * da)));
        RenderUtil.drawRoundedBorder(ctx.getMatrices(), dX, dY, dW, dH, 14, 1, ColorUtil.rgba(255, 255, 255, (int)(15 * da)));

        FontUtils.sf_medium[15].centeredDraw(ctx.getMatrices(), "Create Account", dX + dW / 2, dY + 24 - FontUtils.sf_medium[15].getHeight() / 2, ColorUtil.rgba(255, 255, 255, (int)(255 * da)));
        float ix = dX + 24, iy = dY + 48, iw = dW - 48, ih = 34;
        RenderUtil.drawRoundedRect(ctx.getMatrices(), ix, iy, iw, ih, 8, ColorUtil.rgba(15, 15, 18, (int)(255 * da)));
        float ty = iy + ih / 2 - FontUtils.sf_medium[13].getHeight() / 2;
        if (createText.isEmpty()) FontUtils.sf_medium[13].centeredDraw(ctx.getMatrices(), "Enter nickname", ix + iw / 2, ty, ColorUtil.rgba(140, 140, 145, (int)(255 * da)));
        else FontUtils.sf_medium[13].centeredDraw(ctx.getMatrices(), createText.toString() + (System.currentTimeMillis() / 500 % 2 == 0 ? "|" : ""), ix + iw / 2, ty, ColorUtil.rgba(230, 230, 235, (int)(255 * da)));

        float bw = 90, bh = 28, by = dY + dH - 42, cx = dX + dW / 2 - bw - 8, cax = dX + dW / 2 + 8;
        createConfirmAnim.setDirection(in(mx, my, cx, by, bw, bh) ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
        createCancelAnim.setDirection(in(mx, my, cax, by, bw, bh) ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
        float cp = (float) createConfirmAnim.getOutput(), cap = (float) createCancelAnim.getOutput();
        RenderUtil.drawRoundedRect(ctx.getMatrices(), cx, by, bw, bh, 6, ColorUtil.blendColorsInt(ColorUtil.reAlphaInt(ColorUtil.getColorStyle(0), (int)(200 * da)), ColorUtil.reAlphaInt(ColorUtil.getColorStyle(0), (int)(255 * da)), cp));
        FontUtils.sf_medium[13].centeredDraw(ctx.getMatrices(), "Create", cx + bw / 2, by + bh / 2 - FontUtils.sf_medium[13].getHeight() / 2, ColorUtil.rgba(255, 255, 255, (int)(255 * da)));
        RenderUtil.drawRoundedRect(ctx.getMatrices(), cax, by, bw, bh, 6, ColorUtil.blendColorsInt(ColorUtil.rgba(45, 45, 52, (int)(255 * da)), ColorUtil.rgba(60, 60, 68, (int)(255 * da)), cap));
        FontUtils.sf_medium[13].centeredDraw(ctx.getMatrices(), "Cancel", cax + bw / 2, by + bh / 2 - FontUtils.sf_medium[13].getHeight() / 2, ColorUtil.rgba(255, 255, 255, (int)(255 * da)));
    }

    private void renderClearDlg(DrawContext ctx, int mx, int my) {
        float da = (float) clearDlgAnim.getOutput();
        if (da < .01f) return;
        RenderUtil.drawRoundedRect(ctx.getMatrices(), 0, 0, width, height, 0, ColorUtil.rgba(0, 0, 0, (int)(140 * da)));
        float dW = 280, dH = 120, dX = (width - dW) / 2, dY = (height - dH) / 2;
        RenderUtil.drawBlur(ctx.getMatrices(), dX, dY, dW, dH, 14, 10 * da, ColorUtil.rgba(180, 180, 200, (int)(180 * da)));
        RenderUtil.drawRoundedRect(ctx.getMatrices(), dX, dY, dW, dH, 14, ColorUtil.rgba(20, 20, 24, (int)(250 * da)));
        RenderUtil.drawRoundedBorder(ctx.getMatrices(), dX, dY, dW, dH, 14, 1, ColorUtil.rgba(255, 255, 255, (int)(15 * da)));

        FontUtils.sf_medium[15].centeredDraw(ctx.getMatrices(), "Clear all accounts?", dX + dW / 2, dY + 26 - FontUtils.sf_medium[15].getHeight() / 2, ColorUtil.rgba(255, 255, 255, (int)(255 * da)));
        FontUtils.sf_medium[11].centeredDraw(ctx.getMatrices(), "This cannot be undone", dX + dW / 2, dY + 46 - FontUtils.sf_medium[11].getHeight() / 2, ColorUtil.rgba(140, 140, 145, (int)(255 * da)));

        float bw = 90, bh = 28, by = dY + dH - 40, cx = dX + dW / 2 - bw - 8, cax = dX + dW / 2 + 8;
        clearConfirmAnim.setDirection(in(mx, my, cx, by, bw, bh) ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
        clearCancelAnim.setDirection(in(mx, my, cax, by, bw, bh) ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
        float cp = (float) clearConfirmAnim.getOutput(), cap = (float) clearCancelAnim.getOutput();
        RenderUtil.drawRoundedRect(ctx.getMatrices(), cx, by, bw, bh, 6, ColorUtil.blendColorsInt(ColorUtil.rgba(160, 50, 50, (int)(255 * da)), ColorUtil.rgba(190, 60, 60, (int)(255 * da)), cp));
        FontUtils.sf_medium[13].centeredDraw(ctx.getMatrices(), "Confirm", cx + bw / 2, by + bh / 2 - FontUtils.sf_medium[13].getHeight() / 2, ColorUtil.rgba(255, 255, 255, (int)(255 * da)));
        RenderUtil.drawRoundedRect(ctx.getMatrices(), cax, by, bw, bh, 6, ColorUtil.blendColorsInt(ColorUtil.rgba(45, 45, 52, (int)(255 * da)), ColorUtil.rgba(60, 60, 68, (int)(255 * da)), cap));
        FontUtils.sf_medium[13].centeredDraw(ctx.getMatrices(), "Cancel", cax + bw / 2, by + bh / 2 - FontUtils.sf_medium[13].getHeight() / 2, ColorUtil.rgba(255, 255, 255, (int)(255 * da)));
    }

    private void clampScroll() { scrollTgt = Math.max(0, Math.min(scrollTgt, Math.max(0, filteredAccounts.size() * 48 - (ph - 230) + 16))); }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX, my = (int) mouseY;
        if (showCreate) return handleCreateClick(mx, my);
        if (showClear) return handleClearClick(mx, my);
        if (in(mx, my, px + pw - 44, py + 14, 28, 28)) { close(); return true; }
        float sx = px + padX(), sy = py + padY() + 60, sw = pw - padX() * 2;
        if (in(mx, my, sx, sy, sw, 36)) { searchFocused = true; return true; } else searchFocused = false;
        if (handleListClick(mx, my)) return true;
        if (handleActionClick(mx, my)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleCreateClick(int mx, int my) {
        float dW = 280, dH = 140, dX = (width - dW) / 2, dY = (height - dH) / 2, bw = 90, bh = 28, by = dY + dH - 42;
        if (in(mx, my, dX + dW / 2 - bw - 8, by, bw, bh)) { createAccount(); return true; }
        if (in(mx, my, dX + dW / 2 + 8, by, bw, bh)) { showCreate = false; createText.setLength(0); return true; }
        return true;
    }

    private boolean handleClearClick(int mx, int my) {
        float dW = 280, dH = 120, dX = (width - dW) / 2, dY = (height - dH) / 2, bw = 90, bh = 28, by = dY + dH - 40;
        if (in(mx, my, dX + dW / 2 - bw - 8, by, bw, bh)) { accounts.clear(); Manager.ACCOUNT_MANAGER.clearAll(); filter(); showClear = false; return true; }
        if (in(mx, my, dX + dW / 2 + 8, by, bw, bh)) { showClear = false; return true; }
        return true;
    }

    private boolean handleListClick(int mx, int my) {
        float x = px + padX(), y = py + padY() + 105, w = pw - padX() * 2, h = ph - 230;
        if (!in(mx, my, x, y, w, h)) return false;
        float itemH = 48, startY = y + 8;
        for (int i = 0; i < filteredAccounts.size(); i++) {
            float iy = startY + i * itemH - scrollOff, ix = x + 8, iw = w - 16;
            if (iy + itemH < y || iy > y + h) continue;
            float btnW = 60, btnH = 26, delX = ix + iw - btnW - 12, selX = delX - btnW - 8, btnY = iy + (itemH - 4 - btnH) / 2;
            if (in(mx, my, delX, btnY, btnW, btnH)) { deleteAccount(i); return true; }
            if (in(mx, my, selX, btnY, btnW, btnH) || in(mx, my, ix, iy, iw, itemH - 4)) { selectAccount(i); return true; }
        }
        return false;
    }

    private boolean handleActionClick(int mx, int my) {
        float y = py + ph - padY() - 58, btnH = 32, gap = 12;
        float[] ws = new float[ACTIONS.length];
        float tw = 0;
        for (int i = 0; i < ACTIONS.length; i++) { ws[i] = FontUtils.sf_medium[14].getWidth(ACTIONS[i]) + 48; tw += ws[i]; }
        tw += gap * (ACTIONS.length - 1);
        float sx = px + (pw - tw) / 2;
        for (int i = 0; i < ACTIONS.length; i++) {
            if (in(mx, my, sx, y, ws[i], btnH)) {
                switch (i) {
                    case 0 -> { showCreate = true; createText.setLength(0); }
                    case 1 -> createRandom();
                    case 2 -> showClear = true;
                }
                return true;
            }
            sx += ws[i] + gap;
        }
        return false;
    }

    private void selectAccount(int i) { String n = filteredAccounts.get(i); ClientManager.loginAccount(n); Manager.ACCOUNT_MANAGER.setLastSelectedAccount(n); }
    private void deleteAccount(int i) { String n = filteredAccounts.get(i); Manager.ACCOUNT_MANAGER.removeAccount(n); accounts.remove(n); filter(); clampScroll(); }

    private void createAccount() {
        String n = createText.toString().trim();
        if (!n.isEmpty() && accounts.stream().noneMatch(a -> a.equalsIgnoreCase(n))) { accounts.add(n); Manager.ACCOUNT_MANAGER.addAccount(n); filter(); }
        createText.setLength(0);
        showCreate = false;
    }

    private void createRandom() {
        String n = Main.getInstance().name.toLowerCase().replaceAll("\\s+", "") + "_" + (int)(Math.random() * 10000000);
        if (n.length() > 16) n = n.substring(0, 16);
        if (!accounts.contains(n)) { accounts.add(n); Manager.ACCOUNT_MANAGER.addAccount(n); filter(); }
        ClientManager.loginAccount(n);
        Manager.ACCOUNT_MANAGER.setLastSelectedAccount(n);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        float ly = py + padY() + 105, lh = ph - 230;
        if (mouseY >= ly && mouseY <= ly + lh) { scrollTgt -= scrollY * 35; clampScroll(); return true; }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (showCreate) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { showCreate = false; createText.setLength(0); return true; }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) { createAccount(); return true; }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !createText.isEmpty()) { createText.deleteCharAt(createText.length() - 1); return true; }
            if (ctrl() && keyCode == GLFW.GLFW_KEY_V) { paste(createText); return true; }
            return true;
        }
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { searchFocused = false; return true; }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !searchText.isEmpty()) { searchText.deleteCharAt(searchText.length() - 1); filter(); return true; }
            if (ctrl() && keyCode == GLFW.GLFW_KEY_V) { paste(searchText); filter(); return true; }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) { if (showClear) showClear = false; else close(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean ctrl() { return GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS || GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS; }

    private void paste(StringBuilder t) {
        String c = GLFW.glfwGetClipboardString(mc.getWindow().getHandle());
        if (c != null) { String f = c.replaceAll("[^\\w]", ""); int s = 16 - t.length(); if (s > 0) t.append(f.substring(0, Math.min(f.length(), s))); }
    }

    @Override public boolean charTyped(char chr, int modifiers) {
        if (showCreate) { if (createText.length() < 16 && (Character.isLetterOrDigit(chr) || chr == '_')) createText.append(chr); return true; }
        if (searchFocused) { if (searchText.length() < 32 && (Character.isLetterOrDigit(chr) || chr == '_')) { searchText.append(chr); filter(); } return true; }
        return super.charTyped(chr, modifiers);
    }

    @Override public void close() { if (parent != null) mc.setScreen(parent); else super.close(); }
    @Override public boolean shouldCloseOnEsc() { return false; }
}