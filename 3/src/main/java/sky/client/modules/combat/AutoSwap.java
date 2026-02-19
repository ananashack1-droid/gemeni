package sky.client.modules.combat;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.*;
import net.minecraft.screen.slot.SlotActionType;
import sky.client.events.Event;
import sky.client.events.impl.input.EventKey;
import sky.client.modules.Function;
import sky.client.modules.FunctionAnnotation;
import sky.client.modules.Type;
import sky.client.modules.setting.BindSetting;
import sky.client.modules.setting.BooleanSetting;
import sky.client.modules.setting.ModeSetting;
import sky.client.util.player.InventoryUtil;
import sky.client.util.player.TimerUtil;

@FunctionAnnotation(name = "AutoSwap", type = Type.Combat, desc = "Позволяет менять предметы по бинду")
public class AutoSwap extends Function {

    private static final Item[] ITEMS = {
            Items.SHIELD,
            Items.GOLDEN_APPLE,
            Items.TOTEM_OF_UNDYING,
            Items.PLAYER_HEAD,
            Items.FIREWORK_ROCKET
    };

    private final ModeSetting firstItem = new ModeSetting("Первый предмет", "Щит",
            "Щит", "Яблоко", "Тотем", "Шар", "Фейерверк");
    private final ModeSetting secondItem = new ModeSetting("Второй предмет", "Щит",
            "Щит", "Яблоко", "Тотем", "Шар", "Фейерверк");

    private final BindSetting swapKey = new BindSetting("Кнопка смены предмета", 0);
    private final BooleanSetting swapWeapons = new BooleanSetting("Менять топор и меч", false);
    private final BooleanSetting bypass = new BooleanSetting("Обход FT/HW", false);

    private final TimerUtil timer = new TimerUtil();
    private boolean bypassActive;
    private int pendingSlot = -1;
    private boolean wasSprinting = false;

    public AutoSwap() {
        addSettings(swapKey, firstItem, secondItem, swapWeapons, bypass);
    }

    @Override
    public void onEvent(Event event) {
        if (!(event instanceof EventKey e)) return;

        if (e.key == swapKey.getKey()) {
            if (mc.player == null || mc.player.getOffHandStack() == null) return;

            int firstIndex = Math.min(Math.max(firstItem.getIndex(), 0), ITEMS.length - 1);
            int secondIndex = Math.min(Math.max(secondItem.getIndex(), 0), ITEMS.length - 1);

            Item currentItem = mc.player.getOffHandStack().getItem();
            Item targetItem;

            if (currentItem == ITEMS[firstIndex]) {
                targetItem = ITEMS[secondIndex];
            } else {
                targetItem = ITEMS[firstIndex];
            }

            int slot = findItem(targetItem);
            if (slot == -1) return;

            if (bypass.get()) {
                wasSprinting = mc.options.sprintKey.isPressed();
                timer.reset();
                bypassActive = true;
                pendingSlot = slot;
                releaseMovementKeys();
            } else {
                doSwap(slot);
            }
        }

        if (bypassActive) {
            if (pendingSlot != -1 && timer.hasTimeElapsed(90)) {
                doSwap(pendingSlot);
                pendingSlot = -1;
            }

            if (timer.hasTimeElapsed(150)) {
                bypassActive = false;
                pendingSlot = -1;
                restoreMovementKeys();
            }
        }
    }

    private void releaseMovementKeys() {
        if (mc.options == null) return;

        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.sprintKey.setPressed(false);
    }

    private void restoreMovementKeys() {
        if (mc.options == null) return;

        mc.options.sprintKey.setPressed(wasSprinting);
    }

    private void doSwap(int slot) {
        if (mc.interactionManager == null || mc.player == null) return;

        try {
            int slotId = slot;
            if (slot >= 0 && slot < 9) {
                slotId = slot + 36;
            }

            mc.interactionManager.clickSlot(0, slotId, 40, SlotActionType.SWAP, mc.player);

            if (swapWeapons.get()) {
                swapWeapons();
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    private int findItem(Item item) {
        if (mc.player == null || mc.player.getInventory() == null) return -1;

        // Для головы используем специальную логику
        boolean isHead = isHeadItem(item);

        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            if (i == 40) continue;

            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;

            if (isHead) {
                // Ищем любую голову/череп
                if (isHeadItem(stack.getItem())) {
                    return i;
                }
            } else {
                // Обычный предмет
                if (stack.getItem() == item) {
                    return i;
                }
            }
        }
        return -1;
    }

    // Определяем, является ли предмет головой/черепом
    private boolean isHeadItem(Item item) {
        // Список всех известных голов в Minecraft 1.21.4
        return item == Items.PLAYER_HEAD ||
                item == Items.SKELETON_SKULL ||
                item == Items.ZOMBIE_HEAD ||
                item == Items.CREEPER_HEAD ||
                item == Items.DRAGON_HEAD ||
                item == Items.PIGLIN_HEAD ||
                // Дополнительная проверка по названию класса
                item.getClass().getSimpleName().toLowerCase().contains("skull") ||
                item.getClass().getSimpleName().toLowerCase().contains("head");
    }

    private void swapWeapons() {
        if (mc.player == null) return;

        try {
            int sword = findItemByClass(SwordItem.class);
            int axe = findItemByClass(AxeItem.class);

            if (sword != -1 && axe != -1) {
                swapSlotsDirectly(sword, axe);
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    private int findItemByClass(Class<?> itemClass) {
        if (mc.player == null || mc.player.getInventory() == null) return -1;

        for (int i = 0; i < 45; i++) {
            if (i == 40) continue;

            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && itemClass.isInstance(stack.getItem())) {
                return i;
            }
        }

        return -1;
    }

    private void swapSlotsDirectly(int slot1, int slot2) {
        if (mc.interactionManager == null || mc.player == null) return;
        if (slot1 == slot2) return;

        try {
            int containerSlot1 = (slot1 < 9) ? slot1 + 36 : slot1;
            int containerSlot2 = (slot2 < 9) ? slot2 + 36 : slot2;

            mc.interactionManager.clickSlot(0, containerSlot1, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(0, containerSlot2, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(0, containerSlot1, 0, SlotActionType.PICKUP, mc.player);
        } catch (Exception e) {
            // Ignore
        }
    }
}