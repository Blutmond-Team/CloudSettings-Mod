package de.blutmondgilde.cloudsettings;

import com.google.common.collect.Sets;
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
    private static final ConcurrentHashMap<String, String> PendingChanges = new ConcurrentHashMap<>();
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
                Set<String> options = Sets.newHashSet(CloudSettingsAPI.getStoredOptions().get());
                platformHandler.getLogger().info("Got {} options from Cloud", options.size());
                if (options.size() != 0) {
                    if (!platformHandler.getOptionsFile().exists()) {
                        platformHandler.getLogger().debug("Save vanilla config file");
                        Minecraft.getInstance().options.save();
                    }

                    try {
                        BufferedReader reader = new BufferedReader(new FileReader(platformHandler.getOptionsFile()));
                        StringBuilder optionLines = new StringBuilder();

                        String line;
                        while ((line = reader.readLine()) != null) {
                            String optionId = line.substring(0, line.indexOf(':'));
                            String newOptionLine = options.stream()
                                    .filter(s -> s.startsWith(optionId))
                                    .findFirst()
                                    .orElse(line);
                            if (options.remove(newOptionLine)) {
                                platformHandler.getLogger().debug("Updated {} with value {} to options", optionId, newOptionLine);
                            }

                            optionLines.append(newOptionLine).append('\n');
                        }
                        reader.close();
                        platformHandler.getLogger().debug("Option Updating complete. Applying {} remaining Options", options.size());

                        for (String cloudOption : options) {
                            String optionId = cloudOption.substring(0, cloudOption.indexOf(':'));
                            platformHandler.getLogger().debug("Applied {} with value {} to options", optionId, cloudOption);
                            optionLines.append(cloudOption).append('\n');
                        }

                        platformHandler.getLogger().debug("Options applied. Writing option file...");
                        FileUtils.write(platformHandler.getOptionsFile(), optionLines.toString(), StandardCharsets.UTF_8, false);
                        platformHandler.getLogger().debug("Option file written");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    platformHandler.getLogger().debug("Loading newly written option file");
                    // Reload Options
                    Minecraft.getInstance().options.load();
                    platformHandler.getLogger().debug("Loaded option file.");
                    setInitialized(true);
                    // Cache settings
                    checkForChanges();
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
    }

    public static void checkForChanges() {
        if (!isInitialized()) {
            platformHandler.getLogger().debug("Skipping change check due to uninitialized base handler");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(platformHandler.getOptionsFile()))) {
            String line = reader.readLine();
            while (line != null) {
                String id = line.substring(0, line.indexOf(':'));
                if (CACHE.containsKey(id) && !CACHE.get(id).equalsIgnoreCase(line)) {
                    PendingChanges.put(id, line);
                    platformHandler.getLogger().info("Enqueue sync of {} with value {}", id, line);
                }
                CACHE.put(id, line);
                line = reader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        platformHandler.getLogger().info("Got {} changes to sync", PendingChanges.size());
    }
}