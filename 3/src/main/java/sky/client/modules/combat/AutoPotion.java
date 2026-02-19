package sky.client.modules.combat;

import net.minecraft.block.Blocks;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import sky.client.events.Event;
import sky.client.events.impl.EventUpdate;
import sky.client.events.impl.move.EventMotion;
import sky.client.mixin.iface.ClientWorldAccessor;
import sky.client.modules.Function;
import sky.client.modules.FunctionAnnotation;
import sky.client.modules.Type;
import sky.client.modules.setting.BooleanSetting;
import sky.client.modules.setting.MultiSetting;
import sky.client.util.player.TimerUtil;

import java.util.Arrays;

@FunctionAnnotation(name = "AutoPotion", keywords = "AutoBuff", type = Type.Combat, desc = "Автоматически кидает бафы под себя")
public class AutoPotion extends Function {

    private final BooleanSetting autoOff = new BooleanSetting("Авто отключение", false);
    public final MultiSetting potions = new MultiSetting("Бросать",
            Arrays.asList("Силу", "Скорость", "Огнестойкость"),
            new String[]{"Силу", "Скорость", "Огнестойкость"});

    private final TimerUtil timer = new TimerUtil();
    public boolean isActivePotion;
    private boolean spoofed;
    private float prevPitch;

    private enum Pot {
        STRENGTH(StatusEffects.STRENGTH, "Силу"),
        SPEED(StatusEffects.SPEED, "Скорость"),
        FIRE_RES(StatusEffects.FIRE_RESISTANCE, "Огнестойкость");

        final RegistryEntry<StatusEffect> effect;
        final String name;

        Pot(RegistryEntry<StatusEffect> effect, String name) {
            this.effect = effect;
            this.name = name;
        }
    }

    public AutoPotion() {
        addSettings(potions, autoOff);
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof EventMotion e && shouldThrow()) {
            prevPitch = mc.player.getPitch();
            e.setPitch(90);
            spoofed = true;
            isActivePotion = true;
        }

        if (event instanceof EventUpdate) {
            if (isActivePotion && !shouldThrow()) {
                isActivePotion = false;
                if (autoOff.get()) toggle();
            }

            if (spoofed) {
                for (Pot p : Pot.values()) throwPotion(p);

                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
                mc.player.setPitch(prevPitch);
                timer.reset();
                spoofed = false;
                isActivePotion = false;

                if (autoOff.get()) toggle();
            }
        }
    }

    private boolean shouldThrow() {
        if (!mc.player.isOnGround() || !timer.hasTimeElapsed(500)) return false;
        if (mc.world.getBlockState(mc.player.getBlockPos().down()).isOf(Blocks.AIR)) return false;

        if (mc.player.isUsingItem()) {
            ItemStack active = mc.player.getActiveItem();
            if (!active.isOf(Items.SHIELD) && !active.isOf(Items.BOW) && !active.isOf(Items.TRIDENT)) return false;
        }

        for (Pot p : Pot.values()) {
            if (potions.get(p.name) && !mc.player.hasStatusEffect(p.effect) && findSlot(p) != -1) return true;
        }
        return false;
    }

    private int findSlot(Pot type) {
        for (int i = 0; i < 9; i++) {
            ItemStack s = mc.player.getInventory().getStack(i);
            if (!s.isOf(Items.SPLASH_POTION)) continue;

            var contents = s.get(DataComponentTypes.POTION_CONTENTS);
            if (contents != null) {
                for (var eff : contents.getEffects()) {
                    if (eff.getEffectType() == type.effect) return i;
                }
            }
        }
        return -1;
    }

    private void throwPotion(Pot p) {
        if (!potions.get(p.name) || mc.player.hasStatusEffect(p.effect)) return;

        int slot = findSlot(p);
        if (slot == -1) return;

        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        sendSequenced(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
    }

    private void sendSequenced(SequencedPacketCreator creator) {
        if (mc.player.networkHandler == null || mc.world == null) return;
        try (var mgr = ((ClientWorldAccessor) mc.world).getPendingUpdateManager().incrementSequence()) {
            mc.player.networkHandler.sendPacket(creator.predict(mgr.getSequence()));
        }
    }
}