package com.mrcrayfish.mightymail.platform.services;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.ItemLike;

import java.util.function.Consumer;

/**
 * Common-side hooks that need loader specific access (see the loader projects for the implementations)
 */
public interface IPlatformHelper
{
    /**
     * Adds items to a creative tab. {@link CreativeModeTab.Output} is protected since 26.x, so the
     * display generator can only be written where the loader has widened it.
     */
    void displayItemsAcceptor(CreativeModeTab.Builder builder, Consumer<Consumer<ItemLike>> consumer);
}
