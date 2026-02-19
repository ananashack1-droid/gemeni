package sky.client.modules.render;

import com.google.common.collect.Lists;
import lombok.Getter;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.texture.Sprite;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.AirBlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;
import sky.client.Main;
import sky.client.events.Event;
import sky.client.events.impl.EventUpdate;
import sky.client.events.impl.render.EventRender2D;
import sky.client.manager.ClientManager;
import sky.client.manager.Manager;
import sky.client.manager.dragManager.Dragging;
import sky.client.manager.fontManager.FontUtils;
import sky.client.mixin.iface.ItemCooldownEntryAccessor;
import sky.client.mixin.iface.ItemCooldownManagerAccessor;
import sky.client.modules.Function;
import sky.client.modules.FunctionAnnotation;
import sky.client.modules.Type;
import sky.client.modules.setting.*;
import sky.client.util.animations.Animation;
import sky.client.util.animations.impl.EaseBackIn;
import sky.client.util.color.ColorUtil;
import sky.client.util.math.MathUtil;
import sky.client.util.player.ServerUtil;
import sky.client.util.render.RenderAddon;
import sky.client.util.render.RenderUtil;
import sky.client.util.render.Scissor;

import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

import static sky.client.util.render.RenderUtil.*;

@SuppressWarnings("All")
@FunctionAnnotation(name = "HUD", desc = "Интерфейс клиента", type = Type.Render)
public class HUD extends Function {

    private static final Pattern NAME_PATTERN = Pattern.compile("^\\w{3,16}$");
    private static final Pattern STAFF_PATTERN = Pattern.compile(".*(mod|мод|adm|адм|help|хелп|curat|курат|own|овн|dev|supp|сапп|yt|ют|сотруд).*", Pattern.CASE_INSENSITIVE);

    private static final int CARD_BG = ColorUtil.rgba(21, 21, 20, 210);
    private static final int CARD_LINE = ColorUtil.rgba(255, 255, 255, 50);
    private static final int TEXT_DIM = ColorUtil.rgba(255, 255, 255, 180);
    private static final int BAR_BG = ColorUtil.rgba(255, 255, 255, 40);
    private static final int BAR_FILL = ColorUtil.rgba(255, 255, 255, 255);
    private static final int ABSORB = ColorUtil.rgba(255, 215, 0, 200);
    // Темная обводка
    private static final int OUTLINE_COLOR = ColorUtil.rgba(15, 15, 15, 180);

    private static final float CARD_R = 4f;
    private static final float HEADER_H = 19f;
    private static final float ROW_H = 11f;
    private static final float PAD_BOT = 6f;

    private static final Item[] TRACKED_ITEMS = {
            Items.ENDER_PEARL, Items.CHORUS_FRUIT, Items.FIREWORK_ROCKET, Items.SHIELD,
            Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE, Items.TOTEM_OF_UNDYING,
            Items.SNOWBALL, Items.DRIED_KELP, Items.ENDER_EYE, Items.NETHERITE_SCRAP,
            Items.EXPERIENCE_BOTTLE, Items.PHANTOM_MEMBRANE
    };

    private static final Map<Item, String> ITEM_NAMES = Map.ofEntries(
            Map.entry(Items.ENDER_PEARL, "Эндер-жемчюг"), Map.entry(Items.CHORUS_FRUIT, "Хорус"),
            Map.entry(Items.FIREWORK_ROCKET, "Фейрверк"), Map.entry(Items.SHIELD, "Щит"),
            Map.entry(Items.GOLDEN_APPLE, "Золотое яблоко"), Map.entry(Items.ENCHANTED_GOLDEN_APPLE, "Чарка"),
            Map.entry(Items.TOTEM_OF_UNDYING, "Тотем"), Map.entry(Items.SNOWBALL, "Снежок"),
            Map.entry(Items.DRIED_KELP, "Пласт"), Map.entry(Items.ENDER_EYE, "Дезориентация"),
            Map.entry(Items.NETHERITE_SCRAP, "Трапка"), Map.entry(Items.EXPERIENCE_BOTTLE, "Пузырёк опыта"),
            Map.entry(Items.PHANTOM_MEMBRANE, "Аура")
    );

