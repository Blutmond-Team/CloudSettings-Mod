package de.blutmondgilde.cloudsettings;

import com.google.common.collect.Sets;
import de.blutmondgilde.cloudsettings.api.CloudSettingsAPI;
import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;
import lombok.extern.log4j.Log4j2;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.User;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Log4j2(topic = "CloudSettings")
public class CloudSettings {
    public static final String MOD_ID = "cloudsettings";
    public static final String MOD_VERSION = "2.0.0.4";
    @Getter
    private static IPlatformHandler platformHandler;
    @Getter(onMethod_ = {@Synchronized})
    @Setter(onMethod_ = {@Synchronized})
    private static CloudSettingsStatus status = CloudSettingsStatus.BEFORE_START;
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

    public static void beforeOptionsLoaded(Minecraft minecraft, File optionFile, Options optionsObj) {
        if (getStatus().isInitialized()) return;
        getLogger().info("Requesting User Data");
        try {
            Set<String> options = Sets.newHashSet(CloudSettingsAPI.getStoredOptions().get());
            getLogger().info("Got {} options from Cloud", options.size());
            if (options.size() != 0) {
                if (!optionFile.exists()) {
                    getLogger().debug("Save vanilla config file");
                    optionsObj.save();
                }

                if (!optionFile.exists()) {
                    getLogger().error("Vanilla config file still doesn't exist after forced save!");
                    setStatus(CloudSettingsStatus.FAILED);
                    return;
                }

                try {
                    BufferedReader reader = new BufferedReader(new FileReader(optionFile));
                    StringBuilder optionLines = new StringBuilder();

                    String line;
                    while ((line = reader.readLine()) != null) {
                        String optionId = line.substring(0, line.indexOf(':'));
                        String newOptionLine = options.stream()
                                .filter(s -> s.startsWith(optionId))
                                .findFirst()
                                .orElse(line);
                        if (options.remove(newOptionLine)) {
                            getLogger().debug("Updated {} with value {} to options", optionId, newOptionLine);
                        }

                        optionLines.append(newOptionLine).append('\n');
                    }
                    reader.close();
                    getLogger().debug("Option Updating complete. Applying {} remaining Options", options.size());

                    for (String cloudOption : options) {
                        String optionId = cloudOption.substring(0, cloudOption.indexOf(':'));
                        getLogger().debug("Applied {} with value {} to options", optionId, cloudOption);
                        optionLines.append(cloudOption).append('\n');
                    }

                    getLogger().debug("Options applied. Writing option file...");
                    FileUtils.write(optionFile, optionLines.toString(), StandardCharsets.UTF_8, false);
                    getLogger().debug("Option file written");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                getLogger().debug("{} modified. Waiting for minecraft to load file.", optionFile.getName());
                setStatus(CloudSettingsStatus.INITIALIZED);
                // Cache settings
                checkForChanges(optionFile);
            } else {
                // Init new user
                setStatus(CloudSettingsStatus.INITIALIZED);
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            setStatus(CloudSettingsStatus.FAILED);
        }
    }

    public static void checkForChanges(File optionsFile) {
        if (!getStatus().isInitialized()) {
            getLogger().debug("Skipping change check due to uninitialized base handler");
            return;
        }

        if (getStatus().isErrored()) {
            getLogger().info("CloudSettings is disabled due to load up errors.");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(optionsFile))) {
            String line = reader.readLine();
            while (line != null) {
                String id = line.substring(0, line.indexOf(':'));
                if (CACHE.containsKey(id) && !CACHE.get(id).equalsIgnoreCase(line)) {
                    PendingChanges.put(id, line);
                    getLogger().info("Enqueue sync of {} with value {}", id, line);
                }
                CACHE.put(id, line);
                line = reader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        getLogger().info("Got {} changes to sync", PendingChanges.size());
    }

    public static Logger getLogger() {
        return log;
    }
}