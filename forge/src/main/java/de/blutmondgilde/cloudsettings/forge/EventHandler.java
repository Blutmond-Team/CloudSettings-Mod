package de.blutmondgilde.cloudsettings.forge;

import de.blutmondgilde.cloudsettings.CloudSettings;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(bus = Bus.FORGE, value = Dist.CLIENT)
public class EventHandler {
    @SubscribeEvent
    public static void onMainMenu(final ScreenEvent.Opening e) {
        if (e.getScreen() instanceof TitleScreen) {
            CloudSettings.titleScreenOpened();
        }
    }
}
