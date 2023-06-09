package de.blutmondgilde.cloudsettings.mixin.forge;

import de.blutmondgilde.cloudsettings.api.CloudSettingsAPI;
import net.minecraft.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Util.class)
public class MixinUtil {
    @Inject(method = "shutdownExecutors", at = @At("HEAD"))
    private static void onShutdownExecutors(CallbackInfo ci) {
        CloudSettingsAPI.shutdown();
    }
}
