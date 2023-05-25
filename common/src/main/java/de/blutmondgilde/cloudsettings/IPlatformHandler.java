package de.blutmondgilde.cloudsettings;

import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.Logger;

import java.io.File;

public interface IPlatformHandler {
    Logger getLogger();

    default File getOptionsFile() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("options.txt").toFile();
    }

    String getModVersion();
}
