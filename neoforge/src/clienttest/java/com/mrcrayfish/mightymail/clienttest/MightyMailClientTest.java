package com.mrcrayfish.mightymail.clienttest;

import com.mrcrayfish.mightymail.Constants;
import com.mrcrayfish.mightymail.block.MailboxBlock;
import com.mrcrayfish.mightymail.blockentity.MailboxBlockEntity;
import com.mrcrayfish.mightymail.client.gui.screen.PostBoxScreen;
import com.mrcrayfish.mightymail.core.ModBlocks;
import com.mrcrayfish.mightymail.core.ModDataComponents;
import com.mrcrayfish.mightymail.core.ModItems;
import com.mrcrayfish.mightymail.inventory.PostBoxMenu;
import com.mrcrayfish.mightymail.item.PackageItem;
import com.mrcrayfish.mightymail.mail.DeliveryService;
import com.mrcrayfish.mightymail.mail.IMailbox;
import com.mrcrayfish.mightymail.mail.Mailbox;
import com.mrcrayfish.mightymail.mail.PackageInfo;
import com.mrcrayfish.mightymail.network.message.MessageSendPackage;
import com.mrcrayfish.mightymail.network.message.MessageSetMailboxName;
import com.mrcrayfish.mightymail.network.play.ServerPlayHandler;
import com.mrcrayfish.mightymail.wood.MailboxWoods;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.mrcrayfish.mightymail.clienttest.ClientTest.check;
import static com.mrcrayfish.mightymail.clienttest.ClientTest.onServer;
import static com.mrcrayfish.mightymail.clienttest.ClientTest.run;
import static com.mrcrayfish.mightymail.clienttest.ClientTest.screenshot;
import static com.mrcrayfish.mightymail.clienttest.ClientTest.waitTicks;
import static com.mrcrayfish.mightymail.clienttest.ClientTest.waitUntil;

/**
 * The mod in a real NeoForge game: {@code ./gradlew :neoforge:runClientTest}. The core of the Fabric
 * game test, on the NeoForge jar: the generated data loads, a mailbox is placed and named through the
 * server handler, the post box opens its real screen and lists it, a package is sent, delivered (flag
 * up), the mailbox opened (flag down) and the package unpacked, and a mailbox broken with mail inside
 * drops it. The mail code itself is shared, and the Fabric test covers restarts and the pre-26.3
 * import; this one proves the NeoForge registration, networking, screens and data. Screenshots land
 * in neoforge/build/run/clientTest/screenshots.
 */
final class MightyMailClientTest
{
    private static final BlockPos POST_BOX = new BlockPos(0, 71, 0);
    private static final BlockPos MAILBOX = new BlockPos(3, 71, 0);
    private static final BlockPos SPARE = new BlockPos(-3, 71, 0);
    private static final UUID[] MAILBOX_ID = new UUID[1];

    private MightyMailClientTest() {}

    static void steps()
    {
        ClientTest.createWorld("mighty-mail-test");
        platform();
        generatedData();
        placeAndName();
        postBox();
        delivery();
        openAndUnpack();
        breakWithMail();
    }

    /** A stone platform in the sky with the player on it, so the screenshots are just the mod. */
    private static void platform()
    {
        onServer(s -> {
            ServerLevel level = s.overworld();
            for(int x = -8; x <= 8; x++)
                for(int z = -8; z <= 8; z++)
                    level.setBlockAndUpdate(new BlockPos(x, 70, z), Blocks.SMOOTH_STONE.defaultBlockState());
            player(s).teleportTo(level, 0.5, 71, -4.5, Set.of(), 0, 0, false);
            check(DeliveryService.get(s).isPresent(), "the delivery service exists in a new world");
            check(DeliveryService.get(s).map(d -> d.getMailboxes().isEmpty()).orElse(false), "a new world starts with no mailboxes");
        });
        check(MailboxWoods.registered().size() == MailboxWoods.VANILLA.size(), "the eleven vanilla mailboxes are registered, and no compat ones without their mods");
        waitTicks(20);
    }

