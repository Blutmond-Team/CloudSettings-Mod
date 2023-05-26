package de.blutmondgilde.cloudsettings.forge;

import de.blutmondgilde.cloudsettings.CloudSettings;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenOpenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(bus = Bus.FORGE, value = Dist.CLIENT)
public class EventHandler {
    @SubscribeEvent
    public static void onMainMenu(final ScreenOpenEvent e) {
        if (e.getScreen() instanceof TitleScreen) {
            CloudSettings.titleScreenOpened();
        }
    }
}
