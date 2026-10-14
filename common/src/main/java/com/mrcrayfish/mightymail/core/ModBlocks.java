package com.mrcrayfish.mightymail.core;

import com.mrcrayfish.framework.api.registry.RegistryContainer;
import com.mrcrayfish.framework.api.registry.RegistryEntry;
import com.mrcrayfish.mightymail.block.PostBoxBlock;
import com.mrcrayfish.mightymail.util.Utils;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

/**
 * Author: MrCrayfish
 *
 * The mailboxes are not here: every wood's mailbox comes from {@link com.mrcrayfish.mightymail.wood.MailboxWoods}
 * and is registered by the loader at startup, since Framework's container scan only takes fixed fields.
 */
@RegistryContainer
public class ModBlocks
{
    public static final RegistryEntry<PostBoxBlock> POST_BOX = RegistryEntry.blockWithItem(
            Utils.resource("post_box"),
            PostBoxBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .strength(3.5F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops());
}
