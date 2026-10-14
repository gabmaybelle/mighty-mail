package com.mrcrayfish.mightymail.datagen;

import com.mrcrayfish.mightymail.core.ModBlocks;
import com.mrcrayfish.mightymail.wood.MailboxWood;
import com.mrcrayfish.mightymail.wood.MailboxWoods;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
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
}
