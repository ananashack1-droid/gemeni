package sky.client.mixin.player;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sky.client.events.Event;
import sky.client.events.impl.EventUpdate;
import sky.client.manager.IMinecraft;
import sky.client.manager.Manager;

@Mixin(ClientPlayerEntity.class)
public abstract class MixinClientPlayerEntity implements IMinecraft {

    @Shadow
    public abstract void move(MovementType type, Vec3d movement);

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickHead(CallbackInfo ci) {
        Event.call(new EventUpdate());
    }

    @Inject(method = "pushOutOfBlocks", at = @At("HEAD"), cancellable = true)
    private void onPushOutOfBlocksHook(double x, double d, CallbackInfo ci) {
        // Прямое обращение к переменной модуля в твоем менеджере
        if (Manager.FUNCTION_MANAGER.phase.isState()) {
            ci.cancel();
        }
    }

    @Inject(method = "isCamera", at = @At("HEAD"), cancellable = true)
    private void onIsCamera(CallbackInfoReturnable<Boolean> cir) {
        if (Manager.FUNCTION_MANAGER.phase.isState()) {
            cir.setReturnValue(true);
        }
    }

    @ModifyExpressionValue(method = "tickMovement", at = @At(value = "FIELD", target = "Lnet/minecraft/client/network/ClientPlayerEntity;horizontalCollision:Z"))
    private boolean hookHorizontalCollision(boolean original) {
        if (Manager.FUNCTION_MANAGER.phase.isState()) {
            return false;
        }
        return original;
    }
}