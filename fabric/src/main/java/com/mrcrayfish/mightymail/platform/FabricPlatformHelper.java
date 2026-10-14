package com.mrcrayfish.mightymail.platform;

import com.mrcrayfish.mightymail.block.MailboxBlock;
import com.mrcrayfish.mightymail.item.MailboxItem;
import com.mrcrayfish.mightymail.platform.services.IPlatformHelper;
import com.mrcrayfish.mightymail.wood.MailboxWoods;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

import java.util.function.Consumer;

public class FabricPlatformHelper implements IPlatformHelper
{
    @Override
    public void displayItemsAcceptor(CreativeModeTab.Builder builder, Consumer<Consumer<ItemLike>> consumer)
    {
        builder.displayItems((params, output) -> consumer.accept(output::accept));
    }

    @Override
    public MailboxWoods.Registrar mailboxRegistrar()
    {
        return (wood, properties) -> {
            Identifier id = wood.blockId();
            MailboxBlock block = Registry.register(BuiltInRegistries.BLOCK, id, new MailboxBlock(properties.get().setId(ResourceKey.create(Registries.BLOCK, id))));
            Registry.register(BuiltInRegistries.ITEM, id, new MailboxItem(block, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)).useBlockDescriptionPrefix()));
            return () -> block;
        };
    }
}
