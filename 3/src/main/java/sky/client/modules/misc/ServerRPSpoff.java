package sky.client.modules.misc;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket;
import net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket;
import sky.client.events.Event;
import sky.client.events.impl.EventPacket;
import sky.client.modules.Function;
import sky.client.modules.FunctionAnnotation;
import sky.client.modules.Type;

@FunctionAnnotation(name = "ServerRPSpoff", desc = "", type = Type.Misc)
public class ServerRPSpoff extends Function {


    @Override
    public void onEvent(Event event) {
        if (event instanceof EventPacket packetEvent) {
            if (packetEvent.getPacket() instanceof ResourcePackSendS2CPacket packet) {
                ClientPlayNetworkHandler networkHandler = mc.getNetworkHandler();
                if (networkHandler != null) {
                    networkHandler.sendPacket(new ResourcePackStatusC2SPacket(packet.id(), ResourcePackStatusC2SPacket.Status.ACCEPTED));
                    networkHandler.sendPacket(new ResourcePackStatusC2SPacket(packet.id(), ResourcePackStatusC2SPacket.Status.SUCCESSFULLY_LOADED));
                }

                event.setCancel(true);
            }
        }
    }
}