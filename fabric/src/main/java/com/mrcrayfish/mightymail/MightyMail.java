package com.mrcrayfish.mightymail;

import com.mrcrayfish.framework.FrameworkSetup;
import com.mrcrayfish.mightymail.datagen.CompatPackGen;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
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
                    ResourceLoader.registerBuiltinPack(CompatPackGen.packId(modId), container,
                        Component.literal("Mighty Mail: " + modId), PackActivationType.ALWAYS_ENABLED);
                }
            }
        });
    }
}
