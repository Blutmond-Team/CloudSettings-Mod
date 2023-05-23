package de.blutmondgilde.cloudsettings.forge;

import me.shedaniel.architectury.platform.forge.EventBuses;
import de.blutmondgilde.cloudsettings.CloudSettings;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CloudSettings.MOD_ID)
public class CloudSettingsForge {
    public CloudSettingsForge() {
		// Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(CloudSettings.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
            CloudSettings.init();
    }
}