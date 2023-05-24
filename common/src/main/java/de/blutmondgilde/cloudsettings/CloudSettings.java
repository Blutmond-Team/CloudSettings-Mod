package de.blutmondgilde.cloudsettings;

import de.blutmondgilde.cloudsettings.api.CloudSettingsAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CloudSettings {
    public static final String MOD_ID = "cloudsettings";
    private static IPlatformHandler platformHandler;
    private static boolean initialized = false;
    private static final ConcurrentHashMap<String, String> CACHE = new ConcurrentHashMap<>();
    private static final Executor executor = Executors.newSingleThreadExecutor();

    public static void init(IPlatformHandler handler) {
        platformHandler = handler;
    }

    public static User getUser() {
        return Minecraft.getInstance().getUser();
    }

    public static void titleScreenOpened() {
        if (initialized) return;
        platformHandler.getLogger().info("Requesting User Data");
        CloudSettingsAPI.getStoredOptions().thenAccept(options -> {
            if (options.length == 0) return;
            platformHandler.getOptionsFile().getParentFile().mkdirs();
            try {
                FileUtils.write(platformHandler.getOptionsFile(), String.join("\n", options), StandardCharsets.UTF_8, false);
                initialized = true;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                // Reload Options
                Minecraft.getInstance().options.load();
                platformHandler.getLogger().info("{} options loaded from cloud.", options.length);
                // Cache settings
                checkForChanges();
            }
        });
    }

    public static void checkForChanges() {
        if (!initialized) return;
        executor.execute(() -> {
            Set<String> changes = new HashSet<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(platformHandler.getOptionsFile()))) {
                String line = reader.readLine();
                while (line != null) {
                    String id = line.substring(0, line.indexOf(':'));
                    if (CACHE.put(id, line) != null) {
                        changes.add(line);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            String[] changedOptions = changes.stream().toArray(String[]::new);
            CloudSettingsAPI.storeSettings(changedOptions);
        });
    }
}