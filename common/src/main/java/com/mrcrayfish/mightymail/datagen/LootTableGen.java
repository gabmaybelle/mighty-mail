package com.mrcrayfish.mightymail.datagen;

import com.mrcrayfish.mightymail.core.ModBlocks;
import net.minecraft.core.registries.SingleRegistryBootstrap;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class LootTableGen
{
    /**
     * Loot tables are registry entries since 26.x, so they are generated as part of the reloadable
     * registry bootstrap (see {@link RegistriesGen}) rather than by a standalone data provider.
     */
    public static SingleRegistryBootstrap<LootTable> create()
    {
        return new LootTableProvider(Set.of(), List.of(new LootTableProvider.SubProviderEntry(Blocks::new, LootContextParamSets.BLOCK)));
    }

    /**
     * Every block of this mod drops itself; the loaders' own loot providers reuse this list.
     */
    public static void forEachBlock(Consumer<Block> consumer)
    {
        consumer.accept(ModBlocks.MAIL_BOX_OAK.get());
        consumer.accept(ModBlocks.MAIL_BOX_SPRUCE.get());
        consumer.accept(ModBlocks.MAIL_BOX_BIRCH.get());
        consumer.accept(ModBlocks.MAIL_BOX_JUNGLE.get());
        consumer.accept(ModBlocks.MAIL_BOX_ACACIA.get());
        consumer.accept(ModBlocks.MAIL_BOX_DARK_OAK.get());
        consumer.accept(ModBlocks.MAIL_BOX_MANGROVE.get());
        consumer.accept(ModBlocks.MAIL_BOX_CHERRY.get());
        consumer.accept(ModBlocks.MAIL_BOX_CRIMSON.get());
        consumer.accept(ModBlocks.MAIL_BOX_WARPED.get());
        consumer.accept(ModBlocks.MAIL_BOX_PALE_OAK.get());
        consumer.accept(ModBlocks.POST_BOX.get());
    }

    private static class Blocks extends BlockLootSubProvider
    {
        private Blocks(LootTableSubProvider.Context context)
        {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags(), context);
        }

        @Override
        public void generate() {}

        /**
         * The vanilla implementation checks every registered block for a loot table, which fails on
         * vanilla blocks, so the tables for this mod's blocks are handed to the output directly.
         */
        @Override
        public void run()
        {
            forEachBlock(block -> this.output.accept(block.getLootTable().orElseThrow(), this.createSingleItemTable(block)));
        }
    }
}
