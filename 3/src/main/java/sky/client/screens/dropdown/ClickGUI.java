package sky.client.screens.dropdown;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.joml.Vector4i;
import org.lwjgl.glfw.GLFW;
import sky.client.manager.ClientManager;
import sky.client.manager.IMinecraft;
import sky.client.manager.Manager;
import sky.client.manager.themeManager.Style;
import sky.client.modules.Function;
import sky.client.modules.Type;
import sky.client.modules.setting.*;
import sky.client.screens.dropdown.impl.*;
import sky.client.screens.dropdown.search.SearchState;
import sky.client.util.animations.impl.EaseInOutQuad;
import sky.client.util.color.ColorUtil;
import sky.client.manager.fontManager.FontUtils;
import sky.client.util.math.MathUtil;
import sky.client.util.render.RenderUtil;
import sky.client.util.render.Scissor;
import sky.client.util.render.providers.ResourceProvider;

import java.awt.*;
import java.util.*;

import static sky.client.util.render.RenderUtil.drawBlur;

public class ClickGUI extends Screen implements IMinecraft {
    private static final int PW = 125, PH = 280, PM = 8, TMT = 5, TH = 20, FH = 20;
    private static final int SAY = TMT + TH, SAH = PH - SAY - 5;
    private static final int SH = 20, SMB = 10, SMW = 60, THH = 16, TMB = 40, TMW = 180, VT = 11;
    private static final float TAS = 0.2f, SS = 12f, SLF = 20f, SSF = 12f, TSS = 15f, TSLF = 15f;

    private final Color GUI_COL = Manager.FUNCTION_MANAGER.clickGUI.getGuiColor();
    private final Set<Type> cats = EnumSet.of(Type.Combat, Type.Move, Type.Render, Type.Player, Type.Misc);
    private static final Map<Type, Float> scrollOff = new HashMap<>(), scrollTgt = new HashMap<>();
    private final Map<Function, Float> arrowProg = new HashMap<>(), expProg = new HashMap<>();
    private final EaseInOutQuad openAnim = new EaseInOutQuad(180, 1);
    private final EaseInOutQuad fadeAnim = new EaseInOutQuad(300, 1);
    private double anim, fade;
    private boolean closing;

    private final SearchState search;
    private boolean binding;
    private Function bindFunc;

    private SliderSetting dragSlider;
    private int dragX, dragW;

    private final BooleanSettingRenderer boolR = new BooleanSettingRenderer();
    private final BindBooleanSettingRenderer bindBoolR = new BindBooleanSettingRenderer();
    private final BindSettingRenderer bindR = new BindSettingRenderer();
    private final ModeSettingRenderer modeR = new ModeSettingRenderer();
    private final MultiSettingRenderer multiR = new MultiSettingRenderer();
    private final SliderSettingRenderer sliderR = new SliderSettingRenderer();
    private final TextSettingRenderer textR = new TextSettingRenderer();

    private static float themeOff, themeTgt;
    private float menuAnim, menuTgt, alphaAnim, nameAnim;
    private static boolean themeMenu;

    private static boolean pickerOpen;
    private static int selCol1 = -1, selCol2 = -1;
    private float p1x = 0.5f, p1y = 0.5f, p2x = 0.5f, p2y = 0.5f, pickerAnim;
    private boolean dragP1, dragP2;

    public ClickGUI() {
        super(Text.literal("ClickGUI"));
        search = new SearchState();
        if (scrollOff.isEmpty()) cats.forEach(c -> { scrollOff.put(c, 0f); scrollTgt.put(c, 0f); });
    }

    @Override
    public void init() {
        closing = false;
        openAnim.setDirection(Direction.AxisDirection.POSITIVE);
        openAnim.reset();
        fadeAnim.setDirection(Direction.AxisDirection.POSITIVE);
        fadeAnim.reset();
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        anim = openAnim.getOutput();
        fade = fadeAnim.getOutput();
        if (closing && openAnim.finished(Direction.AxisDirection.NEGATIVE)) { super.close(); return; }
        if (anim <= 0.01) return;

        super.render(ctx, mx, my, delta);
        ctx.getMatrices().push();

        for (Type c : cats) scrollOff.put(c, MathUtil.lerp(scrollOff.get(c), scrollTgt.get(c), SLF));

        int tw = cats.size() * (PW + PM) - PM, sx = (width - tw) / 2, sy = (height - PH) / 2;

        int i = 0;
        for (Type c : cats) panel(ctx, sx + i++ * (PW + PM), sy, c, mx, my, (float)anim);

        Function hov = getHovered(mx, my, sx, sy);
        if (hov != null && hov.desc != null && !hov.desc.isEmpty()) drawDesc(ctx, hov.desc, sy, (float)fade);
        DescriptionRenderQueue.renderAll(ctx);

        renderSearch(ctx, (float)fade);
        renderThemeBtn(ctx, mx, my, (float)fade);
        renderTheme(ctx, mx, my, (float)fade);
        expiry(ctx, (float)fade);
        ctx.getMatrices().pop();
    }

