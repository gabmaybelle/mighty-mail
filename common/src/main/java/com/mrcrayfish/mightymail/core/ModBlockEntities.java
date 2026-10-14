package com.mrcrayfish.mightymail.core;

import com.mrcrayfish.framework.api.registry.RegistryContainer;
import com.mrcrayfish.framework.api.registry.RegistryEntry;
import com.mrcrayfish.mightymail.blockentity.MailboxBlockEntity;
import com.mrcrayfish.mightymail.blockentity.PostBoxBlockEntity;
import com.mrcrayfish.mightymail.util.Utils;
import com.mrcrayfish.mightymail.wood.MailboxWoods;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Author: MrCrayfish
 */
@RegistryContainer
public class ModBlockEntities
{
    static
    {
        // The mailbox blocks must exist before the block entity type below lists them
        MailboxWoods.ensureRegistered();
    }

    public static final RegistryEntry<BlockEntityType<MailboxBlockEntity>> MAIL_BOX = RegistryEntry.blockEntity(Utils.resource("mail_box"), MailboxBlockEntity::new, () -> MailboxWoods.blocks().toArray(Block[]::new));

    public static final RegistryEntry<BlockEntityType<PostBoxBlockEntity>> POST_BOX = RegistryEntry.blockEntity(Utils.resource("post_box"), PostBoxBlockEntity::new, () -> new Block[]{
            ModBlocks.POST_BOX.get()
    });
}