    /** Oak's generated recipe, loot table and model load, which also catches a jar built without datagen. */
    private static void generatedData()
    {
        Identifier id = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "oak_mail_box");
        check(BuiltInRegistries.BLOCK.containsKey(id) && BuiltInRegistries.ITEM.containsKey(id), "the oak mailbox block and item exist");
        onServer(s -> {
            check(s.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, id)).isPresent(), "the oak mailbox has its crafting recipe");
            check(s.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(Constants.MOD_ID, "blocks/oak_mail_box"))) != LootTable.EMPTY, "the oak mailbox has its loot table");
        });
        run(mc -> {
            BlockStateModelSet models = mc.getModelManager().getBlockStateModelSet();
            BlockState state = MailboxWoods.block("oak").defaultBlockState();
            check(models.get(state) != models.missingModel() && !models.getParticleMaterial(state).sprite().contents().name().equals(MissingTextureAtlasSprite.getLocation()), "the oak mailbox has a real model and texture");
        });
    }

    /** Placing a mailbox makes the player its owner; the naming handler sets its name. */
    private static void placeAndName()
    {
        onServer(s -> {
            ServerLevel level = s.overworld();
            ServerPlayer player = player(s);
            placeAsPlayer(level, player, POST_BOX, ModBlocks.POST_BOX.get().defaultBlockState());
            placeAsPlayer(level, player, MAILBOX, MailboxWoods.block("oak").defaultBlockState());
            Optional<Mailbox> mailbox = DeliveryService.get(s).orElseThrow().getMailboxAtPosition(level, MAILBOX);
            check(mailbox.isPresent(), "a placed mailbox is registered with the delivery service");
            check(mailbox.flatMap(Mailbox::owner).map(player.getUUID()::equals).orElse(false), "the player who placed it owns it");
            ServerPlayHandler.handleMessageSetMailboxName(new MessageSetMailboxName(MAILBOX, "Test Box"), player);
            check(mailbox.flatMap(Mailbox::customName).map("Test Box"::equals).orElse(false), "the naming screen's message names the mailbox");
            MAILBOX_ID[0] = mailbox.map(Mailbox::id).orElse(UUID.randomUUID());
        });
    }

    /** The post box opens its real screen on the client, lists the mailbox, and the send handler posts the package. */
    private static void postBox()
    {
        onServer(s -> use(s.overworld(), player(s), POST_BOX));
        waitUntil("the post box screen", 100, mc -> mc.gui.screen() instanceof PostBoxScreen);
        run(mc -> {
            PostBoxScreen screen = (PostBoxScreen) mc.gui.screen();
            check(screen.getMenu().getMailboxes().stream().map(IMailbox::getCustomName).anyMatch(name -> name.map("Test Box"::equals).orElse(false)), "the post box lists the mailbox by its name");
            check(screen.getMenu().getMailboxes().stream().allMatch(box -> box.getOwner(null).isPresent()), "the list carries the owner (face and name) on the client");
        });
        waitTicks(10);
        screenshot("mighty_mail_post_box");
        onServer(s -> {
            ServerPlayer player = player(s);
            PostBoxMenu menu = (PostBoxMenu) player.containerMenu;
            menu.getContainer().setItem(0, new ItemStack(Items.DIAMOND, 3));
            ServerPlayHandler.handleMessageSendPackage(new MessageSendPackage(MAILBOX_ID[0], "Hello from the test"), player, null);
            check(menu.getContainer().isEmpty(), "sending empties the post box slots");
            check(DeliveryService.get(s).orElseThrow().getMailboxAtPosition(s.overworld(), MAILBOX).map(m -> m.queue().size() == 1).orElse(false), "the package is queued for the mailbox");
            player.closeContainer();
        });
        waitUntil("the post box screen to close", 100, mc -> !(mc.gui.screen() instanceof PostBoxScreen));
    }

    /** The delivery tick moves the package into the mailbox and raises the flag. */
    private static void delivery()
    {
        waitTicks(40);
        onServer(s -> {
            ServerLevel level = s.overworld();
            check(level.getBlockState(MAILBOX).getValue(MailboxBlock.ENABLED), "the flag goes up when the package arrives");
            ItemStack inside = level.getBlockEntity(MAILBOX) instanceof MailboxBlockEntity entity ? entity.getItem(0) : ItemStack.EMPTY;
            check(inside.is(ModItems.PACKAGE.get()), "the mailbox holds a package");
            PackageInfo info = inside.get(ModDataComponents.PACKAGE_INFO.get());
            check(info != null && info.message().map("Hello from the test"::equals).orElse(false), "the package carries the message");
            check(PackageItem.getPackagedItems(inside).nonEmptyItemCopyStream().anyMatch(stack -> stack.is(Items.DIAMOND) && stack.getCount() == 3), "the package holds the 3 diamonds");
            player(s).teleportTo(level, 3.5, 71, -4.5, Set.of(), 0, 10, false);
        });
        waitTicks(15);
        screenshot("mighty_mail_flag_up");
    }

    /** Opening the mailbox lowers the flag; opening the package gives the player its items. */
    private static void openAndUnpack()
    {
        onServer(s -> use(s.overworld(), player(s), MAILBOX));
        waitUntil("the mailbox screen", 100, mc -> mc.gui.screen() != null);
        waitTicks(5);
        onServer(s -> {
            ServerLevel level = s.overworld();
            ServerPlayer player = player(s);
            check(!level.getBlockState(MAILBOX).getValue(MailboxBlock.ENABLED), "opening the mailbox lowers the flag");
            ItemStack pack = ((MailboxBlockEntity) level.getBlockEntity(MAILBOX)).removeItem(0, 1);
            player.closeContainer();
            player.getInventory().clearContent();
            player.setItemInHand(InteractionHand.MAIN_HAND, pack);
            pack.getItem().use(level, player, InteractionHand.MAIN_HAND);
        });
        waitTicks(40);
        onServer(s -> {
            check(player(s).getInventory().countItem(Items.DIAMOND) == 3, "opening the package gives the player the 3 diamonds");
            check(player(s).getInventory().countItem(ModItems.PACKAGE.get()) == 0, "the package is used up");
        });
    }

    /** A mailbox broken with mail inside drops the mail and leaves the registry. */
    private static void breakWithMail()
    {
        onServer(s -> {
            ServerLevel level = s.overworld();
            placeAsPlayer(level, player(s), SPARE, MailboxWoods.block("cherry").defaultBlockState());
            DeliveryService service = DeliveryService.get(s).orElseThrow();
            Mailbox spare = service.getMailboxAtPosition(level, SPARE).orElseThrow();
            check(service.sendMail(spare.id(), PackageItem.create(NonNullList.withSize(1, new ItemStack(Items.BREAD, 2)), "Spare mail", "Test")).success(), "a package can be sent to a second mailbox");
        });
        waitTicks(40);
        onServer(s -> {
            ServerLevel level = s.overworld();
            check(level.getBlockState(SPARE).getValue(MailboxBlock.ENABLED), "the second mailbox's flag goes up");
            player(s).getInventory().clearContent();
            level.destroyBlock(SPARE, true, player(s));
        });
        waitTicks(20);
        onServer(s -> {
            ServerLevel level = s.overworld();
            long packages = level.getEntitiesOfClass(ItemEntity.class, new AABB(SPARE).inflate(3)).stream().filter(e -> e.getItem().is(ModItems.PACKAGE.get())).count();
            check(packages == 1 || player(s).getInventory().countItem(ModItems.PACKAGE.get()) == 1, "breaking a mailbox with mail inside drops the package");
            check(DeliveryService.get(s).orElseThrow().getMailboxAtPosition(level, SPARE).isEmpty(), "the broken mailbox is gone from the delivery service");
        });
    }

    /** Places a block the way a player does, so the mod's placement hook runs (owner, pending name). */
    private static void placeAsPlayer(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state)
    {
        level.setBlock(pos, state, Block.UPDATE_ALL);
        state.getBlock().setPlacedBy(level, pos, state, player, new ItemStack(state.getBlock()));
    }

    /** Right-clicks the block with an empty hand, server side. */
    private static void use(ServerLevel level, ServerPlayer player, BlockPos pos)
    {
        BlockState state = level.getBlockState(pos);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        state.useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false));
    }

    private static ServerPlayer player(MinecraftServer server)
    {
        return server.getPlayerList().getPlayers().get(0);
    }
}
