package de.blutmondgilde.cloudsettings.fabric;

import de.blutmondgilde.cloudsettings.CloudSettings;
import de.blutmondgilde.cloudsettings.IPlatformHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screens.TitleScreen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CloudSettingsFabric implements ModInitializer, IPlatformHandler {
    public static final Logger LOGGER = LogManager.getLogger("CloudSettings");

    @Override
    public void onInitialize() {
        CloudSettings.init(this);
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof TitleScreen) {
                CloudSettings.titleScreenOpened();
            } else {
                LOGGER.info("Screen: {}", screen.getClass());
            }
        });
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }
}