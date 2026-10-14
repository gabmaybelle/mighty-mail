package com.mrcrayfish.mightymail.core;

import com.mrcrayfish.framework.api.registry.RegistryContainer;
import com.mrcrayfish.framework.api.registry.RegistryEntry;
import com.mrcrayfish.mightymail.Constants;
import com.mrcrayfish.mightymail.platform.Services;
import com.mrcrayfish.mightymail.util.Utils;
import com.mrcrayfish.mightymail.wood.MailboxWoods;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * Author: MrCrayfish
 */
@RegistryContainer
public class ModCreativeTabs
{
    public static final RegistryEntry<CreativeModeTab> MAIN = RegistryEntry.creativeModeTab(Utils.resource("creative_tab"), builder -> {
        builder.icon(() -> new ItemStack(MailboxWoods.block("oak")));
        builder.title(Component.translatable("itemGroup." + Constants.MOD_ID));
        // CreativeModeTab.Output is protected in vanilla; the loaders widen it, so the lambda lives in their helpers
        Services.PLATFORM.displayItemsAcceptor(builder, consumer -> {
            MailboxWoods.blocks().forEach(consumer::accept);
            consumer.accept(ModBlocks.POST_BOX.get());
        });
    });
}
