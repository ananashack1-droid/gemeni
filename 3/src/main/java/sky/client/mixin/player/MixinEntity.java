package sky.client.mixin.player;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import sky.client.manager.IMinecraft;
import sky.client.manager.Manager;
import sky.client.modules.combat.*;
import sky.client.modules.combat.*;
import sky.client.modules.combat.rotation.RotationController;
import sky.client.modules.movement.freelook.CameraOverriddenEntity;
import sky.client.modules.movement.freelook.FreeLookState;
import sky.client.modules.player.NoPush;
import sky.client.modules.render.Trails;
import sky.client.util.IEntity;
import sky.client.util.player.AuraUtil;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("All")
@Mixin(Entity.class)
public abstract class MixinEntity implements IEntity, CameraOverriddenEntity, IMinecraft {

    @Unique
    private float cameraYaw;
    @Unique
    private float cameraPitch;

    @Unique
    private List<Trails.Trail> trails = new ArrayList<>();
    @Unique
    private Vec3d lastTrailPos;

    @Shadow
    private Box boundingBox;

    @Shadow
    protected static Vec3d movementInputToVelocity(Vec3d movementInput, float speed, float yaw) {
        double d = movementInput.lengthSquared();
        if (d < 1.0E-7) {
            return Vec3d.ZERO;
        } else {
            Vec3d vec3d = (d > 1.0F ? movementInput.normalize() : movementInput).multiply(speed);
            float sin = MathHelper.sin(yaw * ((float) Math.PI / 180F));
            float cos = MathHelper.cos(yaw * ((float) Math.PI / 180F));
            return new Vec3d(vec3d.x * cos - vec3d.z * sin, vec3d.y, vec3d.z * cos + vec3d.x * sin);
        }
    }

    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void onChangeLookDirection(double deltaX, double deltaY, CallbackInfo ci) {
        Entity self = (Entity)(Object)this;
        if (FreeLookState.active && self instanceof ClientPlayerEntity) {
            cameraYaw += (float) deltaX * 0.15F;
            cameraPitch = MathHelper.clamp(cameraPitch + (float) deltaY * 0.15F, -90.0F, 90.0F);
            ci.cancel();
        }
    }

    @Override
    public float getCameraPitch() {
        return cameraPitch;
    }

    @Override
    public float getCameraYaw() {
        return cameraYaw;
    }

    @Override
    public void setCameraPitch(float pitch) {
        cameraPitch = pitch;
    }

    @Override
    public void setCameraYaw(float yaw) {
        cameraYaw = yaw;
    }

    @ModifyArgs(method = "pushAwayFrom", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;addVelocity(DDD)V"))
    private void pushAwayFromHook(Args args) {
        Entity self = (Entity)(Object)this;
        NoPush noPush = Manager.FUNCTION_MANAGER.noPush;
        if (self == mc.player && noPush.state && noPush.mods.get("Игроки")) {
            args.set(0, 0d);
            args.set(1, 0d);
            args.set(2, 0d);
        }
    }

    @Override
    public List<Trails.Trail> getTrails() {
        return trails;
    }

    @Override
    public Vec3d getLastTrailPos() {
        return lastTrailPos;
    }

    @Override
    public void setLastTrailPos(Vec3d pos) {
        this.lastTrailPos = pos;
    }

    @ModifyExpressionValue(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isControlledByPlayer()Z"))
    private boolean fixFallDistanceCalculation(boolean original) {
        Entity self = (Entity)(Object)this;
        return self != mc.player && original;
    }

    @Inject(method = "getBoundingBox", at = @At("RETURN"), cancellable = true)
    private void getBoundingBox(CallbackInfoReturnable<Box> cir) {
        Entity self = (Entity)(Object)this;
        HitBox hitBox = Manager.FUNCTION_MANAGER.hitBox;
        if (hitBox.state && mc != null && mc.player != null && self.getId() != mc.player.getId()) {
            float halfSize = hitBox.size.get().floatValue() / 2f;
            Box expanded = new Box(boundingBox.minX - halfSize, boundingBox.minY - halfSize, boundingBox.minZ - halfSize, boundingBox.maxX + halfSize, boundingBox.maxY + halfSize, boundingBox.maxZ + halfSize);
            cir.setReturnValue(expanded);
        }
    }
    // Вставлять внутрь MixinEntity
    @Inject(method = "isInsideWall", at = @At("HEAD"), cancellable = true)
    private void onIsInsideWall(CallbackInfoReturnable<Boolean> cir) {
        if (Manager.FUNCTION_MANAGER.phase.isState()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "checkBlockCollision", at = @At("HEAD"), cancellable = true)
    private void onCheckBlockCollision(CallbackInfo ci) {
        if (Manager.FUNCTION_MANAGER.phase.isState()) {
            ci.cancel();
        }
    }

    @Inject(method = "updateVelocity", at = @At("HEAD"), cancellable = true)
    private void onUpdateVelocity(float speed, Vec3d movementInput, CallbackInfo ci) {
        Entity self = (Entity)(Object)this;
        if (self != mc.player) return;

        Vec3d customVelocity = null;

        AttackAura attackAura = Manager.FUNCTION_MANAGER.attackAura;
        AutoExplosion autoExplosion = Manager.FUNCTION_MANAGER.autoExplosion;
        RotationController rotationController = Manager.ROTATION;

        boolean correcting = (attackAura.state && attackAura.correction.get()) || rotationController.isControlling();
        if (correcting) {
            float yaw = rotationController.getYaw();
            customVelocity = movementInputToVelocity(movementInput, speed, yaw);
        }
        else if (autoExplosion.check()) {
            customVelocity = movementInputToVelocity(movementInput, speed, autoExplosion.serverRot.x);
        }

        if (customVelocity != null) {
            self.setVelocity(self.getVelocity().add(customVelocity));
            ci.cancel();
        }
    }

}
