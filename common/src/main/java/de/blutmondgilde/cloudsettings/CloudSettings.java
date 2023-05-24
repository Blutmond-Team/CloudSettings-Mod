package de.blutmondgilde.cloudsettings;

import de.blutmondgilde.cloudsettings.api.CloudSettingsAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class CloudSettings {
    public static final String MOD_ID = "cloudsettings";
    private static IPlatformHandler platformHandler;
    private static boolean initialized = false;

    public static void init(IPlatformHandler handler) {
        platformHandler = handler;
    }

    public static User getUser() {
        return Minecraft.getInstance().getUser();
    }

    public static void titleScreenOpened() {
        if (initialized) return;
        platformHandler.getLogger().info("Requesting User Data");
        CloudSettingsAPI.getStoredOptions().thenAccept(strings -> {
            if (strings.length == 0) return;
            platformHandler.getOptionsFile().getParentFile().mkdirs();
            try {
                FileUtils.write(platformHandler.getOptionsFile(), String.join("\n", strings), StandardCharsets.UTF_8, false);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                // Reload Options
                Minecraft.getInstance().options.load();
                platformHandler.getLogger().info("{} options loaded from cloud.", strings.length);
            }
        });
    }
}