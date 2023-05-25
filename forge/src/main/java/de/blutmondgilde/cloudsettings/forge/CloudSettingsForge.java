package de.blutmondgilde.cloudsettings.forge;

import de.blutmondgilde.cloudsettings.CloudSettings;
import de.blutmondgilde.cloudsettings.IPlatformHandler;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(CloudSettings.MOD_ID)
public class CloudSettingsForge implements IPlatformHandler {
    public static final Logger LOGGER = LogManager.getLogger("CloudSettings");

    public CloudSettingsForge() {
        CloudSettings.init(this);
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }
}