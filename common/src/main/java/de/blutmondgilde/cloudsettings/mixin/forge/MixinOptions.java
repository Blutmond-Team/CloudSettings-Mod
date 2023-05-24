package de.blutmondgilde.cloudsettings.mixin.forge;

import de.blutmondgilde.cloudsettings.CloudSettings;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Options.class)
public class MixinOptions {
    @Inject(method = "save", at = @At("RETURN"))
    private void onSave(CallbackInfo ci) {
        CloudSettings.checkForChanges();
    }
}
