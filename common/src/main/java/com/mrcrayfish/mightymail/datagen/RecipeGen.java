package com.mrcrayfish.mightymail.datagen;

import com.mrcrayfish.mightymail.core.ModBlocks;
import com.mrcrayfish.mightymail.wood.MailboxWood;
import com.mrcrayfish.mightymail.wood.MailboxWoods;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.concurrent.CompletableFuture;

public class RecipeGen extends RecipeProvider
{
    public RecipeGen(HolderLookup.Provider provider, RecipeOutput output)
    {
        super(provider, output);
    }

    @Override
    public void buildRecipes()
    {
        for(MailboxWood wood : MailboxWoods.VANILLA)
        {
            this.mailbox(MailboxWoods.block(wood.id()), BuiltInRegistries.BLOCK.getValue(wood.planks()), BuiltInRegistries.BLOCK.getValue(wood.fence()));
        }

        shaped(RecipeCategory.DECORATIONS, ModBlocks.POST_BOX.get())
            .pattern("III")
            .pattern("ICI")
            .pattern("I I")
            .define('I', Items.IRON_INGOT)
            .define('C', Items.CHEST)
            .unlockedBy("has_ingot", has(Items.IRON_INGOT))
            .unlockedBy("has_chest", has(Items.CHEST))
            .save(this.output);
    }

    private void mailbox(ItemLike item, Block plank, Block fence)
    {
        shaped(RecipeCategory.DECORATIONS, item)
            .pattern("PCP")
            .pattern("PPP")
            .pattern(" F ")
            .define('P', plank)
            .define('C', Blocks.CHEST)
            .define('F', fence)
            .unlockedBy("has_plank", has(plank))
            .unlockedBy("has_chest", has(Items.CHEST))
            .unlockedBy("has_fence", has(fence))
            .save(this.output);
    }

    public static final class Runner extends RecipeProvider.Runner
    {
        public Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider)
        {
            super(output, lookupProvider);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider provider, RecipeOutput output)
        {
            return new RecipeGen(provider, output);
        }

        @Override
        public String getName()
        {
            return "Mighty Mail Recipes";
        }
    }
}
