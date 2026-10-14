package com.mrcrayfish.mightymail;

import com.mrcrayfish.framework.api.datagen.FrameworkModelProvider;
import com.mrcrayfish.mightymail.client.ClientHandler;
import com.mrcrayfish.mightymail.datagen.*;
import com.mrcrayfish.mightymail.platform.NeoForgePlatformHelper;
import com.mrcrayfish.mightymail.wood.MailboxWoods;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.fml.ModList;

import java.util.Set;

@Mod(Constants.MOD_ID)
public class MightyMail
{
    public MightyMail(IEventBus bus)
    {
        // Mailboxes are registered through deferred registers; the blocks exist before Framework's block entity type lists them
        MailboxWoods.ensureRegistered();
        NeoForgePlatformHelper.register(bus);
        bus.addListener(this::onCommonSetup);
        bus.addListener(this::onClientSetup);
        bus.addListener(this::onGatherData);
        bus.addListener(this::onAddPackFinders);
    }

    private void onCommonSetup(FMLCommonSetupEvent event)
    {
        event.enqueueWork(Bootstrap::init);
    }

    private void onClientSetup(FMLClientSetupEvent event)
    {
        event.enqueueWork(ClientHandler::setup);
    }

    /** The mailboxes of other wood mods live in a built-in pack each, enabled only when that mod is present */
    private void onAddPackFinders(AddPackFindersEvent event)
    {
        for(String modId : MailboxWoods.compatMods())
        {
            if(ModList.get().isLoaded(modId))
            {
                event.addPackFinders(Identifier.fromNamespaceAndPath(Constants.MOD_ID, CompatPackGen.packFolder(modId)),
                    event.getPackType(), Component.literal("Mighty Mail: " + modId), PackSource.BUILT_IN, true, Pack.Position.TOP);
            }
        }
    }

    @SuppressWarnings({"UnstableApiUsage"})
    private void onGatherData(GatherDataEvent.Client event)
    {
        event.createProvider(BlockTagGen::new);

        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        // Recipes and loot tables are registry entries since 26.x, generated from the reloadable registry set
        event.addProvider(DatapackBuiltinEntriesProvider.forReloadableLayer(output, Constants.MOD_ID, event.getWorldLookupProvider(), event.getReloadableLookupProvider(), RegistriesGen.RELOADABLE, Set.of(Constants.MOD_ID)));
        event.addProvider(new FrameworkModelProvider(output, BlockStatesGen::new, ItemModelsGen::new));
        for(String modId : MailboxWoods.compatMods())
        {
            event.addProvider(new CompatPackGen(output, modId));
        }
    }
}