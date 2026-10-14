package com.mrcrayfish.mightymail.datagen;

import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;

/**
 * Everything this mod adds to the reloadable registry layer (data packs): loot tables and recipes.
 * Each loader's data generation entry point turns this into the json files.
 */
public class RegistriesGen
{
    public static final RegistrySetBuilder RELOADABLE = new RegistrySetBuilder()
        .add(Registries.LOOT_TABLE, LootTableGen.create())
        .add(RecipeGen.create());
}
