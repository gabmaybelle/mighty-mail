package com.mrcrayfish.mightymail;

import com.mrcrayfish.framework.api.datagen.FrameworkModelProvider;
import com.mrcrayfish.mightymail.client.ClientHandler;
import com.mrcrayfish.mightymail.datagen.*;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.Set;

@Mod(Constants.MOD_ID)
public class MightyMail
{
    public MightyMail(IEventBus bus)
    {
        bus.addListener(this::onCommonSetup);
        bus.addListener(this::onClientSetup);
        bus.addListener(this::onGatherData);
    }

    private void onCommonSetup(FMLCommonSetupEvent event)
    {
        event.enqueueWork(Bootstrap::init);
    }

    private void onClientSetup(FMLClientSetupEvent event)
    {
        event.enqueueWork(ClientHandler::setup);
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
    }
}