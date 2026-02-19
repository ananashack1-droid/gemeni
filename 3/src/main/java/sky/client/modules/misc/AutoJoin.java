package sky.client.modules.misc;

import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.screen.slot.SlotActionType;
import sky.client.events.Event;
import sky.client.events.impl.EventUpdate;
import sky.client.modules.Function;
import sky.client.modules.FunctionAnnotation;
import sky.client.modules.Type;
import sky.client.modules.setting.SliderSetting;
import sky.client.util.math.MathUtil;
import sky.client.util.player.TimerUtil;

@FunctionAnnotation(
        name = "AutoJoin",
        desc = "Автоматически кликает по слоту для входа на сервер",
        type = Type.Misc
)
public class AutoJoin extends Function {

    private final SliderSetting targetSlot =
            new SliderSetting("Target Slot", 1, 1, 54, 1);

    private final SliderSetting minDelay =
            new SliderSetting("Min Delay", 400, 100, 10000, 1);

    private final SliderSetting maxDelay =
            new SliderSetting("Max Delay", 600, 100, 10000, 1);

    private final TimerUtil timer = new TimerUtil();

    private long nextDelay;

    public AutoJoin() {
        addSettings(targetSlot, minDelay, maxDelay);
    }

    @Override
    protected void onEnable() {
        timer.reset();
        generateNextDelay();
    }

    @Override
    public void onEvent(Event event) {
        if (!(event instanceof EventUpdate)) return;
        if (mc.player == null || mc.currentScreen == null) return;

        // суффикс в списке модов
        this.name = "AutoJoin §7[" + targetSlot.get().intValue() + "]";

        // ❗ В ТВОЕЙ ВЕРСИИ GenericContainerScreen НЕ GENERIC
        if (!(mc.currentScreen instanceof GenericContainerScreen screen)) return;

        int slot = targetSlot.get().intValue() - 1;

        if (slot < 0 || slot >= screen.getScreenHandler().slots.size()) return;

        if (!timer.hasTimeElapsed(nextDelay)) return;

        mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                slot,
                0,
                SlotActionType.PICKUP,
                mc.player
        );

        timer.reset();
        generateNextDelay();
    }

    private void generateNextDelay() {
        double min = minDelay.get().doubleValue();
        double max = maxDelay.get().doubleValue();

        if (min > max) {
            double temp = min;
            min = max;
            max = temp;
        }

        nextDelay = (long) MathUtil.random(min, max);
    }
}
