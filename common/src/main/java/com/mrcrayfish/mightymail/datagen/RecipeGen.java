package com.mrcrayfish.mightymail.datagen;

import com.mrcrayfish.mightymail.core.ModBlocks;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.MultiRegistryBootstrap;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Set;

public class RecipeGen extends RecipeProvider
{
    public RecipeGen(BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements)
    {
        super(recipes, advancements);
    }

    /**
     * Recipes are registry entries since 26.x, so they are generated as part of the reloadable
     * registry bootstrap (see {@link RegistriesGen}) rather than by a standalone data provider.
     */
    public static MultiRegistryBootstrap create()
    {
        return new MultiRegistryBootstrap()
        {
            @Override
            public Set<ResourceKey<? extends Registry<?>>> requestedRegistries()
            {
                return Set.of(Registries.RECIPE, Registries.ADVANCEMENT);
            }

            @Override
            public void run(BootstrapGetter registries)
            {
                new RecipeGen(registries.get(Registries.RECIPE), registries.get(Registries.ADVANCEMENT)).buildRecipes();
            }
        };
    }

    @Override
    public void buildRecipes()
    {
        this.mailbox(ModBlocks.MAIL_BOX_OAK.get(), Blocks.OAK_PLANKS, Blocks.OAK_FENCE);
        this.mailbox(ModBlocks.MAIL_BOX_SPRUCE.get(), Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_FENCE);
        this.mailbox(ModBlocks.MAIL_BOX_BIRCH.get(), Blocks.BIRCH_PLANKS, Blocks.BIRCH_FENCE);
        this.mailbox(ModBlocks.MAIL_BOX_JUNGLE.get(), Blocks.JUNGLE_PLANKS, Blocks.JUNGLE_FENCE);
        this.mailbox(ModBlocks.MAIL_BOX_ACACIA.get(), Blocks.ACACIA_PLANKS, Blocks.ACACIA_FENCE);
        this.mailbox(ModBlocks.MAIL_BOX_DARK_OAK.get(), Blocks.DARK_OAK_PLANKS, Blocks.DARK_OAK_FENCE);
        this.mailbox(ModBlocks.MAIL_BOX_MANGROVE.get(), Blocks.MANGROVE_PLANKS, Blocks.MANGROVE_FENCE);
        this.mailbox(ModBlocks.MAIL_BOX_CHERRY.get(), Blocks.CHERRY_PLANKS, Blocks.CHERRY_FENCE);
        this.mailbox(ModBlocks.MAIL_BOX_CRIMSON.get(), Blocks.CRIMSON_PLANKS, Blocks.CRIMSON_FENCE);
        this.mailbox(ModBlocks.MAIL_BOX_WARPED.get(), Blocks.WARPED_PLANKS, Blocks.WARPED_FENCE);
        this.mailbox(ModBlocks.MAIL_BOX_PALE_OAK.get(), Blocks.PALE_OAK_PLANKS, Blocks.PALE_OAK_FENCE);

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
}
