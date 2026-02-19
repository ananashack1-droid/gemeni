package sky.client.modules.movement;

import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import sky.client.events.impl.EventUpdate;
import sky.client.modules.setting.ModeSetting;
import sky.client.modules.setting.SliderSetting;
import sky.client.events.Event;
import sky.client.events.impl.move.EventMotion;
import sky.client.modules.Function;
import sky.client.modules.FunctionAnnotation;
import sky.client.modules.Type;
import sky.client.util.move.MoveUtil;
import sky.client.util.player.InventoryUtil;
import sky.client.util.player.TimerUtil;

@FunctionAnnotation(name = "Flight", desc = "", type = Type.Move)
public class Flight extends Function {
    private final ModeSetting mode = new ModeSetting("Тип", "Motion", "Motion","ElytraRWOld");
    private final SliderSetting xspeed = new SliderSetting("X - Скорость", 1f, 0.0f, 5f, 0.1f);
    private final SliderSetting yspeed = new SliderSetting("Y - Скорость", 1f, 0.0f, 5f, 0.1f);

    public Flight() {
        addSettings(mode, xspeed, yspeed);
    }
    private final TimerUtil timerUtil = new TimerUtil(), swapTimer = new TimerUtil();
    int item = -1;

    @Override
    public void onEvent(Event event) {
        if (mode.is("Motion")) {
            if (event instanceof EventMotion) {
                double y = 0.0;
                if (mc.options.jumpKey.isPressed()) {
                    y = yspeed.get().floatValue();
                } else if (mc.options.sneakKey.isPressed()) {
                    y = -yspeed.get().floatValue();
                }
                mc.player.setVelocity(0, y, 0);
                if (mc.options.sprintKey.isPressed()) {
                    MoveUtil.setMotion(xspeed.get().floatValue());
                }
            }
        }
        if (mode.is("ElytraRWOld")) {
            if (event instanceof EventUpdate) {
                for (int i = 0; i < 9; ++i) {
                    if (mc.player.getInventory().getStack(i).isOf(Items.ELYTRA) && !mc.player.isOnGround() && !mc.player.isSubmergedInWater() && !mc.player.isInLava() && !mc.player.isGliding()) {
                        int swapDelay = 520;
                        if (timerUtil.hasTimeElapsed(swapDelay)) {
                            swapTimer.reset();
                            InventoryUtil.swapSlotsUniversal(6, i, false, false);
                            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                            mc.player.startGliding();
                            InventoryUtil.swapSlotsUniversal(6, i, false, false);
                            item = i;
                            timerUtil.reset();
                        }

                        if (mc.player.isGliding()) {
                            InventoryUtil.inventorySwapClick2(Items.FIREWORK_ROCKET,true, false);
                        }
                    }
                }
            }
        }
    }
}