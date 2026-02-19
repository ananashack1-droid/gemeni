package sky.client.modules.combat;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PlayerHeadItem;
import sky.client.events.Event;
import sky.client.events.impl.EventUpdate;
import sky.client.events.impl.move.EventEntitySpawn;
import sky.client.manager.Manager;
import sky.client.modules.Function;
import sky.client.modules.FunctionAnnotation;
import sky.client.modules.Type;
import sky.client.modules.setting.BooleanSetting;
import sky.client.modules.setting.MultiSetting;
import sky.client.modules.setting.SliderSetting;
import sky.client.util.player.InventoryUtil;

import java.util.Arrays;

@FunctionAnnotation(name = "AutoTotem", desc = "Берёт в руки тотем при определённом здоровье", type = Type.Combat)
public class AutoTotem extends Function {

    private final MultiSetting mode = new MultiSetting("Брать если",
            Arrays.asList("Кристалл", "Игрок с булавой"),
            new String[]{"Кристалл", "Игрок с булавой", "Рядом крипер", "Обсидиан", "Якорь", "Падение", "Вагонетка"});

    public final SliderSetting hp = new SliderSetting("Здоровье", 4.5f, 2f, 20f, 0.1f);
    private final SliderSetting hpElytra = new SliderSetting("Брать раньше на элитрах", 5, 2, 6, 1);
    private final BooleanSetting back = new BooleanSetting("Возвращать предмет", true);
    private final BooleanSetting noBall = new BooleanSetting("Не брать если шар", false);
    private final BooleanSetting saveEnchanted = new BooleanSetting("Сохранять чаренные тотемы", true);
    private final BooleanSetting absorb = new BooleanSetting("+ Золотые сердца", false);

    private final SliderSetting crystalDist = new SliderSetting("До кристалла", 4, 2, 6, 1, () -> mode.get("Кристалл"));
    private final SliderSetting anchorDist = new SliderSetting("До якоря", 4, 2, 6, 1, () -> mode.get("Якорь"));
    private final SliderSetting minecartDist = new SliderSetting("До Вагонетки", 4, 2, 8, 1, () -> mode.get("Вагонетка"));
    private final SliderSetting obsidianDist = new SliderSetting("До Обсидиана", 4, 2, 8, 1, () -> mode.get("Обсидиан"));

    private int savedSlot = -1;

    public AutoTotem() {
        addSettings(mode, hp, hpElytra, back, noBall, saveEnchanted, absorb, crystalDist, anchorDist, minecartDist, obsidianDist);
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof EventEntitySpawn e) {
            Entity ent = e.getEntity();
            float dist = mc.player != null ? ent.distanceTo(mc.player) : Float.MAX_VALUE;

            if (mode.get("Кристалл") && ent instanceof EndCrystalEntity && dist <= crystalDist.get().floatValue()) {
                forceTotem();
            }
            if (mode.get("Вагонетка") && ent instanceof TntMinecartEntity && dist <= minecartDist.get().floatValue()) {
                forceTotem();
            }
        }

        if (event instanceof EventUpdate) {
            ItemStack offhand = mc.player.getOffHandStack();
            boolean hasTotem = offhand.isOf(Items.TOTEM_OF_UNDYING);

            if (shouldEquip()) {
                int slot = getTotemSlot();
                if (slot == -1) return;

                if (saveEnchanted.get() && hasTotem && offhand.hasEnchantments()) {
                    ItemStack candidate = mc.player.getInventory().getStack(slot);
                    if (candidate.isOf(Items.TOTEM_OF_UNDYING) && !candidate.hasEnchantments()) {
                        swap(slot);
                        return;
                    }
                }

                if (!hasTotem) swap(slot);
            } else if (savedSlot != -1 && back.get()) {
                swap(savedSlot);
                savedSlot = -1;
            }
        }
    }

    private void swap(int slot) {
        InventoryUtil.swapSlotsUniversal(slot, 40, false, true);
        if (savedSlot == -1) savedSlot = slot;
    }

    private void forceTotem() {
        if (!mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
            int slot = getTotemSlot();
            if (slot != -1) swap(slot);
        }
    }

    private int getTotemSlot() {
        ItemStack offhand = mc.player.getOffHandStack();

        if (saveEnchanted.get()) {
            if (offhand.isOf(Items.TOTEM_OF_UNDYING) && offhand.hasEnchantments()) {
                return findTotem(false);
            }
            int normal = findTotem(false);
            return normal != -1 ? normal : findTotem(true);
        }

        return InventoryUtil.getItemSlot(Items.TOTEM_OF_UNDYING);
    }

    private int findTotem(boolean enchanted) {
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack s = mc.player.getInventory().getStack(i);
            if (s.isOf(Items.TOTEM_OF_UNDYING) && s.hasEnchantments() == enchanted) return i;
        }
        return -1;
    }

    private boolean shouldEquip() {
        float absorption = absorb.get() && mc.player.hasStatusEffect(StatusEffects.ABSORPTION)
                ? mc.player.getAbsorptionAmount() : 0f;
        float threshold = hp.get().floatValue();

        if (mc.player.getHealth() + absorption <= threshold) return true;

        boolean hasBall = noBall.get() && mc.player.getOffHandStack().getItem() instanceof PlayerHeadItem
                && !(mode.get("Якорь") && mc.player.fallDistance > 5f);

        if (!hasBall) {
            if (checkDanger()) return true;
        }

        if (mc.player.getInventory().armor.get(2).isOf(Items.ELYTRA)
                && mc.player.getHealth() <= threshold + hpElytra.get().floatValue()) return true;

        return mode.get("Падение") && !mc.player.isGliding() && mc.player.fallDistance > 10f;
    }

    private boolean checkDanger() {
        for (Entity e : Manager.SYNC_MANAGER.getEntities()) {
            float dist = mc.player.distanceTo(e);

            if (mode.get("Кристалл") && e instanceof EndCrystalEntity && dist < crystalDist.get().floatValue()) return true;
            if (mode.get("Вагонетка") && e instanceof TntMinecartEntity && dist < minecartDist.get().floatValue()) return true;
            if (mode.get("Рядом крипер") && e instanceof CreeperEntity c && dist < 5f && c.getClientFuseTime(0f) > 0f) return true;
        }

        if (mode.get("Игрок с булавой")) {
            for (PlayerEntity p : Manager.SYNC_MANAGER.getPlayers()) {
                if (p == mc.player) continue;
                if (!p.getMainHandStack().isOf(Items.MACE)) continue;

                double dy = p.getY() - mc.player.getY();
                double yVel = p.getVelocity().y;
                boolean inAir = !p.isOnGround() && !p.isTouchingWater() && !p.isClimbing();

                if (dy > 1.5 && inAir && Math.abs(yVel) > 0.1 && p.distanceTo(mc.player) < 24) return true;
            }
        }

        if (mode.get("Якорь") && InventoryUtil.TotemUtil.getBlock(anchorDist.get().floatValue(), Blocks.RESPAWN_ANCHOR) != null) return true;
        if (mode.get("Обсидиан") && InventoryUtil.TotemUtil.getBlock(obsidianDist.get().floatValue(), Blocks.OBSIDIAN) != null) return true;

        return false;
    }

    @Override
    protected void onEnable() { savedSlot = -1; }

    @Override
    protected void onDisable() { savedSlot = -1; }
}