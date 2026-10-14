package com.mrcrayfish.mightymail.datagen;

import com.mrcrayfish.mightymail.core.ModBlocks;
import com.mrcrayfish.mightymail.wood.MailboxWoods;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;

import java.util.concurrent.CompletableFuture;

public class BlockTagGen extends TagsProvider<Block>
{
    public BlockTagGen(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider)
    {
        super(output, Registries.BLOCK, lookupProvider);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider)
    {
        TagAppender<Block> axe = this.tag(BlockTags.MINEABLE_WITH_AXE);
        MailboxWoods.blocks().forEach(block -> axe.add(key(block)));

        this.tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(key(ModBlocks.POST_BOX.get()));

        this.tag(BlockTags.NEEDS_STONE_TOOL)
                .add(key(ModBlocks.POST_BOX.get()));
    }

    // 26.3 tag appenders take registry keys rather than block instances
    private static ResourceKey<Block> key(Block block)
    {
        return block.builtInRegistryHolder().key();
    }
}
