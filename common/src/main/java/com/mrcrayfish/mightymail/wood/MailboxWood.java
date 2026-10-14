package com.mrcrayfish.mightymail.wood;

import com.mrcrayfish.mightymail.Constants;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

/**
 * One kind of mailbox. Every mailbox block, model, recipe and name comes from one of these, so adding
 * a wood is adding a row to {@link MailboxWoods}.
 *
 * @param id          the block path, for example {@code oak} or {@code biomesoplenty_fir}
 * @param name        the English block name
 * @param planks      the wood's planks block, used for the crafting recipe and the break particles
 * @param fence       the wood's fence block, used for the crafting recipe
 * @param painted     MrCrayfish's hand-painted 32x32 texture, or null to build the model from the planks texture
 * @param requiredMod the mod that adds the wood, or null for vanilla; the mailbox only exists when it is loaded
 * @param mapColor    the colour on maps
 */
public record MailboxWood(String id, String name, Identifier planks, Identifier fence, @Nullable Identifier painted,
                          @Nullable String requiredMod, MapColor mapColor)
{
    public Identifier blockId()
    {
        return Identifier.fromNamespaceAndPath(Constants.MOD_ID, this.id + "_mail_box");
    }

    /** The planks texture, {@code <namespace>:block/<path>} */
    public Identifier planksTexture()
    {
        return Identifier.fromNamespaceAndPath(this.planks.getNamespace(), "block/" + this.planks.getPath());
    }

    public boolean isVanilla()
    {
        return this.requiredMod == null;
    }

    /** A vanilla wood: planks and fence from {@link Blocks}, painted texture shipped in this mod. */
    public static MailboxWood vanilla(String wood, String name, MapColor mapColor)
    {
        return new MailboxWood(wood, name,
            Identifier.withDefaultNamespace(wood + "_planks"),
            Identifier.withDefaultNamespace(wood + "_fence"),
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "block/" + wood + "_mail_box"),
            null, mapColor);
    }

    /** A wood from another mod, with the usual {@code <wood>_planks} and {@code <wood>_fence} ids and a name made from the wood. */
    public static MailboxWood modded(String modId, String wood, MapColor mapColor)
    {
        return new MailboxWood(modId + "_" + wood, titleCase(wood) + " Mailbox",
            Identifier.fromNamespaceAndPath(modId, wood + "_planks"),
            Identifier.fromNamespaceAndPath(modId, wood + "_fence"),
            null, modId, mapColor);
    }

    /** {@code flowering_azalea} to {@code Flowering Azalea} */
    static String titleCase(String id)
    {
        StringBuilder out = new StringBuilder();
        for(String word : id.split("_"))
        {
            if(!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }
}
