package sky.client.modules.player;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.EntityHitResult;
import sky.client.events.Event;
import sky.client.events.impl.input.EventKey;
import sky.client.events.impl.input.EventMouse;
import sky.client.manager.ClientManager;
import sky.client.manager.Manager;
import sky.client.modules.Function;
import sky.client.modules.FunctionAnnotation;
import sky.client.modules.Type;
import sky.client.modules.setting.BindSetting;
import sky.client.modules.setting.BooleanSetting;
import sky.client.modules.setting.ModeSetting;
import sky.client.util.player.InventoryUtil;

@FunctionAnnotation(name = "MiddleClick", keywords = {"MCP", "MCF", "MiddleClick"}, desc = "Действия на колёсико мыши (Пёрл или Друзья)", type = Type.Player)
public class MiddleClick extends Function {
    private final ModeSetting action = new ModeSetting("Действие", "Пёрл", "Пёрл", "Друзья");

    private final ModeSetting pearlMode = new ModeSetting(() -> action.is("Пёрл"), "Тип Pearl", "Обычный", "Обычный", "По бинду");
    private final BindSetting pearlBind = new BindSetting("Кнопка Pearl", 0, () -> action.is("Пёрл") && pearlMode.is("По бинду"));
    private final BooleanSetting inventoryUse = new BooleanSetting("Использовать из инвентаря", true, "Не используйте на HollyWorld (баниться)", () -> action.is("Пёрл"));

    public MiddleClick() {
        addSettings(action, pearlMode, pearlBind, inventoryUse);
    }

    @Override
    public void onEvent(Event event) {
        if (action.is("Друзья")) {
            if (event instanceof EventMouse mouse && mouse.getButton() == 2) {
                handleFriend();
            }
        }

        else if (action.is("Пёрл")) {
            if (pearlMode.is("Обычный")) {
                if (event instanceof EventMouse mouseTick && mouseTick.getButton() == 2) {
                    throwPearl();
                }
            }
            if (pearlMode.is("По бинду")) {
                if (event instanceof EventKey e && e.key == pearlBind.getKey()) {
                    throwPearl();
                }
            }
        }
    }

    private void throwPearl() {
        if (!mc.player.getItemCooldownManager().isCoolingDown(Items.ENDER_PEARL.getDefaultStack())) {
            if (Manager.FUNCTION_MANAGER.attackAura.target != null) {
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround(), false));
            }
            InventoryUtil.inventorySwapClick2(Items.ENDER_PEARL, inventoryUse.get(), true);
        }
    }

    private void handleFriend() {
        if (mc.crosshairTarget instanceof EntityHitResult entityHitResult) {
            if (entityHitResult.getEntity() instanceof PlayerEntity player) {
                final String name = player.getName().getString();
                if (Manager.FRIEND_MANAGER.isFriend(name)) {
                    Manager.FRIEND_MANAGER.removeFriend(name);
                    ClientManager.message(Formatting.GRAY + name + Formatting.RED + " удалён из друзей");
                } else {
                    Manager.FRIEND_MANAGER.addFriend(name);
                    ClientManager.message(Formatting.GRAY + name + Formatting.GREEN + " добавлен в друзья");
                }
            }
        }
    }
}