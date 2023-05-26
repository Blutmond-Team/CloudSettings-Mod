package de.blutmondgilde.cloudsettings.mixin.forge;

import de.blutmondgilde.cloudsettings.CloudSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Overlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {
    @Inject(method = "setOverlay", at = @At(value = "HEAD"))
    public void beforeTitleScreen(Overlay overlay, CallbackInfo ci) {
        CloudSettings.beforeTitleScreen();
    }
}