    public final MultiSetting setting = new MultiSetting("Элементы",
            Arrays.asList("WaterMark", "TargetHUD", "KeyBinds", "StaffList", "PotionHUD", "ItemCoolDownHUD", "Coordinates / TPS", "ArmorHUD", "Notifications"),
            new String[]{"WaterMark", "TargetHUD", "KeyBinds", "StaffList", "PotionHUD", "ItemCoolDownHUD", "Coordinates / TPS", "ArmorHUD", "Notifications"});

    private final BooleanSetting outline = new BooleanSetting("Обводка", true);
    private final SliderSetting outlineWidth = new SliderSetting("Толщина обводки", 1.5f, 0.5f, 3.0f, 0.1f);

    private final ModeSetting hudColor = new ModeSetting("Цвет худа", "Обычный", "Обычный", "Зависит от темы");
    private final ModeSetting gradientType = new ModeSetting(() -> hudColor.is("Зависит от темы"), "Тип градиента", "Слева направо", "Слева направо", "Справа налево");
    private final SliderSetting customAlpha = new SliderSetting("Прозрачность", 120, 120, 255, 5);
    private final BooleanSetting visibleCrosshair = new BooleanSetting("Показывать TargetHUD при навидении", false, "", () -> setting.get("TargetHUD"));
    private final BooleanSetting blur = new BooleanSetting("Размытие", false);
    private final SliderSetting headRounding = new SliderSetting("Закругление головы", 2f, 0f, 12f, 1f);

    public final Dragging watermarkDrag = Main.getInstance().createDrag(this, "WaterMark", 10, 10);
    public final Dragging targethudDrag = Main.getInstance().createDrag(this, "TargetHUD", 10, 45);
    public final Dragging keybindsDrag = Main.getInstance().createDrag(this, "KeyBindsHUD", 10, 95);
    public final Dragging stafflistDrag = Main.getInstance().createDrag(this, "StaffListHUD", 10, 128);
    public final Dragging cooldownDrag = Main.getInstance().createDrag(this, "CoolDownHUD", 10, 165);
    public final Dragging potionDrag = Main.getInstance().createDrag(this, "PotionHUD", 10, 198);
    public final Dragging coordsDrag = Main.getInstance().createDrag(this, "CoordinatesHUD", 10, 198);
    public final Dragging armorDrag = Main.getInstance().createDrag(this, "ArmorHUD", 478, 468);

    private final Animation tHudAnim = new EaseBackIn(300, 1, 1.5f);
    private final List<StaffPlayer> staffList = new ArrayList<>(32);
    private final Set<String> addedStaff = new HashSet<>(64);

    private LivingEntity target;
    private float lastHp, lastAbsorb;
    private float potionH, cooldownH, keybindsH, staffH, staffW, staffNameW;

    public HUD() {
        addSettings(setting, outline, outlineWidth, hudColor, gradientType, customAlpha, visibleCrosshair, blur, headRounding);
    }

    @Override
    public void onEvent(Event event) {
        if (mc == null || mc.player == null || mc.world == null) return;

        if (event instanceof EventUpdate && setting.get("StaffList")) updateStaff();

        if (event instanceof EventRender2D e) {
            if (setting.get("WaterMark")) waterMark(e);
            if (setting.get("TargetHUD")) targetHud(e);
            if (setting.get("StaffList")) staffList(e);
            if (setting.get("KeyBinds")) keyBinds(e);
            if (setting.get("ItemCoolDownHUD")) cooldowns(e);
            if (setting.get("PotionHUD")) potions(e);
            if (setting.get("Coordinates / TPS")) coords(e);
            if (setting.get("ArmorHUD")) armor(e);
        }
    }

    @Override
    public void onDisable() {
        staffList.clear();
        addedStaff.clear();
    }

    private float anim(float cur, float target) {
        return MathUtil.fast(cur, target, 15);
    }

    private float rowsStartY(float y) {
        return y + HEADER_H + 7.5f;
    }

    private float iconY(float y) {
        return y + (HEADER_H - 9f) * 0.5f + 0.5f;
    }

    private float titleY(float y) {
        return y + (HEADER_H - 10f) * 0.5f + 1.0f;
    }

    private void drawCard(EventRender2D e, float x, float y, float w, float h) {
        var m = e.getDrawContext().getMatrices();
        if (blur.get() && customAlpha.get().intValue() <= 240) {
            drawBlur(m, x, y, w, h, new Vector4f(CARD_R, CARD_R, CARD_R, CARD_R), 8, -1);
        }
        drawRoundedRect(m, x, y, w, h, CARD_R, CARD_BG);

        // ТЕМНАЯ ЗАКРУГЛЕННАЯ ОБВОДКА
        if (outline.get()) {
            float width = outlineWidth.get().floatValue();
            // Используем drawRoundedBorder для красивой закругленной обводки
            drawRoundedBorder(m, x, y, w, h, CARD_R, width, OUTLINE_COLOR);
        }
    }

