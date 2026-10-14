package com.mrcrayfish.mightymail.client;

import com.mrcrayfish.mightymail.client.gui.screen.PostBoxScreen;
import com.mrcrayfish.mightymail.core.ModMenuTypes;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

/**
 * Author: MrCrayfish
 */
public class ClientHandler implements ClientModInitializer
{
    @Override
    public void onInitializeClient()
    {
        MenuScreens.register(ModMenuTypes.POST_BOX.get(), PostBoxScreen::new);
        // Since 26.x the chunk layer (cutout) is chosen from the model's texture transparency; no registration needed
    }
}
