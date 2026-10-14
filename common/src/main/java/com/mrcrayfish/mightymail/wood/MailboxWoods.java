package com.mrcrayfish.mightymail.wood;

import com.mrcrayfish.framework.platform.Services;
import com.mrcrayfish.mightymail.block.MailboxBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The single list of mailbox woods. Registration, data generation, the creative tab and the migrate
 * command all read it, so a new wood is one more row here.
 */
public final class MailboxWoods
{
    /** The vanilla woods, in creative tab order. Their block ids are unchanged from Mighty Mail 1.2.1. */
    public static final List<MailboxWood> VANILLA = List.of(
        MailboxWood.vanilla("oak", "Oak Mailbox", Blocks.OAK_PLANKS.defaultMapColor()),
        MailboxWood.vanilla("spruce", "Spruce Mailbox", Blocks.SPRUCE_PLANKS.defaultMapColor()),
        MailboxWood.vanilla("birch", "Birch Mailbox", Blocks.BIRCH_PLANKS.defaultMapColor()),
        MailboxWood.vanilla("jungle", "Jungle Mailbox", Blocks.JUNGLE_PLANKS.defaultMapColor()),
        MailboxWood.vanilla("acacia", "Acacia Mailbox", Blocks.ACACIA_PLANKS.defaultMapColor()),
        MailboxWood.vanilla("dark_oak", "Dark Oak Mailbox", Blocks.DARK_OAK_PLANKS.defaultMapColor()),
        MailboxWood.vanilla("mangrove", "Mangrove Mailbox", Blocks.MANGROVE_PLANKS.defaultMapColor()),
        MailboxWood.vanilla("cherry", "Cherry Mailbox", Blocks.CHERRY_PLANKS.defaultMapColor()),
        MailboxWood.vanilla("crimson", "Crimson Mailbox", Blocks.CRIMSON_PLANKS.defaultMapColor()),
        MailboxWood.vanilla("warped", "Warped Mailbox", Blocks.WARPED_PLANKS.defaultMapColor()),
        MailboxWood.vanilla("pale_oak", "Pale Oak Mailbox", Blocks.PALE_OAK_PLANKS.defaultMapColor())
    );

    /** Woods from other mods. Each mailbox only exists when its mod is loaded. Planks ids checked against the 26.3 jars. */
    public static final List<MailboxWood> COMPAT = List.of(
        // Biomes O' Plenty
        MailboxWood.modded("biomesoplenty", "fir", MapColor.TERRACOTTA_WHITE),
        MailboxWood.modded("biomesoplenty", "pine", MapColor.TERRACOTTA_ORANGE),
        MailboxWood.modded("biomesoplenty", "maple", MapColor.TERRACOTTA_ORANGE),
        MailboxWood.modded("biomesoplenty", "redwood", MapColor.TERRACOTTA_ORANGE),
        MailboxWood.modded("biomesoplenty", "mahogany", MapColor.TERRACOTTA_PINK),
        MailboxWood.modded("biomesoplenty", "jacaranda", MapColor.QUARTZ),
        MailboxWood.modded("biomesoplenty", "palm", MapColor.TERRACOTTA_YELLOW),
        MailboxWood.modded("biomesoplenty", "willow", MapColor.TERRACOTTA_LIGHT_GREEN),
        MailboxWood.modded("biomesoplenty", "dead", MapColor.STONE),
        MailboxWood.modded("biomesoplenty", "magic", MapColor.COLOR_BLUE),
        MailboxWood.modded("biomesoplenty", "umbran", MapColor.TERRACOTTA_BLUE),
        MailboxWood.modded("biomesoplenty", "hellbark", MapColor.TERRACOTTA_GRAY),
        MailboxWood.modded("biomesoplenty", "empyreal", MapColor.TERRACOTTA_PURPLE),
        MailboxWood.modded("biomesoplenty", "origin_oak", MapColor.WOOD),
        // Ecologics
        MailboxWood.modded("ecologics", "coconut", MapColor.TERRACOTTA_BROWN),
        MailboxWood.modded("ecologics", "walnut", MapColor.TERRACOTTA_BROWN),
        MailboxWood.modded("ecologics", "azalea", MapColor.TERRACOTTA_LIGHT_GREEN),
        MailboxWood.modded("ecologics", "flowering_azalea", MapColor.TERRACOTTA_PINK),
        MailboxWood.modded("ecologics", "maple", MapColor.TERRACOTTA_ORANGE),
        // Wilder Wild
        MailboxWood.modded("wilderwild", "baobab", MapColor.TERRACOTTA_ORANGE),
        MailboxWood.modded("wilderwild", "cypress", MapColor.TERRACOTTA_LIGHT_GRAY),
        MailboxWood.modded("wilderwild", "palm", MapColor.TERRACOTTA_YELLOW),
        MailboxWood.modded("wilderwild", "maple", MapColor.TERRACOTTA_ORANGE),
        MailboxWood.modded("wilderwild", "willow", MapColor.TERRACOTTA_LIGHT_GREEN)
    );