    private Function getHovered(int mx, int my, int sx, int sy) {
        int i = 0;
        for (Type c : cats) {
            int px = sx + i++ * (PW + PM);
            if (!inRect(mx, my, px, sy, PW, PH)) continue;

            float off = scrollOff.get(c), cy = sy + SAY - off;
            for (Function f : Manager.FUNCTION_MANAGER.getFunctions(c)) {
                if (!visible(f)) continue;
                float prog = expProg.getOrDefault(f, f.expanded ? 1f : 0f);
                int th = FH + (int)(settingsH(f) * prog);

                if (inRect(mx, my, px, (int)cy, PW, FH) && my >= sy + SAY && my <= sy + SAY + SAH) return f;
                cy += th;
            }
        }
        return null;
    }

    private void drawDesc(DrawContext ctx, String desc, int sy, float fade) {
        if (fade < 0.01f) return;
        int dw = (int) FontUtils.durman[19].getWidth(desc), dh = 20;
        int dx = (width - dw) / 2, dy = sy - dh - 10;
        var gui = Manager.FUNCTION_MANAGER.clickGUI;
        if (gui.blur.get() && gui.blurSetting.get("Описание")) drawBlur(ctx.getMatrices(), dx - 6, dy - 3.5f, dw + 12, dh, 12, 8 * fade, -1);
        RenderUtil.drawRoundedRect(ctx.getMatrices(), dx - 6, dy - 3.5f, dw + 12, dh, 6, ColorUtil.applyAlpha(GUI_COL.getRGB(), fade));
        FontUtils.durman[19].drawLeftAligned(ctx.getMatrices(), desc, dx, dy, ColorUtil.applyAlpha(-1, fade));
    }

    private float updateExp(Function f) {
        float tgt = f.expanded ? 1f : 0f, prog = expProg.getOrDefault(f, tgt);
        prog = MathUtil.lerp(prog, tgt, 15f);
        if (Math.abs(tgt - prog) < 0.001f) prog = tgt;
        expProg.put(f, prog);
        return prog;
    }

    private void panel(DrawContext ctx, int x, int y, Type cat, int mx, int my, float fade) {
        if (fade < 0.01f) return;
        var gui = Manager.FUNCTION_MANAGER.clickGUI;

        if (gui.blur.get() && gui.blurSetting.get("Панели")) drawBlur(ctx.getMatrices(), x, y, PW, PH, 12, 8 * fade, -1);
        RenderUtil.drawRoundedRect(ctx.getMatrices(), x, y, PW, PH, 12, ColorUtil.applyAlpha(GUI_COL.getRGB(), fade));
        FontUtils.sf_bold[20].drawLeftAligned(ctx.getMatrices(), cat.name(), x + (PW - (int)FontUtils.sf_bold[20].getWidth(cat.name())) / 2, y + TMT + 2, ColorUtil.applyAlpha(-1, fade));
        FontUtils.icomoon[20].drawLeftAligned(ctx.getMatrices(), cat.icon, x + (PW - (int)FontUtils.sf_bold[20].getWidth(cat.icon)) / 2 - 50, y + TMT + 2, ColorUtil.applyAlpha(-1, fade));

        float contentFade = Math.max(0, (fade - 0.2f) / 0.8f);
        if (contentFade < 0.01f) return;

        clampScroll(cat);
        float off = scrollOff.compute(cat, (k, v) -> MathUtil.lerp(v, MathUtil.lerp(v, scrollTgt.get(k), SLF), SSF));

        ctx.getMatrices().push();
        Scissor.push();
        Scissor.setFromComponentCoordinates(x, y + SAY, PW, SAH);

        float curY = y + SAY - off;
        for (Function f : Manager.FUNCTION_MANAGER.getFunctions(cat)) {
            if (!visible(f)) continue;
            float prog = updateExp(f);
            int setH = (int)(settingsH(f) * prog), th = FH + setH;

            if (curY + th < y + SAY || curY > y + PH) { curY += th; continue; }

            int baseAlpha = (int)(gui.alphaModules.get().intValue() * contentFade);
            int c1 = ColorUtil.applyAlpha(f.state ? ColorUtil.getColorStyle(30) : 0xFFC6C6C6, contentFade);
            int c2 = ColorUtil.applyAlpha(f.state ? ColorUtil.getColorStyle(120) : 0xFFC6C6C6, contentFade);
            int cm = f.state ? ColorUtil.getColorStyle(30, baseAlpha) : ColorUtil.rgba(198, 198, 198, baseAlpha);

            if (gui.filling.get()) RenderUtil.rectRGB(ctx.getMatrices(), x + 4, curY - 1, PW - 8, th - 1, gui.rounding.get().intValue(), cm, cm, cm, cm);
            if (gui.strike.get()) RenderUtil.drawRoundedBorder(ctx.getMatrices(), x + 4, curY - 1, PW - 8, th - 1, gui.rounding.get().intValue(), 0f, cm);

            String txt = binding && bindFunc == f ? (f.getBindCode() == 0 ? "Binding..." : "Binding... [" + ClientManager.getKey(f.getBindCode()) + "]") : f.name;
            FontUtils.sf_medium[16].renderGradientText(ctx.getMatrices(), txt, x + 10, curY + 3, c1, c2);

            if (setH > 0) {
                float sty = curY + FH;
                ctx.getMatrices().push();
                Scissor.push();
                Scissor.setFromComponentCoordinates(x + 1, (int)sty, PW - 2, setH);

                for (Setting s : f.getSettings()) {
                    if (!s.isVisible()) continue;
                    int h = settingH(s, PW - 20);
                    if (h <= 0) continue;
                    renderSetting(ctx, s, x + 10, (int)sty, PW - 20, h);
                    sty += h + 1;
                }

                Scissor.pop();
                ctx.getMatrices().pop();
            }

            if (!f.getSettings().isEmpty()) {
                float ap = arrowProg.getOrDefault(f, f.expanded ? 1f : 0f);
                ap = MathUtil.lerp(ap, f.expanded ? 1f : 0f, 15f);
                if (Math.abs((f.expanded ? 1f : 0f) - ap) < 0.001f) ap = f.expanded ? 1f : 0f;
                arrowProg.put(f, ap);

                ctx.getMatrices().push();
                ctx.getMatrices().translate(x + PW - 15, curY + FH / 2 - 2, 0);
                ctx.getMatrices().multiply(new Quaternionf().fromAxisAngleRad(new Vector3f(0, 0, 1), (float)Math.toRadians(90f * ap)));
                FontUtils.sf_medium[16].drawLeftAligned(ctx.getMatrices(), "→", -4, -FontUtils.sf_medium[16].getHeight() / 2, c1);
                ctx.getMatrices().pop();
            }
            curY += th;
        }

        clampScroll(cat);
        Scissor.pop();
        ctx.getMatrices().pop();
    }

