package com.mrcrayfish.mightymail.datagen;

import com.mrcrayfish.framework.api.datagen.FrameworkGenerator;
import com.mrcrayfish.mightymail.block.MailboxBlock;
import com.mrcrayfish.mightymail.block.PostBoxBlock;
import com.mrcrayfish.mightymail.core.ModBlocks;
import com.mrcrayfish.mightymail.util.Utils;
import com.mrcrayfish.mightymail.wood.MailboxWood;
import com.mrcrayfish.mightymail.wood.MailboxWoods;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.BlockModelDefinitionGenerator;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.*;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.Map;

import static net.minecraft.client.data.models.BlockModelGenerators.*;

@SuppressWarnings("UnstableApiUsage")
public class BlockStatesGen extends FrameworkGenerator
{
    public BlockStatesGen(Map<Block, BlockModelDefinitionGenerator> generators, Map<Item, ClientItem> items, Map<Identifier, ModelInstance> models)
    {
        super(generators, items, models);
    }

    @Override
    public void generate()
    {
        for(MailboxWood wood : MailboxWoods.VANILLA)
        {
            this.mailbox(wood);
        }
        this.postBox(ModBlocks.POST_BOX.get());
    }


    private void registerItemWithModelFromMultiVariant(Block block, MultiVariant variant)
    {
        Identifier location = variant.variants().unwrap().getFirst().value().modelLocation();
        this.items.put(block.asItem(), this.createClientItem(ItemModelUtils.plainModel(location)));
    }
    
    private Variant plainModel(Identifier location)
    {
        return new Variant(location);
    }
    
    private MultiVariant variant(Variant variant)
    {
        return new MultiVariant(WeightedList.of(variant));
    }
    
    private MultiVariant plainVariant(Identifier location)
    {
        return this.variant(this.plainModel(location));
    }

    private void mailbox(MailboxWood wood)
    {
        MailboxBlock block = MailboxWoods.block(wood.id());
        TextureMapping textures = new TextureMapping()
            .put(TextureSlot.PARTICLE, new Material(wood.planksTexture()))
            .put(TextureSlot.TEXTURE, new Material(wood.painted()));
        MultiVariant mailboxVariant = this.plainVariant(ModelDefinitions.MAIL_BOX.create(block, textures, this.models::put));
        MultiVariant mailboxUncheckedVariant = this.plainVariant(ModelDefinitions.MAIL_BOX_UNCHECKED.create(block, textures, this.models::put));
        this.registerItemWithModelFromMultiVariant(block, mailboxVariant);
        this.generators.put(block, MultiVariantGenerator.dispatch(block)
            .with(PropertyDispatch.initial(MailboxBlock.DIRECTION, MailboxBlock.ENABLED)
                .select(Direction.NORTH, false, mailboxVariant)
                .select(Direction.EAST, false, mailboxVariant.with(Y_ROT_90))
                .select(Direction.SOUTH, false, mailboxVariant.with(Y_ROT_180))
                .select(Direction.WEST, false, mailboxVariant.with(Y_ROT_270))
                .select(Direction.NORTH, true, mailboxUncheckedVariant)
                .select(Direction.EAST, true, mailboxUncheckedVariant.with(Y_ROT_90))
                .select(Direction.SOUTH, true, mailboxUncheckedVariant.with(Y_ROT_180))
                .select(Direction.WEST, true, mailboxUncheckedVariant.with(Y_ROT_270))));
    }

    private void postBox(PostBoxBlock block)
    {
        MultiVariant postboxVariant = this.plainVariant(ModelLocationUtils.getModelLocation(block));
        this.registerItemWithModelFromMultiVariant(block, postboxVariant);
        this.generators.put(block, MultiVariantGenerator.dispatch(block)
            .with(PropertyDispatch.initial(PostBoxBlock.DIRECTION)
                .select(Direction.NORTH, postboxVariant)
                .select(Direction.EAST, postboxVariant.with(Y_ROT_90))
                .select(Direction.SOUTH, postboxVariant.with(Y_ROT_180))
                .select(Direction.WEST, postboxVariant.with(Y_ROT_270))));
    }
}
