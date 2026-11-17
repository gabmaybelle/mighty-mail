package com.mrcrayfish.mightymail.datagen;

import com.mrcrayfish.mightymail.core.ModBlocks;
import com.mrcrayfish.mightymail.wood.MailboxWoods;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;

import java.util.concurrent.CompletableFuture;

public class BlockTagGen extends IntrinsicHolderTagsProvider<Block>
{
    public BlockTagGen(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider)
    {
        super(output, Registries.BLOCK, lookupProvider, block -> block.builtInRegistryHolder().key());
    }

    @Override
    protected void addTags(HolderLookup.Provider provider)
    {
        var axe = this.tag(BlockTags.MINEABLE_WITH_AXE);
        MailboxWoods.blocks().forEach(axe::add);

        this.tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(ModBlocks.POST_BOX.get());

        this.tag(BlockTags.NEEDS_STONE_TOOL)
                .add(ModBlocks.POST_BOX.get());
    }
}