    private void renderSetting(DrawContext ctx, Setting s, int x, int y, int w, int h) {
        if (s instanceof BooleanSetting b) boolR.render(ctx, b, x, y, w, h);
        else if (s instanceof BindBooleanSetting b) bindBoolR.render(ctx, b, x, y, w, h);
        else if (s instanceof BindSetting b) bindR.render(ctx, b, x, y - 2, w, h);
        else if (s instanceof ModeSetting m) modeR.render(ctx, m, x, y, w, h);
        else if (s instanceof MultiSetting m) multiR.render(ctx, m, x, y, w, h);
        else if (s instanceof SliderSetting sl) sliderR.render(ctx, sl, x, y - 2, w, h);
        else if (s instanceof TextSetting t) textR.render(ctx, t, x, y, w, h);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        int tw = TMW, tx = (width - tw) / 2, ty = (height + PH) / 2 + TMB;
        if (inRect(mx, my, tx, ty, tw, THH)) { themeTgt -= (float)sy * TSS; return true; }

        int ttw = cats.size() * (PW + PM) - PM, stx = (width - ttw) / 2, sty = (height - PH) / 2;
        int i = 0;
        for (Type c : cats) {
            int px = stx + i++ * (PW + PM);
            if (inRect(mx, my, px, sty + SAY, PW, SAH)) {
                int max = maxScroll(c);
                if (max > 0) { scrollTgt.compute(c, (k, v) -> Math.max(0, Math.min(v - (float)sy * SS, max))); return true; }
                return false;
            }
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    private int maxScroll(Type cat) {
        int h = 0;
        for (Function f : Manager.FUNCTION_MANAGER.getFunctions(cat)) {
            if (!visible(f)) continue;
            h += FH + (int)(settingsH(f) * expProg.getOrDefault(f, f.expanded ? 1f : 0f));
        }
        return Math.max(0, h - SAH);
    }

    @Override
    public boolean charTyped(char c, int code) {
        if (search.focused) {
            String prev = search.text;
            if (search.text.length() < 30) {
                search.text = search.text.substring(0, search.cursorPosition) + c + search.text.substring(search.cursorPosition);
                search.cursorPosition++;
            }
            if (!prev.equals(search.text)) resetScroll();
            return true;
        }
        for (Type cat : cats) for (Function f : Manager.FUNCTION_MANAGER.getFunctions(cat)) if (f.expanded) for (Setting s : f.getSettings())
            if (s instanceof TextSetting t && t.isFocused() && textR.charTyped(t, c, code)) return true;
        return super.charTyped(c, code);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mod) {
        if (closing) return true;

        for (Type cat : cats) for (Function f : Manager.FUNCTION_MANAGER.getFunctions(cat)) if (visible(f) && f.expanded) for (Setting s : f.getSettings()) {
            if (!s.isVisible()) continue;
            if (s instanceof BindBooleanSetting b && b.isListeningForBind()) { b.setKey(key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_DELETE ? 0 : key); b.setListeningForBind(false); return true; }
            if (s instanceof BindSetting b && b.isBinding()) { b.setKey(key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_DELETE ? -1 : key); b.setBinding(false); return true; }
            if (s instanceof TextSetting t && t.isFocused() && textR.keyPressed(t, key, scan, mod)) return true;
        }

        if (binding && bindFunc != null) {
            bindFunc.setBindCode(key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_DELETE ? 0 : key);
            binding = false; bindFunc = null; return true;
        }

        if (search.focused) {
            String prev = search.text;
            switch (key) {
                case GLFW.GLFW_KEY_BACKSPACE -> { if (search.cursorPosition > 0) { search.text = search.text.substring(0, search.cursorPosition - 1) + search.text.substring(search.cursorPosition); search.cursorPosition--; } if (!prev.equals(search.text)) resetScroll(); return true; }
                case GLFW.GLFW_KEY_DELETE -> { if (search.cursorPosition < search.text.length()) search.text = search.text.substring(0, search.cursorPosition) + search.text.substring(search.cursorPosition + 1); if (!prev.equals(search.text)) resetScroll(); return true; }
                case GLFW.GLFW_KEY_LEFT -> { if (search.cursorPosition > 0) search.cursorPosition--; return true; }
                case GLFW.GLFW_KEY_RIGHT -> { if (search.cursorPosition < search.text.length()) search.cursorPosition++; return true; }
                case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_ESCAPE -> { search.focused = false; return true; }
            }
        }
        return super.keyPressed(key, scan, mod);
    }

    private void resetScroll() { for (Type c : cats) { scrollTgt.put(c, 0f); scrollOff.put(c, 0f); } }

    @Override
    public void tick() {
        super.tick();
        if (System.currentTimeMillis() - search.lastCursorBlink >= 500) { search.cursorVisible = !search.cursorVisible; search.lastCursorBlink = System.currentTimeMillis(); }
    }

    @Override
    public void close() {
        if (closing) return;
        dragP1 = dragP2 = false; closing = true;
        openAnim.setDirection(Direction.AxisDirection.NEGATIVE);
        openAnim.reset();
        fadeAnim.setDirection(Direction.AxisDirection.NEGATIVE);
        fadeAnim.reset();
    }

    private int searchX() { return (width - SMW) / 2; }
    private int searchY() { return (height + PH) / 2 + SMB; }

    private void renderSearch(DrawContext ctx, float fade) {
        if (fade < 0.01f) return;
        int sx = searchX(), sy = searchY();
        var gui = Manager.FUNCTION_MANAGER.clickGUI;
        if (gui.blur.get() && gui.blurSetting.get("Поиск")) drawBlur(ctx.getMatrices(), sx, sy, SMW, SH, 6, 12 * fade, -1);
        RenderUtil.drawRoundedRect(ctx.getMatrices(), sx, sy, SMW, SH, 6, ColorUtil.applyAlpha(GUI_COL.getRGB(), fade));

        String txt; int col, tx;
        if (search.text.isEmpty() && !search.focused) { txt = "Поиск..."; col = ColorUtil.applyAlpha(0x78FFFFFF, fade); tx = sx + (SMW - (int)FontUtils.sf_medium[18].getWidth(txt)) / 2; }
        else { txt = search.text + (search.focused && search.cursorVisible ? "|" : ""); col = ColorUtil.applyAlpha(-1, fade); tx = sx + 6; }
        FontUtils.sf_medium[18].drawLeftAligned(ctx.getMatrices(), txt, tx, (int)(sy + (SH - FontUtils.sf_medium[18].getHeight()) / 2), col);
    }

    private void renderThemeBtn(DrawContext ctx, double mx, double my, float fade) {
        if (fade < 0.01f) return;
        int bx = searchX() + 65, by = searchY() + 2;
        boolean hov = inRect(mx, my, bx, by, 16, 16);
        RenderUtil.drawRoundedRect(ctx.getMatrices(), bx, by, 16, 16, 2, ColorUtil.applyAlpha(hov ? GUI_COL.brighter().getRGB() : GUI_COL.getRGB(), fade));
        RenderUtil.drawTexture(ctx.getMatrices(), "images/gui/colors2.png", bx + 3, by + 2.5f, 10, 10, 0, ColorUtil.applyAlpha(-1, fade));
    }

    private void renderTheme(DrawContext ctx, int mx, int my, float baseFade) {
        if (baseFade < 0.01f) return;
        alphaAnim += ((menuTgt > 0.01f ? 1f : 0f) - alphaAnim) * 0.15f;
        if (alphaAnim < 0.01f) return;
        menuAnim += (menuTgt - menuAnim) * TAS;
        if (menuAnim < 0.01f) return;

        float fade = alphaAnim * baseFade;

        int tw = TMW, tx = (width - tw) / 2, ty = (height + PH) / 2 + TMB;
        float oy = (1f - menuAnim) * 10f;
        themeOff = MathUtil.lerp(themeOff, themeTgt, TSLF);

        int pc = ColorUtil.applyAlpha(GUI_COL.getRGB(), fade);
        var gui = Manager.FUNCTION_MANAGER.clickGUI;
        if (gui.blur.get() && gui.blurSetting.get("Темы")) drawBlur(ctx.getMatrices(), tx, ty + oy, tw, THH, 3, 12 * fade, -1);
        RenderUtil.drawRoundedRect(ctx.getMatrices(), tx, ty + oy, tw, THH, 3, pc);

        int cs = THH - 5, pad = 5, total = Manager.STYLE_MANAGER.getStyles().size() + 1;
        float maxS = Math.max(0, (total - VT) * (cs + pad));
        themeTgt = MathHelper.clamp(themeTgt, 0, maxS);
        themeOff = MathHelper.clamp(themeOff, 0, maxS);

        if (total > VT) {
            int ac = ColorUtil.applyAlpha(-1, fade * 0.6f);
            if (themeTgt > 0) FontUtils.sf_medium[16].drawLeftAligned(ctx.getMatrices(), "←", tx - 10, ty + THH / 2 - 5, ac);
            if (themeTgt < maxS) FontUtils.sf_medium[16].drawLeftAligned(ctx.getMatrices(), "→", tx + tw + 4, ty + THH / 2 - 5, ac);
        }

        ctx.getMatrices().push();
        Scissor.push();
        Scissor.setFromComponentCoordinates(tx + 1, ty + oy, tw - 2, THH);

        float stx = tx + pad - themeOff;
        int cty = (int)(ty + (THH - cs) / 2 + 0.9f + oy);
        String hov = null;

        if (selCol1 != -1 && selCol2 != -1) {
            Vector4i v = new Vector4i(ColorUtil.gradient(5, 0, selCol1, selCol2), ColorUtil.gradient(5, 180, selCol1, selCol2), ColorUtil.gradient(5, 90, selCol1, selCol2), ColorUtil.gradient(5, 360, selCol1, selCol2));
            RenderUtil.rectRGB(ctx.getMatrices(), stx, cty + 0.5f, cs, cs, 5, ColorUtil.applyAlpha(v.w, fade), ColorUtil.applyAlpha(v.x, fade), ColorUtil.applyAlpha(v.y, fade), ColorUtil.applyAlpha(v.z, fade));
        } else {
            RenderUtil.drawTexture(ctx.getMatrices(), "images/gui/pips.png", stx, cty + 0.5f, cs, cs, 5, ColorUtil.applyAlpha(-1, fade));
        }
        if (inRect(mx, my, (int)stx, cty, cs, cs)) { RenderUtil.drawRoundedBorder(ctx.getMatrices(), stx - 1, cty - 0.5f, cs + 2, cs + 2, 5, 0.1f, ColorUtil.applyAlpha(-1, fade)); hov = "ЛКМ - Создать свою тему"; }
        stx += cs + pad;

        for (Style s : Manager.STYLE_MANAGER.getStyles()) {
            int c1 = s.colors[0], c2 = s.colors.length > 1 ? s.colors[1] : c1;
            Vector4i v = new Vector4i(ColorUtil.gradient(5, 0, c1, c2), ColorUtil.gradient(5, 180, c1, c2), ColorUtil.gradient(5, 90, c1, c2), ColorUtil.gradient(5, 360, c1, c2));
            RenderUtil.rectRGB(ctx.getMatrices(), stx, cty + 0.5f, cs, cs, 5, ColorUtil.applyAlpha(v.w, fade), ColorUtil.applyAlpha(v.x, fade), ColorUtil.applyAlpha(v.y, fade), ColorUtil.applyAlpha(v.z, fade));
            if (inRect(mx, my, (int)stx, cty, cs, cs)) hov = s.name;
            stx += cs + pad;
        }

        Scissor.pop();
        ctx.getMatrices().pop();

        pickerAnim += ((pickerOpen ? 1f : 0f) - pickerAnim) * 0.2f;
        if (pickerAnim > 0.01f) pickers(ctx, tx + pad, cty, mx, my, fade);

        nameAnim += ((hov != null ? 1f : 0f) - nameAnim) * 0.2f;
        if (nameAnim > 0.01f && hov != null) {
            int tc = ColorUtil.applyAlpha(-1, nameAnim * fade);
            FontUtils.sf_medium[18].drawLeftAligned(ctx.getMatrices(), hov, (width - (int)FontUtils.sf_medium[18].getWidth(hov)) / 2, cty + 18, tc);
            if (hov.toLowerCase().contains("custom")) FontUtils.sf_medium[14].drawLeftAligned(ctx.getMatrices(), "ПКМ — удалить", (width - (int)FontUtils.sf_medium[14].getWidth("ПКМ — удалить")) / 2, cty + 32, ColorUtil.applyAlpha(-1, nameAnim * fade * 0.7f));
        }
    }

    private void pickers(DrawContext ctx, int x, int y, int mx, int my, float baseFade) {
        float fade = pickerAnim * baseFade;
        int pw = 85, ph = 51;
        float ox = (1f - pickerAnim) * 30f, sc = 0.95f + 0.05f * pickerAnim;
        int px = (int)(x - pw - 20 + ox), py = y - ph / 2 - 12;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(px + pw / 2f, py + ph / 2f, 0);
        ctx.getMatrices().scale(sc, sc, 1f);
        ctx.getMatrices().translate(-pw / 2f, -ph / 2f, 0);

        var gui = Manager.FUNCTION_MANAGER.clickGUI;
        if (gui.blur.get() && gui.blurSetting.get("Создание темы")) RenderUtil.drawBlur(ctx.getMatrices(), 0, 0, pw, ph, 4, 12 * fade, -1);
        RenderUtil.drawRoundedRect(ctx.getMatrices(), 0, 0, pw, ph, 4, ColorUtil.applyAlpha(GUI_COL.getRGB(), fade));

        int ps = 30;
        RenderUtil.drawTexture(ctx.getMatrices(), "images/gui/pick.png", 5, 5, ps, ps, 14, ColorUtil.applyAlpha(-1, fade));
        RenderUtil.drawRoundedBorder(ctx.getMatrices(), 5, 5, ps, ps, 14, 0.1f, ColorUtil.applyAlpha(-1, fade));
        RenderUtil.drawCircle(ctx.getMatrices(), (int)(5 + p1x * ps), (int)(5 + p1y * ps), 4f, ColorUtil.applyAlpha(0xFF000000, fade));

        RenderUtil.drawTexture(ctx.getMatrices(), "images/gui/pick.png", 50, 5, ps, ps, 14, ColorUtil.applyAlpha(-1, fade));
        RenderUtil.drawRoundedBorder(ctx.getMatrices(), 50, 5, ps, ps, 14, 0.1f, ColorUtil.applyAlpha(-1, fade));
        RenderUtil.drawCircle(ctx.getMatrices(), (int)(50 + p2x * ps), (int)(5 + p2y * ps), 4f, ColorUtil.applyAlpha(0xFF000000, fade));

        RenderUtil.drawRoundedRect(ctx.getMatrices(), pw - 10, 0, 10, 10, new Vector4f(0, 4, 0, 4), ColorUtil.applyAlpha(-1, fade));
        FontUtils.sf_medium[20].drawLeftAligned(ctx.getMatrices(), "×", pw - 8, -1.5f, ColorUtil.applyAlpha(0xFFFF0000, fade));

        RenderUtil.drawRoundedRect(ctx.getMatrices(), 14, 39, 56, 8, new Vector4f(1, 1, 1, 1), ColorUtil.applyAlpha(-1, fade));
        FontUtils.durman[12].drawLeftAligned(ctx.getMatrices(), "Добавить тему", 18, 39, ColorUtil.applyAlpha(0xFF000000, fade));

        ctx.getMatrices().pop();
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (btn == 0 && pickerOpen) {
            int pw = 85, ph = 51, px = (width - TMW) / 2 - pw - 20 + 5, py = (height + PH) / 2 + TMB - ph / 2 - 12 + 5, ps = 30;
            if (dragP1) { p1x = MathHelper.clamp((float)(mx - px) / ps, 0, 1); p1y = MathHelper.clamp((float)(my - py) / ps, 0, 1); selCol1 = ColorUtil.getPixelColor(ResourceProvider.color_image, p1x, p1y); return true; }
            if (dragP2) { p2x = MathHelper.clamp((float)(mx - px - 45) / ps, 0, 1); p2y = MathHelper.clamp((float)(my - py) / ps, 0, 1); selCol2 = ColorUtil.getPixelColor(ResourceProvider.color_image, p2x, p2y); return true; }
        }
        if (dragSlider != null && btn == 0) { sliderR.mouseDragged(dragSlider, mx, dragX, dragW); return true; }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        dragP1 = dragP2 = false;
        if (dragSlider != null) { sliderR.mouseReleased(dragSlider); dragSlider = null; return true; }
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int tw = cats.size() * (PW + PM) - PM, sx = (width - tw) / 2, sy = (height - PH) / 2;
        int srx = searchX(), sry = searchY();

        if (pickerOpen) {
            int pw = 85, ph = 51, tx = (width - TMW) / 2 + 5, ty = (height + PH) / 2 + TMB + (THH - 11) / 2;
            int px = tx - pw - 20, py = ty - ph / 2 - 12, ps = 30;

            if (inRect(mx, my, px + pw - 10, py, 10, 10)) { pickerOpen = false; return true; }
            if (inRect(mx, my, px + 5, py + 5, ps, ps)) { dragP1 = true; p1x = MathHelper.clamp((float)(mx - px - 5) / ps, 0, 1); p1y = MathHelper.clamp((float)(my - py - 5) / ps, 0, 1); selCol1 = ColorUtil.getPixelColor(ResourceProvider.color_image, p1x, p1y); return true; }
            if (inRect(mx, my, px + 50, py + 5, ps, ps)) { dragP2 = true; p2x = MathHelper.clamp((float)(mx - px - 50) / ps, 0, 1); p2y = MathHelper.clamp((float)(my - py - 5) / ps, 0, 1); selCol2 = ColorUtil.getPixelColor(ResourceProvider.color_image, p2x, p2y); return true; }
            if (inRect(mx, my, px + 14, py + 39, 56, 8)) {
                String name = "Custom"; int idx = 2;
                boolean exists = true;
                while (exists) {
                    exists = false;
                    for (Style st : Manager.STYLE_MANAGER.getStyles()) if (st.name.equals(name)) { exists = true; name = "Custom-" + idx++; break; }
                }
                Manager.STYLE_MANAGER.addCustomTheme(name, selCol1, selCol2);
                Manager.STYLE_MANAGER.setTheme(new Style(name, new int[]{selCol1, selCol2}));
                pickerOpen = false; return true;
            }
        }

        if (inRect(mx, my, srx, sry, SMW, SH)) { search.focused = true; search.cursorPosition = search.text.length(); return true; }
        else search.focused = false;

        if (inRect(mx, my, srx + 65, sry + 2, 16, 16)) { themeMenu = !themeMenu; menuTgt = themeMenu ? 1f : 0f; }

        if (themeMenu) {
            int ttx = (width - TMW) / 2, tty = (height + PH) / 2 + TMB, cs = THH - 5, pad = 5;
            float maxS = Math.max(0, (Manager.STYLE_MANAGER.getStyles().size() + 1 - VT) * (cs + pad));
            if (themeTgt > 0 && inRect(mx, my, ttx - 15, tty, 10, THH)) { themeTgt -= (cs + pad) * 3; return true; }
            if (themeTgt < maxS && inRect(mx, my, ttx + TMW + 5, tty, 10, THH)) { themeTgt += (cs + pad) * 3; return true; }

            float cx = ttx + pad - themeOff;
            int cty = tty + (THH - cs) / 2;
            if (inRect(mx, my, (int)cx, cty, cs, cs)) { pickerOpen = !pickerOpen; return true; }
            cx += cs + pad;

            if (btn == 0) for (Style s : Manager.STYLE_MANAGER.getStyles()) { if (inRect(mx, my, (int)cx, cty, cs, cs)) { Manager.STYLE_MANAGER.setTheme(s); return true; } cx += cs + pad; }
            if (btn == 1) { cx = ttx + pad + cs + pad - themeOff; for (Style s : new ArrayList<>(Manager.STYLE_MANAGER.getStyles())) { if (inRect(mx, my, (int)cx, cty, cs, cs) && s.name.startsWith("Custom")) { Manager.STYLE_MANAGER.removeStyle(s); return true; } cx += cs + pad; } }
        }

        if (binding && bindFunc != null) { bindFunc.setBindCode(-(btn + 2)); binding = false; bindFunc = null; return true; }

        for (Type c : cats) for (Function f : Manager.FUNCTION_MANAGER.getFunctions(c)) if (f.expanded) for (Setting s : f.getSettings()) {
            if (!s.isVisible()) continue;
            if (s instanceof BindBooleanSetting b && b.isListeningForBind()) { b.setKey(-(btn + 2)); b.setListeningForBind(false); return true; }
            if (s instanceof BindSetting b && b.isBinding()) { b.setKey(-(btn + 2)); b.setBinding(false); return true; }
        }

        int i = 0;
        for (Type c : cats) {
            int px = sx + i++ * (PW + PM);
            float off = scrollOff.get(c), cy = sy + SAY - off;

            for (Function f : Manager.FUNCTION_MANAGER.getFunctions(c)) {
                if (!visible(f)) continue;
                float prog = expProg.getOrDefault(f, f.expanded ? 1f : 0f);
                int sh = (int)(settingsH(f) * prog), th = FH + sh;

                if (cy + th < sy + SAY) { cy += th; continue; }
                if (cy > sy + SAY + SAH) break;

                if (inRect(mx, my, px, (int)cy, PW, FH) && my >= sy + SAY && my <= sy + SAY + SAH) {
                    if (btn == 0) { f.toggle(); return true; }
                    if (btn == 1) { f.expanded = !f.expanded; clampScroll(c); return true; }
                    if (btn == 2) { binding = true; bindFunc = f; return true; }
                }

                if (sh > 0) {
                    float sty = cy + FH;
                    for (Setting s : f.getSettings()) {
                        if (!s.isVisible()) continue;
                        int h = settingH(s, PW - 20);
                        if (h <= 0) continue;
                        if (inRect(mx, my, px, (int)sty, PW, h) && my >= sy + SAY && my <= sy + SAY + SAH) {
                            if (clickSetting(s, mx, my, btn, px + 10, (int)sty, PW - 20, h)) return true;
                        }
                        sty += h + 1;
                    }
                }
                cy += th;
            }
        }

        for (Type c : cats) for (Function f : Manager.FUNCTION_MANAGER.getFunctions(c)) if (f.expanded) for (Setting s : f.getSettings())
            if (s instanceof TextSetting t) t.setFocused(false);

        return super.mouseClicked(mx, my, btn);
    }

    private boolean clickSetting(Setting s, double mx, double my, int btn, int x, int y, int w, int h) {
        if (s instanceof BooleanSetting b) return boolR.mouseClicked(b, mx, my, btn, x, y, w, h);
        if (s instanceof BindBooleanSetting b) return bindBoolR.mouseClicked(b, mx, my, btn, x, y, w, h);
        if (s instanceof BindSetting b) return bindR.mouseClicked(b, mx, my, btn, x, y - 2, w, h);
        if (s instanceof ModeSetting m) return modeR.mouseClicked(m, mx, my, btn, x, y, w, h);
        if (s instanceof MultiSetting m) return multiR.mouseClicked(m, mx, my, btn, x, y, w, h);
        if (s instanceof SliderSetting sl && sliderR.mouseClicked(sl, mx, my, btn, x, y - 2, w, h)) { dragSlider = sl; dragX = x; dragW = w; return true; }
        if (s instanceof TextSetting t) return textR.mouseClicked(t, mx, my, btn, x, y, w, h);
        return false;
    }

    private void expiry(DrawContext ctx, float fade) {
        if (fade < 0.01f) return;
        String t = "Окончание - " + Manager.USER_PROFILE.getExpiry();
        FontUtils.durman[18].drawLeftAligned(ctx.getMatrices(), t, 9, mc.getWindow().getScaledHeight() - (int)FontUtils.durman[18].getHeight() - 5, ColorUtil.applyAlpha(-1, fade));
    }

    private boolean visible(Function f) {
        String q = search.text.toLowerCase();
        return q.isEmpty() || f.name.toLowerCase().contains(q) || f.keywords.toLowerCase().contains(q);
    }

    @Override public void renderBackground(DrawContext ctx, int mx, int my, float d) {}
    @Override public boolean shouldPause() { return false; }

    private int settingsH(Function f) {
        int h = 0;
        for (Setting s : f.getSettings()) if (s.isVisible()) h += settingH(s, PW - 20) + 1;
        return Math.max(0, h);
    }

    private int settingH(Setting s, int w) {
        if (!s.isVisible()) return 0;
        if (s instanceof BooleanSetting) return boolR.getHeight();
        if (s instanceof BindBooleanSetting) return bindBoolR.getHeight();
        if (s instanceof BindSetting) return bindR.getHeight();
        if (s instanceof ModeSetting m) return modeR.getHeight(m, w);
        if (s instanceof MultiSetting m) return multiR.getHeight(m, w);
        if (s instanceof SliderSetting) return sliderR.getHeight();
        if (s instanceof TextSetting) return textR.getHeight();
        return 0;
    }

    private void clampScroll(Type c) {
        int max = maxScroll(c);
        scrollTgt.put(c, MathHelper.clamp(scrollTgt.get(c), 0f, max));
        scrollOff.put(c, MathHelper.clamp(scrollOff.get(c), 0f, max));
    }

    private boolean inRect(double mx, double my, float x, float y, float w, float h) { return mx >= x && mx <= x + w && my >= y && my <= y + h; }
}