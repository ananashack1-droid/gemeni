package sky.client.modules.player;

import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import sky.client.modules.setting.BooleanSetting;
import sky.client.modules.setting.TextSetting;
import sky.client.events.Event;
import sky.client.events.impl.EventPacket;
import sky.client.events.impl.EventUpdate;
import sky.client.modules.Function;
import sky.client.modules.FunctionAnnotation;
import sky.client.modules.Type;

@FunctionAnnotation(name = "AutoRespawn", desc = "Автоматически респавнит вас при смерти", type = Type.Player)
public class AutoRespawn extends Function {
    private BooleanSetting autohome = new BooleanSetting("Автоматически телепортироваться домой",true);
    private TextSetting home = new TextSetting("Название точки дома","home",() -> autohome.get());

    public AutoRespawn() {
        addSettings(autohome,home);
    }
    @Override
    public void onEvent(Event event) {
        if (event instanceof EventPacket eventPacket) {
            if (eventPacket.getPacket() instanceof GameMessageS2CPacket packet) {
                String message = packet.content().getString();

                if (message.contains("Вы были убиты") || message.contains("убиты") || message.contains("потеряно монет") || message.contains("потеряно") || message.contains("насмерть")) {
                    if (autohome.get()) {
                        mc.player.networkHandler.sendChatMessage("/home " + home.getValue());;
                    }
                }
            }
        }

        if (event instanceof EventUpdate eventUpdate) {
            if (mc.currentScreen instanceof DeathScreen) {
                mc.player.requestRespawn();
                mc.setScreen(null);
            }
        }
    }
}
