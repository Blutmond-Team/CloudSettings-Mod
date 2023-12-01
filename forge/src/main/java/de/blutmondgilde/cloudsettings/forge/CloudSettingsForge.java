package de.blutmondgilde.cloudsettings.forge;

import de.blutmondgilde.cloudsettings.CloudSettings;
import de.blutmondgilde.cloudsettings.IPlatformHandler;
import net.minecraftforge.fml.common.Mod;

@Mod(CloudSettings.MOD_ID)
public class CloudSettingsForge implements IPlatformHandler {
    public CloudSettingsForge() {
        CloudSettings.init(this);
    }
}