    private void drawCardTitle(EventRender2D e, float x, float y, String title, @Nullable String iconTex) {
        var m = e.getDrawContext().getMatrices();
        if (iconTex != null) {
            drawTexture(m, iconTex, x + 6f, iconY(y), 9, 9, 0, -1);
        }
        FontUtils.durman[15].drawLeftAligned(m, title, x + 16.5f, titleY(y), -1);
    }

    private void drawLeftTick(EventRender2D e, float x, float lineY) {
        drawRoundedRect(e.getDrawContext().getMatrices(), x, lineY + 3.8f, 2f, 1f, 0.25f, CARD_LINE);
    }

    private void waterMark(EventRender2D e) {
        float x = watermarkDrag.getX(), y = watermarkDrag.getY();
        var m = e.getDrawContext().getMatrices();

        String client = Main.getInstance().name;
        String ver = SharedConstants.getGameVersion().getName();
        String username = Manager.USER_PROFILE.getName();
        String fps = ClientManager.getFps() + "fps";
        String ping = ClientManager.getPing() + "ms";

        var f = FontUtils.durman[15];
        var fBig = FontUtils.durman[16];

        float pad = 6f;
        float h = 22f;

        float wClient = fBig.getWidth(client);
        float wVer = f.getWidth(ver);
        float wUser = f.getWidth(username);
        float wFps = f.getWidth(fps);
        float wPing = f.getWidth(ping);

        float w = pad + wClient + 6 + wVer + 10 + wUser + 10 + wFps + 10 + wPing + pad;

        drawCard(e, x, y, w, h);

        float cx = x + pad;
        fBig.drawLeftAligned(m, client, cx, y + 6f, -1);
        cx += wClient + 6;

        f.drawLeftAligned(m, ver, cx, y + 7f, TEXT_DIM);
        cx += wVer + 10;

        drawRoundedRect(m, cx - 6, y + 6f, 1f, 10f, 0, CARD_LINE);

        f.drawLeftAligned(m, username, cx, y + 7f, -1);
        cx += wUser + 10;

        drawRoundedRect(m, cx - 6, y + 6f, 1f, 10f, 0, CARD_LINE);

        f.drawLeftAligned(m, fps, cx, y + 7f, -1);
        cx += wFps + 10;

        drawRoundedRect(m, cx - 6, y + 6f, 1f, 10f, 0, CARD_LINE);

        f.drawLeftAligned(m, ping, cx, y + 7f, -1);

        watermarkDrag.setWidth(w);
        watermarkDrag.setHeight(h);
    }

