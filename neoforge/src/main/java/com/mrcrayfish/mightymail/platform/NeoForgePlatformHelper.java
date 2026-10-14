package com.mrcrayfish.mightymail.platform;

import com.mrcrayfish.mightymail.Constants;
import com.mrcrayfish.mightymail.block.MailboxBlock;
import com.mrcrayfish.mightymail.item.MailboxItem;
import com.mrcrayfish.mightymail.platform.services.IPlatformHelper;
import com.mrcrayfish.mightymail.wood.MailboxWoods;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.world.level.ItemLike;

import java.util.function.Consumer;

public class NeoForgePlatformHelper implements IPlatformHelper
{
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    /** Called from the mod constructor so the deferred registers fire with the registry events. */
    public static void register(IEventBus bus)
    {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }

    @Override
    public void displayItemsAcceptor(CreativeModeTab.Builder builder, Consumer<Consumer<ItemLike>> consumer)
    {
        builder.displayItems((params, output) -> consumer.accept(output::accept));
    }

    @Override
    public MailboxWoods.Registrar mailboxRegistrar()
    {
        return (wood, properties) -> {
            String path = wood.blockId().getPath();
            DeferredBlock<MailboxBlock> block = BLOCKS.registerBlock(path, MailboxBlock::new, properties);
            ITEMS.registerItem(path, props -> new MailboxItem(block.get(), props), () -> new Item.Properties().useBlockDescriptionPrefix());
            return block;
        };
    }
}
