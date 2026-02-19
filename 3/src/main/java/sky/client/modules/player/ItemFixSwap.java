package sky.client.modules.player;

import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import sky.client.events.Event;
import sky.client.events.impl.EventPacket;
import sky.client.modules.Function;
import sky.client.modules.FunctionAnnotation;
import sky.client.modules.Type;

@FunctionAnnotation(
        name = "ItemFixSwap",
        keywords = {"NoSlotChange", "NoServerDesync", "СлотФиксер"},
        desc = "Убирает переключение слота от античита",
        type = Type.Player)
public class ItemFixSwap extends Function {
    public ItemFixSwap() {
        addSettings();
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof EventPacket e) {
            if (e.isReceivePacket()) {
               if (e.getPacket() instanceof UpdateSelectedSlotC2SPacket packetHeldItemChange) {
                   event.setCancel(true);
               }
            }
        }
    }
}