    private void targetHud(EventRender2D e) {
        float x = targethudDrag.getX(), y = targethudDrag.getY();
        target = resolveTarget();

        double scale = tHudAnim.getOutput();
        if (scale == 0.0 || target == null) return;

        float hp = MathHelper.clamp(target.getHealth(), 0f, target.getMaxHealth());
        float maxHp = Math.max(1f, target.getMaxHealth());

        lastHp = MathUtil.fast(lastHp, hp, 8);
        lastAbsorb = MathUtil.fast(
                lastAbsorb,
                Math.max(0f, target.getAbsorptionAmount()),
                8
        );

        float hpPct = MathHelper.clamp(lastHp / maxHp, 0f, 1f);
        float absorbPct = MathHelper.clamp(
                lastAbsorb / (maxHp + lastAbsorb),
                0f,
                1f
        );

        float headW = 35f, headH = 35f;
        float gap = 5f;
        float infoW = 85f, infoH = 35f;
        float totalW = headW + gap + infoW;

        e.getMatrixStack().push();
        RenderAddon.sizeAnimation(
                e.getMatrixStack(),
                x + totalW / 2f,
                y + infoH / 2f,
                scale
        );

        drawCard(e, x, y, headW, headH);
        RenderAddon.drawHead(
                e.getDrawContext().getMatrices(),
                target,
                x + 3f,
                y + 3f,
                headW - 6f,
                headRounding.get().floatValue()
        );

        float ix = x + headW + gap;
        drawCard(e, ix, y, infoW, infoH);

        String name = Manager.FUNCTION_MANAGER.nameProtect
                .getProtectedName(target.getName().getString());
        if (name.length() > 14) name = name.substring(0, 14);

        var nameFont = FontUtils.durman[15];
        var hpFont = FontUtils.durman[13];

        nameFont.drawLeftAligned(
                e.getDrawContext().getMatrices(),
                name,
                ix + 5f,
                y + 6f,
                -1
        );

        String hpText = String.format("HP: %.1f", lastHp);
        hpFont.drawLeftAligned(
                e.getDrawContext().getMatrices(),
                hpText,
                ix + 5f,
                y + 18f,
                TEXT_DIM
        );

        float barX = ix + 5f;
        float barY = y + 27f;
        float barW = 75f;
        float barH = 5f;

        drawRoundedRect(
                e.getDrawContext().getMatrices(),
                barX, barY,
                barW, barH,
                2.5f,
                BAR_BG
        );

        drawRoundedRect(
                e.getDrawContext().getMatrices(),
                barX, barY,
                barW * hpPct,
                barH,
                2.5f,
                BAR_FILL
        );

        if (lastAbsorb > 0.1f) {
            drawRoundedRect(
                    e.getDrawContext().getMatrices(),
                    barX, barY,
                    barW * absorbPct,
                    barH,
                    2.5f,
                    ABSORB
            );
        }

        e.getMatrixStack().pop();

        targethudDrag.setWidth(totalW);
        targethudDrag.setHeight(infoH);
    }

    private void keyBinds(EventRender2D e) {
        float x = keybindsDrag.getX(), y = keybindsDrag.getY();
        var font = FontUtils.durman[13];

        List<String[]> binds = new ArrayList<>();
        for (var f : Manager.FUNCTION_MANAGER.getFunctions()) {
            if (f.bind != 0 && f.state) {
                binds.add(new String[]{f.name, shortKey(ClientManager.getKey(f.bind)).toUpperCase()});
            }
            for (var s : f.getSettings()) {
                if (s instanceof BindBooleanSetting bs && bs.isVisible() && bs.getBindKey() != 0 && bs.get()) {
                    binds.add(new String[]{bs.getName(), shortKey(ClientManager.getKey(bs.getBindKey())).toUpperCase()});
                }
            }
        }
        if (binds.isEmpty()) return;

        float w = 96f;
        for (var b : binds) w = Math.max(w, 20 + font.getWidth(b[0]) + font.getWidth(b[1]) + 20);

        keybindsH = anim(keybindsH, binds.size() * ROW_H + PAD_BOT);
        float h = HEADER_H + keybindsH;

        drawCard(e, x, y, w, h);
        drawCardTitle(e, x, y, "Keybinds", "images/hud/keybinds.png");

        float off = 0f;
        for (var b : binds) {
            float lineY = rowsStartY(y) + off;

            drawLeftTick(e, x + 7f, lineY);
            font.drawLeftAligned(e.getDrawContext().getMatrices(), b[0], x + 16f, lineY, -1);

            float kw = font.getWidth(b[1]);
            font.drawLeftAligned(e.getDrawContext().getMatrices(), b[1], x + w - kw - 8f, lineY, TEXT_DIM);

            off += ROW_H;
        }

        keybindsDrag.setWidth(w);
        keybindsDrag.setHeight(h);
    }

