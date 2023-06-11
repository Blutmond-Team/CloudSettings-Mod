package de.blutmondgilde.cloudsettings.mixin.forge;

import de.blutmondgilde.cloudsettings.CloudSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;

@Mixin(Options.class)
public abstract class MixinOptions {
    @Shadow
    protected Minecraft minecraft;

    @Shadow
    public abstract File getFile();

    @Inject(method = "load", at = @At("HEAD"))
    public void onLoad(CallbackInfo ci) {
        CloudSettings.beforeOptionsLoaded(minecraft, getFile(), (Options) (Object) this);
    }

    @Inject(method = "save()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;broadcastOptions()V"))
    public void onSave(CallbackInfo ci) {
        CloudSettings.getLogger().info("Detected options saving. Checking for updates...");
        Minecraft.getInstance().execute(() -> CloudSettings.checkForChanges(getFile()));
    }
}
