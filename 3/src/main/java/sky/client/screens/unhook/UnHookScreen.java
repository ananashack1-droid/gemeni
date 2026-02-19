package sky.client.screens.unhook;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import sky.client.manager.ClientManager;
import sky.client.manager.IMinecraft;
import sky.client.manager.Manager;
import sky.client.modules.misc.UnHook;

public class UnHookScreen extends Screen implements IMinecraft {
    private final UnHook unHookFunction;

    public UnHookScreen() {
        super(Text.literal("UnHook"));
        unHookFunction = Manager.FUNCTION_MANAGER.unHook;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        handleTimers();
    }

    private void handleTimers() {
        ClientManager.legitMode = true;
        unHookFunction.onUnhook();
        mc.setScreen(null);
    }
}