    private void potions(EventRender2D e) {
        float x = potionDrag.getX(), y = potionDrag.getY();
        List<StatusEffectInstance> effs = new ArrayList<>(mc.player.getStatusEffects());
        if (effs.isEmpty()) return;

        float w = 92f;
        var font = FontUtils.durman[13];

        for (var eff : effs) {
            String n = I18n.translate(eff.getEffectType().value().getTranslationKey());
            String d = fmtDuration(eff);
            w = Math.max(w, 20 + font.getWidth(n) + font.getWidth(d) + 20);
        }

        potionH = anim(potionH, effs.size() * ROW_H + PAD_BOT);
        float h = HEADER_H + potionH;

        drawCard(e, x, y, w, h);
        drawCardTitle(e, x, y, "Potions", "images/hud/potion.png");

        Scissor.push();
        Scissor.setFromComponentCoordinates(x, y, w, h);

        float off = 0f;
        var sprites = mc.getStatusEffectSpriteManager();
        List<Runnable> delayed = Lists.newArrayList();

        for (var eff : effs) {
            String n = I18n.translate(eff.getEffectType().value().getTranslationKey());
            if (eff.getAmplifier() > 0) n += " " + (eff.getAmplifier() + 1);
            String d = fmtDuration(eff);

            int col = (eff.getDuration() <= 60 && eff.getDuration() > 0)
                    ? ColorUtil.rgba(255, 255, 255, (int) (Math.sin(System.currentTimeMillis() / 200.0) * 80 + 128))
                    : -1;

            float lineY = rowsStartY(y) + off;

            Sprite sp = sprites.getSprite(eff.getEffectType());
            float fy = lineY;
            delayed.add(() -> e.getDrawContext().drawSpriteStretched(RenderLayer::getGuiTextured, sp, (int) (x + 6f), (int) (fy - 1f), 9, 9, -1));

            drawLeftTick(e, x + 7f, lineY);

            font.drawLeftAligned(e.getDrawContext().getMatrices(), n, x + 16f, lineY, col);

            // ВОЗВРАЩАЕМ ДИНАМИЧЕСКУЮ ПОЗИЦИЮ (без фикса)
            float dw = font.getWidth(d);
            font.drawLeftAligned(e.getDrawContext().getMatrices(), d, x + w - dw - 6f, lineY, TEXT_DIM);

            off += ROW_H;
        }

        Scissor.unset();
        Scissor.pop();
        delayed.forEach(Runnable::run);

        potionDrag.setWidth(w);
        potionDrag.setHeight(h);
    }

    private void cooldowns(EventRender2D e) {
        float x = cooldownDrag.getX(), y = cooldownDrag.getY();

        var mgr = mc.player.getItemCooldownManager();
        var acc = (ItemCooldownManagerAccessor) mgr;

        List<Item> active = new ArrayList<>();
        var font = FontUtils.durman[13];

        float w = 96f;
        for (Item item : TRACKED_ITEMS) {
            ItemStack stack = new ItemStack(item);
            if (!mgr.isCoolingDown(stack)) continue;
            active.add(item);

            String n = ITEM_NAMES.getOrDefault(item, stack.getName().getString());
            String t = fmtCooldown(mgr, acc, stack);

            w = Math.max(w, 20 + font.getWidth(n) + font.getWidth(t) + 20);
        }
        if (active.isEmpty()) return;

        cooldownH = anim(cooldownH, active.size() * ROW_H + PAD_BOT);
        float h = HEADER_H + cooldownH;

        drawCard(e, x, y, w, h);
        drawCardTitle(e, x, y, "Cooldowns", "images/hud/cooldown.png");

        Scissor.push();
        Scissor.setFromComponentCoordinates(x, y, w, h);

        float off = 0f;
        for (Item item : active) {
            ItemStack stack = item.getDefaultStack();
            String n = ITEM_NAMES.getOrDefault(item, stack.getName().getString());
            String t = fmtCooldown(mgr, acc, stack);

            float lineY = rowsStartY(y) + off;

            RenderAddon.renderItem(e.getDrawContext(), stack, x + 5.5f, lineY - 2f, 0.7f, false);

            drawLeftTick(e, x + 7f, lineY);

            font.drawLeftAligned(e.getDrawContext().getMatrices(), n, x + 16f, lineY, -1);

            float tw = font.getWidth(t);
            font.drawLeftAligned(e.getDrawContext().getMatrices(), t, x + w - tw - 6f, lineY, TEXT_DIM);

            off += ROW_H;
        }

        Scissor.unset();
        Scissor.pop();

        cooldownDrag.setWidth(w);
        cooldownDrag.setHeight(h);
    }

