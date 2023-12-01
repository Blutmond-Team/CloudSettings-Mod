package de.blutmondgilde.cloudsettings.neoforge;

import de.blutmondgilde.cloudsettings.CloudSettings;
import de.blutmondgilde.cloudsettings.IPlatformHandler;
import net.neoforged.fml.common.Mod;

@Mod(CloudSettings.MOD_ID)
public class CloudSettingsNeoForge implements IPlatformHandler {
    public CloudSettingsNeoForge() {
        CloudSettings.init(this);
    }
}
