package com.mrcrayfish.mightymail.datagen;

import com.mrcrayfish.mightymail.core.ModBlocks;
import com.mrcrayfish.mightymail.wood.MailboxWoods;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class LootTableGen extends LootTableProvider
{
    public LootTableGen(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider)
    {
        super(output, Set.of(), List.of(new SubProviderEntry(Blocks::new, LootContextParamSets.BLOCK)), lookupProvider);
    }

    /**
     * Every block of this mod drops itself; the loaders' own loot providers reuse this list.
     */
    public static void forEachBlock(Consumer<Block> consumer)
    {
        MailboxWoods.blocks().forEach(consumer);
        consumer.accept(ModBlocks.POST_BOX.get());
    }

    private static class Blocks extends BlockLootSubProvider
    {
        private Blocks(HolderLookup.Provider provider)
        {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags(), provider);
        }

        @Override
        public void generate() {}

        /**
         * The vanilla implementation checks every registered block for a loot table, which fails on
         * vanilla blocks, so the tables for this mod's blocks are handed to the consumer directly.
         */
        @Override
        public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> consumer)
        {
            forEachBlock(block -> consumer.accept(block.getLootTable().orElseThrow(), this.createSingleItemTable(block)));
        }
    }
}