    private void staffList(EventRender2D e) {
        float x = stafflistDrag.getX(), y = stafflistDrag.getY();
        var font = FontUtils.durman[13];

        staffNameW = 0;
        for (var s : staffList) staffNameW = Math.max(staffNameW, font.getWidth(s.name));

        float w = 96f;
        float maxStatusW = Math.max(font.getWidth("[GM3]"), Math.max(font.getWidth("[N]"), Math.max(font.getWidth("[V]"), font.getWidth("[ON]"))));
        w = Math.max(w, 20 + staffNameW + maxStatusW + 20);

        staffW = anim(staffW, w);
        staffH = anim(staffH, (staffList.isEmpty() ? 0 : (staffList.size() * ROW_H + PAD_BOT)));
        float h = HEADER_H + staffH;

        drawCard(e, x, y, staffW, h);
        drawCardTitle(e, x, y, "Staffs", "images/hud/staff.png");

        if (staffList.isEmpty()) {
            stafflistDrag.setWidth(staffW);
            stafflistDrag.setHeight(h);
            return;
        }

        Scissor.push();
        Scissor.setFromComponentCoordinates(x, y, staffW, h);

        Map<String, PlayerListEntry> entries = new HashMap<>();
        mc.getNetworkHandler().getPlayerList().forEach(i -> entries.put(i.getProfile().getName(), i));

        float off = 0f;
        for (var s : staffList) {
            float lineY = rowsStartY(y) + off;

            PlayerListEntry info = entries.get(s.name);

            if (info != null && s.status != Status.VANISHED && s.status != Status.SPEC) {
                RenderAddon.drawStaffHead(e.getDrawContext().getMatrices(), info.getSkinTextures().texture(), x + 5.5f, lineY - 2f, 9, 3);
            } else {
                drawTexture(e.getDrawContext().getMatrices(), "images/hud/staffvanish.png", x + 5.5f, lineY - 2f, 9, 9, 3, -1);
            }

            drawLeftTick(e, x + 7f, lineY);

            String nm = s.name.length() > 12 ? s.name.substring(0, 12) : s.name;
            font.drawLeftAligned(e.getDrawContext().getMatrices(), nm, x + 16f, lineY, -1);

            String st = s.status.text;
            float stW = font.getWidth(st);
            font.drawLeftAligned(e.getDrawContext().getMatrices(), st, x + staffW - stW - 6f, lineY, s.status.color);

            off += ROW_H;
            if (off + PAD_BOT > staffH) break;
        }

        Scissor.unset();
        Scissor.pop();

        stafflistDrag.setWidth(staffW);
        stafflistDrag.setHeight(h);
    }

    private void coords(EventRender2D e) {
        float x = coordsDrag.getX(), y = coordsDrag.getY();
        var font = FontUtils.sf_bold[17];

        String coords = String.format("X: %d Y: %d Z: %d", (int) mc.player.getX(), (int) mc.player.getY(), (int) mc.player.getZ());
        String tps = "TPS: " + ClientManager.getTPS();

        float w = Math.max(font.getWidth(coords), font.getWidth(tps));
        boolean left = x < mc.getWindow().getScaledWidth() / 2;

        if (left) {
            font.drawLeftAligned(e.getDrawContext().getMatrices(), tps, x, y, -1);
            font.drawLeftAligned(e.getDrawContext().getMatrices(), coords, x, y + 12, -1);
        } else {
            font.drawRightAligned(e.getDrawContext().getMatrices(), tps, x + w, y, -1);
            font.drawRightAligned(e.getDrawContext().getMatrices(), coords, x + w, y + 12, -1);
        }

        coordsDrag.setWidth(w);
        coordsDrag.setHeight(24);
    }

    private void armor(EventRender2D e) {
        float x = armorDrag.getX(), y = armorDrag.getY();

        int cnt = 0;
        for (int i = 0; i < 4; i++) if (!mc.player.getInventory().armor.get(i).isEmpty()) cnt++;

        int w = cnt > 0 ? 18 * cnt : 35;
        armorDrag.setWidth(w);
        armorDrag.setHeight(20);

        float sx = x;
        for (int i = 3; i >= 0; i--) {
            ItemStack stack = mc.player.getInventory().armor.get(i);
            if (stack.isEmpty()) continue;

            e.getDrawContext().getMatrices().push();
            e.getDrawContext().getMatrices().translate(sx, y, 0);
            e.getDrawContext().drawItem(stack, 0, 0, 0);
            e.getDrawContext().drawStackOverlay(mc.textRenderer, stack, 0, 0);
            e.getDrawContext().getMatrices().pop();

            sx += 18;
        }
    }

    private LivingEntity resolveTarget() {
        LivingEntity res = target;
        if (Manager.FUNCTION_MANAGER.attackAura.target instanceof LivingEntity t) {
            res = t;
            tHudAnim.setDirection(Direction.AxisDirection.POSITIVE);
        } else if (visibleCrosshair.get() && mc.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity t) {
            res = t;
            tHudAnim.setDirection(Direction.AxisDirection.POSITIVE);
        } else if (mc.currentScreen instanceof ChatScreen) {
            res = mc.player;
            tHudAnim.setDirection(Direction.AxisDirection.POSITIVE);
        } else {
            tHudAnim.setDirection(Direction.AxisDirection.NEGATIVE);
        }
        return res;
    }

