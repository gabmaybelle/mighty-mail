package com.mrcrayfish.mightymail;

import com.mrcrayfish.framework.FrameworkSetup;
import com.mrcrayfish.mightymail.datagen.CompatPackGen;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import com.mrcrayfish.mightymail.wood.MailboxWoods;
import net.fabricmc.api.ModInitializer;

public class MightyMail implements ModInitializer
{
    @Override
    public void onInitialize()
    {
        // Mailboxes go first: Framework's block entity type lists them when it registers
        MailboxWoods.ensureRegistered();
        FrameworkSetup.run();
        Bootstrap.init();
        // The mailboxes of other wood mods live in a built-in pack each, enabled only when that mod is present
        FabricLoader.getInstance().getModContainer(Constants.MOD_ID).ifPresent(container -> {
            for(String modId : MailboxWoods.compatMods())
            {
                if(FabricLoader.getInstance().isModLoaded(modId))
                {
                    ResourceManagerHelper.registerBuiltinResourcePack(CompatPackGen.packId(modId), container,
                        Component.literal("Mighty Mail: " + modId), ResourcePackActivationType.ALWAYS_ENABLED);
                }
            }
        });
    }
}
