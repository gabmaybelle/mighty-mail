package com.mrcrayfish.mightymail.client;

import com.mrcrayfish.mightymail.Constants;
import com.mrcrayfish.mightymail.client.gui.screen.PostBoxScreen;
import com.mrcrayfish.mightymail.core.ModMenuTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Author: MrCrayfish
 */
@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public class ClientHandler
{
    public static void setup()
    {
        // Since 26.x the chunk layer (cutout) is chosen from the model's texture transparency; no registration needed
    }

    @SubscribeEvent
    private static void onRegisterMenuScreens(RegisterMenuScreensEvent event)
    {
        event.register(ModMenuTypes.POST_BOX.get(), PostBoxScreen::new);
    }
}