    /** The mods that have compat rows, in table order. */
    public static List<String> compatMods()
    {
        return COMPAT.stream().map(MailboxWood::requiredMod).distinct().toList();
    }

    public static final List<MailboxWood> ALL = concat(VANILLA, COMPAT);

    private static final Map<String, Supplier<MailboxBlock>> BLOCKS = new LinkedHashMap<>();

    private static boolean registered;

    /**
     * Registers a mailbox block and item for every wood whose mod is loaded, once. Framework scans
     * this mod's registry classes during its own startup, which can be before this mod's entry point
     * runs, so {@link com.mrcrayfish.mightymail.core.ModBlockEntities} calls this from its static
     * initialiser and the entry points call it too; whichever comes first does the work.
     */
    public static void ensureRegistered()
    {
        if(registered)
            return;
        registered = true;
        Registrar registrar = com.mrcrayfish.mightymail.platform.Services.PLATFORM.mailboxRegistrar();
        for(MailboxWood wood : ALL)
        {
            if(wood.requiredMod() != null && !Services.PLATFORM.isModLoaded(wood.requiredMod()))
                continue;
            BLOCKS.put(wood.id(), registrar.register(wood, () -> BlockBehaviour.Properties.of()
                .mapColor(wood.mapColor())
                .instrument(NoteBlockInstrument.BASS)
                .strength(3.5F)
                .sound(SoundType.WOOD)
                .ignitedByLava()));
        }
    }

    /** The registered mailbox blocks, vanilla first, in table order. */
    public static List<MailboxBlock> blocks()
    {
        List<MailboxBlock> list = new ArrayList<>(BLOCKS.size());
        BLOCKS.values().forEach(supplier -> list.add(supplier.get()));
        return Collections.unmodifiableList(list);
    }

    /** The mailbox block of a wood by its id, for example {@code oak}, or null if that wood isn't registered. */
    public static MailboxBlock block(String id)
    {
        Supplier<MailboxBlock> supplier = BLOCKS.get(id);
        return supplier != null ? supplier.get() : null;
    }

    /** The rows whose mailboxes exist in this game. */
    public static List<MailboxWood> registered()
    {
        return ALL.stream().filter(wood -> BLOCKS.containsKey(wood.id())).toList();
    }

    /** Registers one mailbox block and its item with the loader's registry. Implemented per loader. */
    @FunctionalInterface
    public interface Registrar
    {
        Supplier<MailboxBlock> register(MailboxWood wood, Supplier<BlockBehaviour.Properties> properties);
    }

    private static List<MailboxWood> concat(List<MailboxWood> a, List<MailboxWood> b)
    {
        List<MailboxWood> list = new ArrayList<>(a);
        list.addAll(b);
        return Collections.unmodifiableList(list);
    }
}
