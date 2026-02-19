package sky.client.modules.render;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.joml.Vector3d;
import sky.client.events.Event;
import sky.client.events.impl.render.EventRender2D;
import sky.client.manager.Manager;
import sky.client.manager.fontManager.FontUtils;
import sky.client.modules.Function;
import sky.client.modules.FunctionAnnotation;
import sky.client.modules.Type;
import sky.client.modules.setting.BooleanSetting;
import sky.client.modules.setting.MultiSetting;
import sky.client.util.render.RenderAddon;
import sky.client.util.render.RenderUtil;
import sky.client.util.render.providers.ResourceProvider;
import sky.client.util.vector.EntityPosition;
import sky.client.util.vector.VectorUtil;

import java.util.*;

@FunctionAnnotation(name = "NameTags", desc = "Нейм таги", type = Type.Render)
public class NameTags extends Function {

    private static final int BG = 0x961E1E1E;
    private static final Set<String> ENCHANTS = Set.of(
            "Protection", "Защита", "Unbreaking", "Прочность", "Looting", "Добыча",
            "Fortune", "Удача", "Efficiency", "Эффективность", "Power", "Сила",
            "Feather Falling", "Невесомость", "Thorns", "Шипы", "Silk Touch", "Шёлковое касание",
            "Respiration", "Подводное дыхание", "Mending", "Починка", "Knockback", "Отдача",
            "Curse of Vanishing", "Проклятие утраты"
    );

    public final MultiSetting tags = new MultiSetting("Энтити", Arrays.asList("Игроки"), new String[]{"Игроки", "Предметы на земле"});
    private final BooleanSetting armorRender = new BooleanSetting("Показывать предметы", true, () -> tags.get("Игроки"));
    private final BooleanSetting effectRender = new BooleanSetting("Показывать эффекты", true, () -> tags.get("Игроки"));
    private final BooleanSetting enchantRender = new BooleanSetting("Показывать чары", true, () -> tags.get("Игроки"));
    private final BooleanSetting sphereRender = new BooleanSetting("Показывать Шары/Талисманы", true, () -> tags.get("Игроки"));
    private final BooleanSetting shulkerCheck = new BooleanSetting("Показывать содержимое шалкеров", true, () -> tags.get("Предметы на земле"));

