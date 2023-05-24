package de.blutmondgilde.cloudsettings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.User;

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
        platformHandler.getLogger().info("Title Screen!");
    }
}