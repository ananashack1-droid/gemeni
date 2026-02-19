package sky.client.mixin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.MinecraftVersion;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sky.client.Main;
import sky.client.manager.ClientManager;
import sky.client.manager.IMinecraft;
import sky.client.manager.Manager;

@Environment(EnvType.CLIENT)
@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient implements IMinecraft {
    @Inject(method = "isMultiplayerEnabled", at = @At("HEAD"), cancellable = true)
    private void isMultiplayerEnabled(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }

    @Inject(method = "getWindowTitle", at = @At("HEAD"), cancellable = true)
    private void getWindowTitle(CallbackInfoReturnable<String> cir) {
        if (!ClientManager.legitMode) {
            cir.setReturnValue(Main.getInstance().name + " " + MinecraftVersion.CURRENT.getName() + " | " + Manager.USER_PROFILE.getName());
        }
    }
    @Inject(at = @At("HEAD"), method = "stop")
    private void stop(CallbackInfo ci) {
        Main.getInstance().shutDown();
    }
    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(CallbackInfo callbackInfo) {
        Main.getInstance().init();
    }
}
