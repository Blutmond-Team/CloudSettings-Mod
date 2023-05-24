package de.blutmondgilde.cloudsettings;

import de.blutmondgilde.cloudsettings.api.CloudSettingsAPI;
import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CloudSettings {
    public static final String MOD_ID = "cloudsettings";
    @Getter
    private static IPlatformHandler platformHandler;
    @Getter(onMethod_ = {@Synchronized})
    @Setter(onMethod_ = {@Synchronized})
    private static boolean initialized = false;
    private static final ConcurrentHashMap<String, String> CACHE = new ConcurrentHashMap<>();
    @Getter
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void init(IPlatformHandler handler) {
        platformHandler = handler;
    }

    public static User getUser() {
        return Minecraft.getInstance().getUser();
    }

    public static void titleScreenOpened() {
        if (isInitialized()) return;
        Minecraft.getInstance().execute(() -> {
            platformHandler.getLogger().info("Requesting User Data");
            try {
                String[] options = CloudSettingsAPI.getStoredOptions().get();
                platformHandler.getLogger().info("Got {} options from Cloud", options.length);
                if (options.length != 0) {
                    if (platformHandler.getOptionsFile().getParentFile().mkdirs()) {
                        platformHandler.getLogger().info("Created Game dir");
                    }

                    try {
                        FileUtils.write(platformHandler.getOptionsFile(), String.join("\n", options), StandardCharsets.UTF_8, false);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // Reload Options
                    Minecraft.getInstance().options.load();
                    platformHandler.getLogger().info("{} options loaded from cloud.", options.length);
                }

                setInitialized(true);
                // Cache settings
                checkForChanges();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
    }

    public static void checkForChanges() {
        if (!isInitialized()) {
            platformHandler.getLogger().info("Skipping change check due to uninitialized base handler");
            return;
        }

        Set<String> changes = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(platformHandler.getOptionsFile()))) {
            String line = reader.readLine();
            while (line != null) {
                String id = line.substring(0, line.indexOf(':'));
                if (CACHE.containsKey(id) && !CACHE.get(id).equalsIgnoreCase(line)) {
                    changes.add(line);
                    platformHandler.getLogger().info("Enqueue sync of {} with value {}", id, line);
                }
                CACHE.put(id, line);
                line = reader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        platformHandler.getLogger().info("Got {} changes to sync", changes.size());
        String[] changedOptions = changes.stream().toArray(String[]::new);
        CloudSettingsAPI.storeSettings(changedOptions);
    }
}