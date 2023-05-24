package de.blutmondgilde.cloudsettings.forge;

import de.blutmondgilde.cloudsettings.CloudSettings;
import de.blutmondgilde.cloudsettings.IPlatformHandler;
import me.shedaniel.architectury.platform.forge.EventBuses;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(CloudSettings.MOD_ID)
public class CloudSettingsForge implements IPlatformHandler {
    public static final Logger LOGGER = LogManager.getLogger("CloudSettings");

    public CloudSettingsForge() {
        // Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(CloudSettings.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        CloudSettings.init(this);
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }
}