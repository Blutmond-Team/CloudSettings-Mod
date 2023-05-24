package de.blutmondgilde.cloudsettings.mixin.forge;

import de.blutmondgilde.cloudsettings.CloudSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Options.class)
public class MixinOptions {
    @Inject(method = "save()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;broadcastOptions()V"))
    public void onSave(CallbackInfo ci) {
        CloudSettings.getPlatformHandler().getLogger().info("Detected options saving. Checking for updates...");
        Minecraft.getInstance().execute(CloudSettings::checkForChanges);
    }
}
