package de.blutmondgilde.cloudsettings.fabric;

import de.blutmondgilde.cloudsettings.CloudSettings;
import de.blutmondgilde.cloudsettings.IPlatformHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screens.TitleScreen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CloudSettingsFabric implements ClientModInitializer, IPlatformHandler {
    public static final Logger LOGGER = LogManager.getLogger("CloudSettings");

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    @Override
    public void onInitializeClient() {
        CloudSettings.init(this);
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof TitleScreen) {
                CloudSettings.titleScreenOpened();
            }
        });
    }
}