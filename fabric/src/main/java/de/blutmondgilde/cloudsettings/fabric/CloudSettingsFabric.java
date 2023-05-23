package de.blutmondgilde.cloudsettings.fabric;

import de.blutmondgilde.cloudsettings.CloudSettings;
import net.fabricmc.api.ModInitializer;

public class CloudSettingsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        CloudSettings.init();
    }
}