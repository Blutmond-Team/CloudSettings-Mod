package de.blutmondgilde.cloudsettings.fabric;

import de.blutmondgilde.cloudsettings.CloudSettings;
import de.blutmondgilde.cloudsettings.IPlatformHandler;
import net.fabricmc.api.ClientModInitializer;

public class CloudSettingsFabric implements ClientModInitializer, IPlatformHandler {
    @Override
    public void onInitializeClient() {
        CloudSettings.init(this);
    }
}