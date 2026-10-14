package com.mrcrayfish.mightymail;

import com.mrcrayfish.framework.api.datagen.FrameworkModelProvider;
import com.mrcrayfish.mightymail.datagen.*;
import com.mrcrayfish.mightymail.wood.MailboxWoods;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootSubProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.item.crafting.Recipe;

import java.util.concurrent.CompletableFuture;

public class DataGeneration implements DataGeneratorEntrypoint
{
    @Override
    @SuppressWarnings({"UnstableApiUsage", "unchecked"})
    public void onInitializeDataGenerator(FabricDataGenerator generator)
    {
        FabricDataGenerator.Pack pack = generator.createPack();
        pack.addProvider(Recipes::new);
        pack.addProvider(Loot::new);
        pack.addProvider(BlockTagGen::new);
        pack.addProvider((FabricDataGenerator.Pack.Factory<FrameworkModelProvider>) output ->
            new FrameworkModelProvider(output, BlockStatesGen::new, ItemModelsGen::new));
        for(String modId : MailboxWoods.compatMods())
        {
            pack.addProvider((FabricDataGenerator.Pack.Factory<CompatPackGen>) output -> new CompatPackGen(output, modId));
        }
    }

    // Recipes and loot tables are registry entries since 26.x; Fabric API wraps the vanilla bootstrap classes
    private static class Recipes extends FabricRecipeProvider
    {
        private Recipes(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries)
        {
            super(output, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider provider, BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements)
        {
            return new RecipeGen(recipes, advancements);
        }

        @Override
        public String getName()
        {
            return "Mighty Mail Recipes";
        }
    }

    private static class Loot extends FabricBlockLootSubProvider
    {
        private Loot(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries)
        {
            super(output, registries);
        }

        @Override
        public void generate()
        {
            LootTableGen.forEachBlock(this::dropSelf);
        }
    }
}