    public NameTags() {
        addSettings(tags, armorRender, effectRender, enchantRender, sphereRender, shulkerCheck);
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof EventRender2D e) {
            if (tags.get("Игроки")) renderPlayers(e);
            if (tags.get("Предметы на земле")) renderItems(e);
        }
    }

    private boolean onScreen(Vector3d v) {
        return v.z >= 0 && v.x >= 0 && v.x <= mc.getWindow().getScaledWidth() && v.y >= 0 && v.y <= mc.getWindow().getScaledHeight();
    }

    private void renderPlayers(EventRender2D e) {
        float delta = e.getDeltatick().getTickDelta(true);
        MatrixStack ms = e.getMatrixStack();

        for (PlayerEntity p : Manager.SYNC_MANAGER.getPlayers()) {
            if (p == null || (p instanceof ClientPlayerEntity && mc.options.getPerspective().isFirstPerson())) continue;

            Vector3d vec = VectorUtil.toScreen(EntityPosition.get(p, 2.0f, delta));
            if (!onScreen(vec)) continue;

            boolean isFriend = Manager.FRIEND_MANAGER.isFriend(p.getName().getString());
            String friendTag = isFriend ? Formatting.GRAY + "[" + Formatting.GREEN + "F" + Formatting.GRAY + "] " : "";
            Text prefix = p.getScoreboardTeam() != null ? p.getScoreboardTeam().getPrefix() : Text.empty();
            String name = Manager.FUNCTION_MANAGER.nameProtect.getProtectedName(p.getGameProfile().getName());
            float hp = p.getHealth() + p.getAbsorptionAmount();
            String hpText = Formatting.GRAY + " [" + Formatting.RED + (hp < 300 ? (int) hp : "?") + Formatting.GRAY + "]";

            Text itemText = null;
            if (sphereRender.get()) {
                ItemStack off = p.getOffHandStack();
                if (!off.isEmpty() && (off.isOf(Items.TOTEM_OF_UNDYING) || off.getItem() instanceof PlayerHeadItem)) {
                    itemText = off.getCustomName();
                }
            }

            float friendW = mc.textRenderer.getWidth(friendTag) * 0.7f;
            float prefixW = mc.textRenderer.getWidth(prefix) * 0.7f;
            float nameW = FontUtils.durman[13].getWidth(name + hpText);
            float itemW = itemText != null ? mc.textRenderer.getWidth(itemText) * 0.7f + 3f : 0f;
            float totalW = friendW + prefixW + nameW + itemW + 8f;

            float x = (float) vec.x - totalW / 2f;
            float y = (float) vec.y - 15f;

            RenderUtil.drawRoundedRect(ms, x, y, totalW, 12f, 1.5f, BG);

            MatrixStack m = e.getDrawContext().getMatrices();
            float tx = x + 4f;

            if (!friendTag.isEmpty()) {
                drawScaled(m, Text.literal(friendTag), tx, y + 3.2f, 0.7f);
                tx += friendW;
            }

            drawScaled(m, prefix, tx, y + 3.2f, 0.7f);
            tx += prefixW;

            FontUtils.durman[13].drawLeftAligned(ms, name + hpText, tx, y + 1.8f, -1);
            tx += nameW + 3f;

            if (itemText != null) {
                drawScaled(m, itemText, tx, y + 3.5f, 0.7f);
            }

            if (effectRender.get()) renderEffects(e, p);
            if (armorRender.get()) renderArmor(e, x + 5f, y, p);
        }
    }

    private void drawScaled(MatrixStack m, Text text, float x, float y, float scale) {
        m.push();
        m.translate(x, y, 0);
        m.scale(scale, scale, 1f);
        mc.textRenderer.draw(text, 0, 0, -1, false, m.peek().getPositionMatrix(),
                mc.getBufferBuilders().getEntityVertexConsumers(), TextRenderer.TextLayerType.NORMAL, 0, 0xF000F0);
        m.pop();
    }

    private void renderEffects(EventRender2D e, PlayerEntity p) {
        Vector3d pos = VectorUtil.toScreen(EntityPosition.get(p, 0f, e.getDeltatick().getTickDelta(true)));
        if (pos.z < 0) return;

        int y = 5;
        for (var eff : p.getStatusEffects()) {
            String name = I18n.translate(eff.getEffectType().value().getName().getString());
            int lvl = eff.getAmplifier() + 1;
            int sec = eff.getDuration() / 20;
            String text = name + (lvl > 1 ? " " + lvl : "") + " | " + sec / 60 + ":" + String.format("%02d", sec % 60);
            FontUtils.durman[14].centeredDraw(e.getDrawContext().getMatrices(), text, (float) pos.x, (float) pos.y + y, -1);
            y += 9;
        }
    }

    private void renderArmor(EventRender2D e, float x, float y, PlayerEntity p) {
        List<ItemStack> items = new ArrayList<>(6);
        items.add(p.getMainHandStack());
        p.getArmorItems().forEach(items::add);
        items.add(p.getOffHandStack());
        items.removeIf(ItemStack::isEmpty);

        float off = 0;
        for (ItemStack stack : items) {
            RenderAddon.renderItem(e.getDrawContext(), stack, x + off - 3f, y - 18f, 0.8f, true);

            if (enchantRender.get() && !stack.getEnchantments().isEmpty()) {
                var enchants = new ArrayList<>(stack.getEnchantments().getEnchantmentEntries());
                enchants.removeIf(en -> ENCHANTS.stream().noneMatch(Enchantment.getName(en.getKey(), en.getIntValue()).getString()::contains));

                if (!enchants.isEmpty()) {
                    int ey = (int) (y - 18f - enchants.size() * 8);
                    MatrixStack m = e.getMatrixStack();

                    for (var en : enchants) {
                        String s = shortEnchant(Enchantment.getName(en.getKey(), en.getIntValue()).getString(), en.getIntValue());
                        m.push();
                        m.translate(x + off, ey, 0);
                        m.scale(0.7f, 0.7f, 1f);
                        FontUtils.durman[14].drawLeftAligned(e.getDrawContext().getMatrices(), s, 0, 0, -1);
                        m.pop();
                        ey += 8;
                    }
                }
            }
            off += 15f;
        }
    }

    private String shortEnchant(String full, int lvl) {
        String[] words = full.split(" ");
        String s = words.length == 1
                ? words[0].substring(0, Math.min(2, words[0].length())).toUpperCase()
                : Arrays.stream(words).filter(w -> !w.isEmpty()).map(w -> String.valueOf(w.charAt(0))).reduce("", String::concat).toUpperCase();
        return s + " " + lvl;
    }

    private void renderItems(EventRender2D e) {
        float delta = e.getDeltatick().getTickDelta(true);

        for (Entity ent : Manager.SYNC_MANAGER.getEntities()) {
            if (!(ent instanceof ItemEntity ie)) continue;

            ItemStack stack = ie.getStack();
            Vector3d vec = VectorUtil.toScreen(EntityPosition.get(ent, 0.6f, delta));
            if (!onScreen(vec)) continue;

            if (shulkerCheck.get() && stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock) {
                renderShulker(e.getDrawContext(), (int) vec.x, (int) vec.y, stack);
            }

            String name = ie.getName().getString();
            int cnt = stack.getCount();
            if (cnt > 1) name += " [x" + Formatting.RED + cnt + Formatting.WHITE + "]";

            float w = FontUtils.sf_bold[15].getWidth(name);
            float h = FontUtils.sf_bold[15].getHeight();
            float x = (float) vec.x - w / 2f - 3f;

            RenderUtil.drawRoundedRect(e.getMatrixStack(), x, (float) vec.y, w + 6f, h + 2f, 1.5f, BG);
            FontUtils.sf_bold[15].centeredDraw(e.getDrawContext().getMatrices(), name, (float) vec.x, (float) vec.y + 0.3f, -1);
        }
    }

    private void renderShulker(DrawContext ctx, int x, int y, ItemStack stack) {
        ContainerComponent cont = stack.get(DataComponentTypes.CONTAINER);
        if (cont == null || cont.copyFirstStack().isEmpty()) return;

        List<ItemStack> items = cont.stream().toList();
        int cols = 9, rows = 3, pad = 8;
        float size = 16f, gap = 0.1f;

        x += pad;
        y -= 82 - 7;

        int w = (int) (cols * size + (cols - 1) * gap + pad * 2);
        int h = (int) (rows * size + (rows - 1) * gap + 14);

        RenderUtil.drawTexture(ctx.getMatrices(), ResourceProvider.container, x - pad, y - 7, w, h, 0, -1);

        for (int i = 0; i < items.size(); i++) {
            float ix = x + (i % cols) * (size + gap);
            float iy = y + (i / cols) * (size + gap);
            RenderAddon.renderItem(ctx, items.get(i), ix, iy, 0.85f, true);
        }
    }
}