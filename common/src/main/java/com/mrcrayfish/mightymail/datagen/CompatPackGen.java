package com.mrcrayfish.mightymail.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mrcrayfish.mightymail.Constants;
import com.mrcrayfish.mightymail.wood.MailboxWood;
import com.mrcrayfish.mightymail.wood.MailboxWoods;
import net.minecraft.SharedConstants;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackFormat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Writes one built-in resource and data pack for the mailboxes of another wood mod, at
 * {@code resourcepacks/compat_<mod>} inside this jar. The loaders enable that pack only when the mod is
 * present, so a missing mod never produces missing-texture or unknown-item errors.
 * <p>
 * The blocks of those woods are not registered while data generation runs (the other mod isn't there),
 * so everything here is written as plain JSON from the table, in the same shape the vanilla providers
 * produce for the vanilla woods. If a vanilla format changes, compare with the generated oak files.
 */
public class CompatPackGen implements DataProvider
{
    private final String modId;
    private final List<MailboxWood> woods;
    private final Path root;

    public CompatPackGen(PackOutput parent, String modId)
    {
        this.modId = modId;
        this.woods = MailboxWoods.COMPAT.stream().filter(wood -> modId.equals(wood.requiredMod())).toList();
        this.root = parent.getOutputFolder().resolve(packFolder(modId));
    }

    /** The pack's folder inside the jar, relative to the resources root. */
    public static String packFolder(String modId)
    {
        return "resourcepacks/compat_" + modId;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache)
    {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        futures.add(DataProvider.saveStable(cache, this.packMeta(), this.root.resolve("pack.mcmeta")));
        JsonObject lang = new JsonObject();
        JsonArray axe = new JsonArray();
        for(MailboxWood wood : this.woods)
        {
            String path = wood.blockId().getPath();
            String blockModel = Constants.MOD_ID + ":block/" + path;
            Path assets = this.root.resolve("assets/" + Constants.MOD_ID);
            Path data = this.root.resolve("data/" + Constants.MOD_ID);
            futures.add(DataProvider.saveStable(cache, this.blockState(blockModel), assets.resolve("blockstates/" + path + ".json")));
            futures.add(DataProvider.saveStable(cache, this.model("mail_box_planks", wood), assets.resolve("models/block/" + path + ".json")));
            futures.add(DataProvider.saveStable(cache, this.model("mail_box_planks_unchecked", wood), assets.resolve("models/block/" + path + "_unchecked.json")));
            futures.add(DataProvider.saveStable(cache, this.itemDefinition(blockModel), assets.resolve("items/" + path + ".json")));
            futures.add(DataProvider.saveStable(cache, this.lootTable(wood), data.resolve("loot_table/blocks/" + path + ".json")));
            futures.add(DataProvider.saveStable(cache, this.recipe(wood), data.resolve("recipe/" + path + ".json")));
            futures.add(DataProvider.saveStable(cache, this.recipeAdvancement(wood), data.resolve("advancement/recipes/decorations/" + path + ".json")));
            lang.addProperty("block." + Constants.MOD_ID + "." + path, wood.name());
            axe.add(wood.blockId().toString());
        }
        futures.add(DataProvider.saveStable(cache, lang, this.root.resolve("assets/" + Constants.MOD_ID + "/lang/en_us.json")));
        JsonObject tag = new JsonObject();
        tag.add("values", axe);
        futures.add(DataProvider.saveStable(cache, tag, this.root.resolve("data/minecraft/tags/block/mineable/axe.json")));
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName()
    {
        return "Mighty Mail compat pack: " + this.modId;
    }

    /** One pack.mcmeta that both the resource and the data side accept: a range spanning the two current formats. */
    private JsonObject packMeta()
    {
        PackFormat resources = SharedConstants.getCurrentVersion().packVersion(PackType.CLIENT_RESOURCES);
        PackFormat data = SharedConstants.getCurrentVersion().packVersion(PackType.SERVER_DATA);
        PackFormat min = resources.compareTo(data) <= 0 ? resources : data;
        PackFormat max = resources.compareTo(data) <= 0 ? data : resources;
        JsonObject pack = new JsonObject();
        pack.addProperty("description", "Mighty Mail mailboxes for " + this.modId);
        pack.add("min_format", formatArray(min));
        pack.add("max_format", formatArray(max));
        JsonObject root = new JsonObject();
        root.add("pack", pack);
        return root;
    }

    private static JsonArray formatArray(PackFormat format)
    {
        JsonArray array = new JsonArray();
        array.add(format.major());
        array.add(format.minor());
        return array;
    }

    private JsonObject blockState(String blockModel)
    {
        JsonObject variants = new JsonObject();
        for(Map.Entry<String, Integer> facing : Map.of("north", 0, "east", 90, "south", 180, "west", 270).entrySet())
        {
            for(boolean enabled : new boolean[]{false, true})
            {
                JsonObject variant = new JsonObject();
                variant.addProperty("model", enabled ? blockModel + "_unchecked" : blockModel);
                if(facing.getValue() != 0)
                    variant.addProperty("y", facing.getValue());
                variants.add("enabled=" + enabled + ",facing=" + facing.getKey(), variant);
            }
        }
        JsonObject root = new JsonObject();
        root.add("variants", variants);
        return root;
    }

    private JsonObject model(String parent, MailboxWood wood)
    {
        JsonObject textures = new JsonObject();
        textures.addProperty("particle", wood.planksTexture().toString());
        textures.addProperty("planks", wood.planksTexture().toString());
        JsonObject root = new JsonObject();
        root.addProperty("parent", Constants.MOD_ID + ":block/" + parent);
        root.add("textures", textures);
        return root;
    }

    private JsonObject itemDefinition(String blockModel)
    {
        JsonObject model = new JsonObject();
        model.addProperty("type", "minecraft:model");
        model.addProperty("model", blockModel);
        JsonObject root = new JsonObject();
        root.add("model", model);
        return root;
    }

    private JsonObject lootTable(MailboxWood wood)
    {
        JsonObject condition = new JsonObject();
        condition.addProperty("type", "minecraft:survives_explosion");
        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", wood.blockId().toString());
        JsonArray entries = new JsonArray();
        entries.add(entry);
        JsonObject pool = new JsonObject();
        pool.add("condition", condition);
        pool.add("entries", entries);
        pool.addProperty("rolls", 1);
        JsonArray pools = new JsonArray();
        pools.add(pool);
        JsonObject root = new JsonObject();
        root.addProperty("type", "minecraft:block");
        root.add("pools", pools);
        return root;
    }

    private JsonObject recipe(MailboxWood wood)
    {
        JsonObject key = new JsonObject();
        key.addProperty("C", "minecraft:chest");
        key.addProperty("F", wood.fence().toString());
        key.addProperty("P", wood.planks().toString());
        JsonArray pattern = new JsonArray();
        pattern.add("PCP");
        pattern.add("PPP");
        pattern.add(" F ");
        JsonObject result = new JsonObject();
        result.addProperty("id", wood.blockId().toString());
        JsonObject root = new JsonObject();
        root.addProperty("type", "minecraft:crafting_shaped");
        root.add("key", key);
        root.add("pattern", pattern);
        root.add("result", result);
        return root;
    }

    private JsonObject recipeAdvancement(MailboxWood wood)
    {
        String recipeId = wood.blockId().toString();
        JsonObject criteria = new JsonObject();
        criteria.add("has_chest", hasItem("minecraft:chest"));
        criteria.add("has_fence", hasItem(wood.fence().toString()));
        criteria.add("has_plank", hasItem(wood.planks().toString()));
        JsonObject recipeConditions = new JsonObject();
        recipeConditions.addProperty("recipes", recipeId);
        JsonObject hasRecipe = new JsonObject();
        hasRecipe.add("conditions", recipeConditions);
        hasRecipe.addProperty("trigger", "minecraft:recipe_unlocked");
        criteria.add("has_the_recipe", hasRecipe);
        JsonArray any = new JsonArray();
        for(String name : new String[]{"has_the_recipe", "has_plank", "has_chest", "has_fence"})
            any.add(name);
        JsonArray requirements = new JsonArray();
        requirements.add(any);
        JsonArray recipes = new JsonArray();
        recipes.add(recipeId);
        JsonObject rewards = new JsonObject();
        rewards.add("recipes", recipes);
        JsonObject root = new JsonObject();
        root.addProperty("parent", "minecraft:recipes/root");
        root.add("criteria", criteria);
        root.add("requirements", requirements);
        root.add("rewards", rewards);
        return root;
    }

    private static JsonObject hasItem(String itemId)
    {
        JsonObject item = new JsonObject();
        item.addProperty("items", itemId);
        JsonArray items = new JsonArray();
        items.add(item);
        JsonObject conditions = new JsonObject();
        conditions.add("items", items);
        JsonObject criterion = new JsonObject();
        criterion.add("conditions", conditions);
        criterion.addProperty("trigger", "minecraft:inventory_changed");
        return criterion;
    }

    public static Identifier packId(String modId)
    {
        return Identifier.fromNamespaceAndPath(Constants.MOD_ID, "compat_" + modId);
    }
}
