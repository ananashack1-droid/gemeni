package sky.client.mixin.world;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sky.client.manager.Manager;

@Mixin(Biome.class)
public class MixinBiome {

    @Inject(method = "getPrecipitation", at = @At("HEAD"), cancellable = true)
    public void getPrecipitationHook(BlockPos pos, int i, CallbackInfoReturnable<Biome.Precipitation> cir) {
        if (Manager.FUNCTION_MANAGER == null || Manager.FUNCTION_MANAGER.customWorld == null) return;

        if (Manager.FUNCTION_MANAGER.customWorld.state && Manager.FUNCTION_MANAGER.customWorld.weatherBox.get()) {

            String mode = Manager.FUNCTION_MANAGER.customWorld.weatherMode.get();

            switch (mode) {
                case "Снег":
                    cir.setReturnValue(Biome.Precipitation.SNOW);
                    break;
                case "Дождь":
                case "Гроза":
                    cir.setReturnValue(Biome.Precipitation.RAIN);
                    break;
            }
        }
    }
}