    private void updateStaff() {
        staffList.clear();
        addedStaff.clear();

        Map<String, PlayerListEntry> entries = new HashMap<>();
        mc.player.networkHandler.getPlayerList().forEach(e -> {
            if (e.getProfile() != null && e.getProfile().getName() != null)
                entries.put(e.getProfile().getName().toLowerCase(Locale.ROOT), e);
        });

        String me = mc.player.getName().getString();
        for (Team team : mc.world.getScoreboard().getTeams()) {
            String prefix = repairStr(team.getPrefix().getString()).toLowerCase(Locale.ROOT);
            for (String member : team.getPlayerList()) {
                if (member == null || member.equals(me) || addedStaff.contains(member)) continue;
                if (!NAME_PATTERN.matcher(member).matches()) continue;

                PlayerListEntry entry = entries.get(member.toLowerCase(Locale.ROOT));
                if (entry != null && (STAFF_PATTERN.matcher(prefix).matches() || Manager.STAFF_MANAGER.isStaff(member))) {
                    staffList.add(new StaffPlayer(member, entry.getProfile().getId()));
                    addedStaff.add(member);
                } else if (entry == null && !team.getPrefix().getString().isEmpty()) {
                    staffList.add(new StaffPlayer(member, null));
                    addedStaff.add(member);
                }
            }
        }
        staffList.sort(Comparator.comparing(s -> s.name));
    }

    private String fmtDuration(StatusEffectInstance eff) {
        if (eff.isInfinite() || eff.getDuration() > 18000) return "**:**";
        return StatusEffectUtil.getDurationText(eff, 1f, 20f).getString().replace("{", "").replace("}", "");
    }

    private String fmtCooldown(ItemCooldownManager mgr, ItemCooldownManagerAccessor acc, ItemStack stack) {
        Object raw = acc.getEntries().get(mgr.getGroup(stack));
        if (raw instanceof ItemCooldownEntryAccessor e) {
            int s = (int) (Math.max(0f, e.getEndTick() - (acc.getTick() + mc.getRenderTickCounter().getTickDelta(true))) / 20f);
            int m = s / 60;
            return m > 0 ? (s % 60 > 0 ? String.format("%dм %02dс", m, s % 60) : m + "м") : s + "с";
        }
        return "0с";
    }

    private String shortKey(String k) {
        if (k == null) return "";
        String u = k.toUpperCase();
        return u.length() > 6 ? u.substring(0, 6) + "…" : u;
    }

    private String repairStr(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) sb.append(c >= 65281 && c <= 65374 ? (char) (c - 65248) : c);
        return sb.toString();
    }

    @Getter
    public class StaffPlayer {
        private final String name;
        private final UUID uuid;
        private Status status;

        StaffPlayer(String name, @Nullable UUID uuid) {
            this.name = name;
            this.uuid = uuid;
            updateStatus();
        }

        void updateStatus() {
            if (mc == null || mc.world == null || mc.getNetworkHandler() == null) {
                status = Status.VANISHED;
                return;
            }

            PlayerListEntry entry = null;
            for (var e : mc.getNetworkHandler().getPlayerList()) {
                if (uuid != null && uuid.equals(e.getProfile().getId())) {
                    entry = e;
                    break;
                }
                if (uuid == null && name.equalsIgnoreCase(e.getProfile().getName())) {
                    entry = e;
                    break;
                }
            }

            if (entry == null) {
                status = Status.VANISHED;
            } else if (entry.getGameMode() == GameMode.SPECTATOR) {
                status = Status.SPEC;
            } else if (mc.world.getPlayerByUuid(entry.getProfile().getId()) != null) {
                status = Status.NEAR;
            } else {
                status = Status.NONE;
            }
        }
    }

    @Getter
    public enum Status {
        NONE("[ON]", 0xFF5CDA13),
        NEAR("[N]", 0xFFAE6E20),
        SPEC("[GM3]", 0xFFCA2828),
        VANISHED("[V]", 0xFFA9A9A9);

        private final String text;
        private final int color;

        Status(String text, int color) {
            this.text = text;
            this.color = color;
        }
    }
}