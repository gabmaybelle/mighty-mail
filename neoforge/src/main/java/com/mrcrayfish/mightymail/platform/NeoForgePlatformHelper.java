package com.mrcrayfish.mightymail.platform;

import com.mrcrayfish.mightymail.platform.services.IPlatformHelper;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.ItemLike;

import java.util.function.Consumer;

public class NeoForgePlatformHelper implements IPlatformHelper
{
    @Override
    public void displayItemsAcceptor(CreativeModeTab.Builder builder, Consumer<Consumer<ItemLike>> consumer)
    {
        builder.displayItems((params, output) -> consumer.accept(output::accept));
    }
}
