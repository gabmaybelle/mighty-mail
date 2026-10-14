package com.mrcrayfish.mightymail.gametest;

import com.mrcrayfish.mightymail.Constants;
import com.mrcrayfish.mightymail.block.MailboxBlock;
import com.mrcrayfish.mightymail.blockentity.MailboxBlockEntity;
import com.mrcrayfish.mightymail.client.gui.screen.PostBoxScreen;
import com.mrcrayfish.mightymail.core.ModBlocks;
import com.mrcrayfish.mightymail.core.ModDataComponents;
import com.mrcrayfish.mightymail.core.ModItems;
import com.mrcrayfish.mightymail.inventory.PostBoxMenu;
import com.mrcrayfish.mightymail.item.PackageItem;
import com.mrcrayfish.mightymail.mail.DeliveryResult;
import com.mrcrayfish.mightymail.mail.DeliveryService;
import com.mrcrayfish.mightymail.mail.IMailbox;
import com.mrcrayfish.mightymail.mail.Mailbox;
import com.mrcrayfish.mightymail.mail.PackageInfo;
import com.mrcrayfish.mightymail.network.message.MessageSendPackage;
import com.mrcrayfish.mightymail.network.message.MessageSetMailboxName;
import com.mrcrayfish.mightymail.network.play.ServerPlayHandler;
import com.mrcrayfish.mightymail.wood.MailboxWoods;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.util.RandomSource;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The mod in a real game: {@code ./gradlew :fabric:runClientGameTest}. One flat world, one player,
 * and the whole mail flow in order: place a mailbox and name it through the same server handler the
 * naming screen uses, open the post box (the real screen, on the client), send a package through
 * the send handler, watch the delivery tick put it in the mailbox and raise the flag, open the
 * mailbox (flag down), open the package, and break a mailbox with mail inside. A few wrong inputs
 * are tried too (a name that is too long, a mailbox that does not exist). Screenshots land in
 * fabric/build/run/clientGameTest/screenshots. Every check is listed at the end; the test fails if
 * any of them did.
 */
public class MightyMailClientTest implements FabricClientGameTest
{
    private static final BlockPos POST_BOX = new BlockPos(0, 71, 0);
    private static final BlockPos MAILBOX = new BlockPos(3, 71, 0);
    private static final BlockPos SPARE = new BlockPos(-3, 71, 0);
    private static final BlockPos GHOST = new BlockPos(0, 71, 4);
    private static final BlockPos COMPAT = new BlockPos(0, 71, -4);
    private final List<String> failures = new ArrayList<>();

    @Override
    public void runTest(ClientGameTestContext ctx)
    {
        try(TestSingleplayerContext world = ctx.worldBuilder().create())
        {
            world.getConnection().waitForChunksRender();
            TestServerContext server = world.getServer();
            this.platform(server);
            this.generatedData(ctx, server, "oak", "from datagen");
            UUID mailboxId = this.placeAndName(server);
            this.postBox(ctx, server, mailboxId);
            this.delivery(ctx, server);
            this.openAndUnpack(ctx, server);
            this.wrongInputs(server);
            this.breakWithMail(ctx, server);
            this.woodMod(ctx, server);
        }
        if(!this.failures.isEmpty())
        {
            throw new AssertionError(this.failures.size() + " check(s) failed:\n - " + String.join("\n - ", this.failures));
        }
    }

    /** A stone platform in the sky with the player on it, so the screenshots are just the mod. */
    private void platform(TestServerContext server)
    {
        server.runOnServer(s -> {
            ServerLevel level = s.overworld();
            for(int x = -8; x <= 8; x++)
                for(int z = -8; z <= 8; z++)
                    level.setBlockAndUpdate(new BlockPos(x, 70, z), Blocks.SMOOTH_STONE.defaultBlockState());
            ServerPlayer player = player(s);
            player.setGameMode(GameType.CREATIVE);
            player.teleportTo(level, 0.5, 71, -4.5, Set.of(), 0, 0, false);
        });
        this.check(server.computeOnServer(s -> DeliveryService.get(s).isPresent()), "the delivery service exists in a new world");
        this.check(server.computeOnServer(s -> DeliveryService.get(s).map(d -> d.getMailboxes().isEmpty()).orElse(false)), "a new world starts with no mailboxes");
        long compatHere = MailboxWoods.COMPAT.stream().filter(wood -> FabricLoader.getInstance().isModLoaded(wood.requiredMod())).count();
        this.check(MailboxWoods.registered().size() == MailboxWoods.VANILLA.size() + compatHere,
            "the eleven vanilla mailboxes are registered plus one per compat wood whose mod is here (" + compatHere + ")");
    }

    /**
     * The generated files for one mailbox are present and load: its block and item, the crafting
     * recipe, the loot table, and a model whose every face has a real sprite. Datagen writes all of
     * them and they are not in git, so this is also what catches a build made without datagen.
     */
    private void generatedData(ClientGameTestContext ctx, TestServerContext server, String wood, String from)
    {
        Identifier id = Identifier.fromNamespaceAndPath(Constants.MOD_ID, wood + "_mail_box");
        this.check(BuiltInRegistries.BLOCK.containsKey(id) && BuiltInRegistries.ITEM.containsKey(id), "the " + wood + " mailbox block and item exist");
        this.check(server.computeOnServer(s -> s.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, id)).isPresent()), "the " + wood + " mailbox has its crafting recipe (" + from + ")");
        this.check(server.computeOnServer(s -> s.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(Constants.MOD_ID, "blocks/" + wood + "_mail_box"))) != LootTable.EMPTY), "the " + wood + " mailbox has its loot table (" + from + ")");
        this.check(ctx.computeOnClient(mc -> {
            BlockStateModelSet models = mc.getModelManager().getBlockStateModelSet();
            BlockState state = MailboxWoods.block(wood).defaultBlockState();
            BlockStateModel model = models.get(state);
            if(model == models.missingModel() || models.getParticleMaterial(state).sprite().contents().name().equals(MissingTextureAtlasSprite.getLocation()))
                return false;
            // Every face of every part must have a real sprite: a planks texture the wood mod renamed shows up here
            List<BlockStateModelPart> parts = new ArrayList<>();
            model.collectParts(RandomSource.create(42), parts);
            List<Direction> sides = new ArrayList<>(List.of(Direction.values()));
            sides.add(null);
            return !parts.isEmpty() && parts.stream().flatMap(part -> sides.stream().flatMap(side -> part.getQuads(side).stream()))
                .noneMatch(quad -> quad.materialInfo().sprite().contents().name().equals(MissingTextureAtlasSprite.getLocation()));
        }), "the " + wood + " mailbox has a real model and texture (" + from + ")");
    }

    /**
     * With Biomes O' Plenty installed ({@code -PtestBop}): its mailboxes exist with a recipe, a loot
     * table and a real texture, and one takes a name and mail like a vanilla one. Without it, the
     * section is skipped and the check above has already shown that none of them exist.
     */
    private void woodMod(ClientGameTestContext ctx, TestServerContext server)
    {
        if(!FabricLoader.getInstance().isModLoaded("biomesoplenty"))
        {
            Constants.LOG.info("[test] Biomes O' Plenty is not installed; the wood-mod section is skipped (run with -PtestBop)");
            return;
        }
        this.generatedData(ctx, server, "biomesoplenty_fir", "from the built-in pack");
        server.runOnServer(s -> {
            ServerLevel level = s.overworld();
            ServerPlayer player = player(s);
            placeAsPlayer(level, player, COMPAT, MailboxWoods.block("biomesoplenty_fir").defaultBlockState());
            DeliveryService service = DeliveryService.get(s).orElseThrow();
            Mailbox fir = service.getMailboxAtPosition(level, COMPAT).orElseThrow();
            ServerPlayHandler.handleMessageSetMailboxName(new MessageSetMailboxName(COMPAT, "Fir Box"), player);
            this.check(fir.customName().map("Fir Box"::equals).orElse(false), "a fir mailbox takes a name");
            this.check(service.sendMail(fir.id(), PackageItem.create(NonNullList.withSize(1, new ItemStack(Items.EMERALD, 2)), "Fir mail", "Test")).success(), "mail can be sent to the fir mailbox");
            player.teleportTo(level, 0.5, 71, 0.5, Set.of(), 180, 10, false);
        });
        ctx.waitTicks(40);
        this.check(server.computeOnServer(s -> s.overworld().getBlockState(COMPAT).getValue(MailboxBlock.ENABLED)), "the fir mailbox's flag goes up on delivery");
        ctx.takeScreenshot("mighty_mail_bop_fir");
    }

    /** Placing a mailbox makes the player its owner; the naming handler sets its name. */
    private UUID placeAndName(TestServerContext server)
    {
        return server.computeOnServer(s -> {
            ServerLevel level = s.overworld();
            ServerPlayer player = player(s);
            placeAsPlayer(level, player, POST_BOX, ModBlocks.POST_BOX.get().defaultBlockState());
            placeAsPlayer(level, player, MAILBOX, MailboxWoods.block("oak").defaultBlockState());
            DeliveryService service = DeliveryService.get(s).orElseThrow();
            Optional<Mailbox> mailbox = service.getMailboxAtPosition(level, MAILBOX);
            this.check(mailbox.isPresent(), "a placed mailbox is registered with the delivery service");
            this.check(mailbox.flatMap(Mailbox::owner).map(player.getUUID()::equals).orElse(false), "the player who placed it owns it");
            ServerPlayHandler.handleMessageSetMailboxName(new MessageSetMailboxName(MAILBOX, "Test Box"), player);
            this.check(mailbox.flatMap(Mailbox::customName).map("Test Box"::equals).orElse(false), "the naming screen's message names the mailbox");
            this.check(mailbox.flatMap(m -> m.getOwner(s)).map(owner -> owner.name().equals(player.getGameProfile().name())).orElse(false), "the owner's name resolves on the server (for the post box list)");
            return mailbox.map(Mailbox::id).orElse(UUID.randomUUID());
        });
    }

    /** The post box opens its real screen on the client, lists the mailbox, and the send handler posts the package. */
    private void postBox(ClientGameTestContext ctx, TestServerContext server, UUID mailboxId)
    {
        server.runOnServer(s -> use(s.overworld(), player(s), POST_BOX));
        ctx.waitFor(mc -> mc.gui.screen() instanceof PostBoxScreen, 100);
        this.check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof PostBoxScreen), "using the post box opens the post box screen");
        boolean listed = ctx.computeOnClient(mc -> mc.gui.screen() instanceof PostBoxScreen screen && screen.getMenu().getMailboxes().stream()
            .map(IMailbox::getCustomName).anyMatch(name -> name.map("Test Box"::equals).orElse(false)));
        this.check(listed, "the post box lists the mailbox by its name");
        boolean ownerShown = ctx.computeOnClient(mc -> mc.gui.screen() instanceof PostBoxScreen screen && screen.getMenu().getMailboxes().stream()
            .allMatch(box -> box.getOwner(null).isPresent()));
        this.check(ownerShown, "the list carries the owner (face and name) on the client");
        ctx.waitTicks(10);
        ctx.takeScreenshot("mighty_mail_post_box");
        server.runOnServer(s -> {
            ServerPlayer player = player(s);
            PostBoxMenu menu = (PostBoxMenu) player.containerMenu;
            menu.getContainer().setItem(0, new ItemStack(Items.DIAMOND, 3));
            ServerPlayHandler.handleMessageSendPackage(new MessageSendPackage(mailboxId, "Hello from the test"), player, null);
            this.check(menu.getContainer().isEmpty(), "sending empties the post box slots");
            DeliveryService service = DeliveryService.get(s).orElseThrow();
            this.check(service.getMailboxAtPosition(s.overworld(), MAILBOX).map(m -> m.queue().size() == 1).orElse(false), "the package is queued for the mailbox");
            player.closeContainer();
        });
        ctx.waitFor(mc -> !(mc.gui.screen() instanceof PostBoxScreen), 100);
    }

    /** The delivery tick moves the package into the mailbox and raises the flag. */
    private void delivery(ClientGameTestContext ctx, TestServerContext server)
    {
        ctx.waitTicks(40);
        server.runOnServer(s -> {
            ServerLevel level = s.overworld();
            this.check(level.getBlockState(MAILBOX).getValue(MailboxBlock.ENABLED), "the flag goes up when the package arrives");
            MailboxBlockEntity entity = (MailboxBlockEntity) level.getBlockEntity(MAILBOX);
            ItemStack inside = entity != null ? entity.getItem(0) : ItemStack.EMPTY;
            this.check(inside.is(ModItems.PACKAGE.get()), "the mailbox holds a package");
            PackageInfo info = inside.get(ModDataComponents.PACKAGE_INFO.get());
            this.check(info != null && info.sender().map(player(s).getGameProfile().name()::equals).orElse(false), "the package carries the sender's name");
            this.check(info != null && info.message().map("Hello from the test"::equals).orElse(false), "the package carries the message");
            this.check(PackageItem.getPackagedItems(inside).nonEmptyItemCopyStream().anyMatch(stack -> stack.is(Items.DIAMOND) && stack.getCount() == 3), "the package holds the 3 diamonds");
            this.check(DeliveryService.get(s).orElseThrow().getMailboxAtPosition(level, MAILBOX).map(m -> m.queue().isEmpty()).orElse(false), "the queue is empty after delivery");
            // Look at the mailbox from a few blocks away for the screenshot
            player(s).teleportTo(level, 3.5, 71, -4.5, Set.of(), 0, 10, false);
        });
        ctx.waitTicks(15);
        ctx.takeScreenshot("mighty_mail_flag_up");
    }

    /** Opening the mailbox lowers the flag; opening the package drops its items for the player. */
    private void openAndUnpack(ClientGameTestContext ctx, TestServerContext server)
    {
        server.runOnServer(s -> use(s.overworld(), player(s), MAILBOX));
        ctx.waitFor(mc -> mc.gui.screen() != null, 100);
        ctx.waitTicks(5);
        server.runOnServer(s -> {
            ServerLevel level = s.overworld();
            ServerPlayer player = player(s);
            this.check(!level.getBlockState(MAILBOX).getValue(MailboxBlock.ENABLED), "opening the mailbox lowers the flag");
            MailboxBlockEntity entity = (MailboxBlockEntity) level.getBlockEntity(MAILBOX);
            ItemStack pack = entity.removeItem(0, 1);
            player.closeContainer();
            player.getInventory().clearContent();
            player.setItemInHand(InteractionHand.MAIN_HAND, pack);
            pack.getItem().use(level, player, InteractionHand.MAIN_HAND);
        });
        ctx.waitTicks(40);
        this.check(server.computeOnServer(s -> player(s).getInventory().countItem(Items.DIAMOND) == 3), "opening the package gives the player the 3 diamonds");
        this.check(server.computeOnServer(s -> player(s).getInventory().countItem(ModItems.PACKAGE.get()) == 0), "the package is used up");
        ctx.takeScreenshot("mighty_mail_unpacked");
    }

    /** Wrong inputs are refused without side effects. */
    private void wrongInputs(TestServerContext server)
    {
        server.runOnServer(s -> {
            ServerLevel level = s.overworld();
            ServerPlayer player = player(s);
            DeliveryService service = DeliveryService.get(s).orElseThrow();
            Mailbox mailbox = service.getMailboxAtPosition(level, MAILBOX).orElseThrow();
            service.markMailboxAsPendingName(player, level, MAILBOX);
            ServerPlayHandler.handleMessageSetMailboxName(new MessageSetMailboxName(MAILBOX, "x".repeat(40)), player);
            this.check(mailbox.customName().map("Test Box"::equals).orElse(false), "a name longer than 32 characters is refused and the old name stays");
            ServerPlayHandler.handleMessageSetMailboxName(new MessageSetMailboxName(MAILBOX, "Sneaky"), player);
            this.check(mailbox.customName().map("Test Box"::equals).orElse(false), "a rename without the naming screen (no pending name) is refused");
            DeliveryResult result = service.sendMail(UUID.randomUUID(), new ItemStack(Items.BREAD));
            this.check(!result.success(), "sending to a mailbox that does not exist fails");
            this.check(!service.sendMail(mailbox.id(), ItemStack.EMPTY).success(), "sending nothing fails");
        });
    }

    /** A mailbox broken with mail inside drops the mail and leaves the registry. */
    private void breakWithMail(ClientGameTestContext ctx, TestServerContext server)
    {
        server.runOnServer(s -> {
            ServerLevel level = s.overworld();
            ServerPlayer player = player(s);
            placeAsPlayer(level, player, SPARE, MailboxWoods.block("cherry").defaultBlockState());
            DeliveryService service = DeliveryService.get(s).orElseThrow();
            Mailbox spare = service.getMailboxAtPosition(level, SPARE).orElseThrow();
            ItemStack pack = PackageItem.create(NonNullList.withSize(1, new ItemStack(Items.BREAD, 2)), "Spare mail", "Test");
            this.check(service.sendMail(spare.id(), pack).success(), "a package can be sent to a second, unnamed mailbox");
        });
        ctx.waitTicks(40);
        server.runOnServer(s -> {
            ServerLevel level = s.overworld();
            this.check(level.getBlockState(SPARE).getValue(MailboxBlock.ENABLED), "the second mailbox's flag goes up");
            player(s).getInventory().clearContent();
            level.destroyBlock(SPARE, true, player(s));
        });
        ctx.waitTicks(20);
        server.runOnServer(s -> {
            ServerLevel level = s.overworld();
            List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(SPARE).inflate(3));
            long packages = drops.stream().filter(e -> e.getItem().is(ModItems.PACKAGE.get())).count();
            this.check(packages == 1 || player(s).getInventory().countItem(ModItems.PACKAGE.get()) == 1, "breaking a mailbox with mail inside drops the package");
            this.check(DeliveryService.get(s).orElseThrow().getMailboxAtPosition(level, SPARE).isEmpty(), "the broken mailbox is gone from the delivery service");
            this.check(DeliveryService.get(s).orElseThrow().getMailboxAtPosition(level, MAILBOX).isPresent(), "the other mailbox is unaffected");
        });
        this.vanishedBlock(ctx, server);
    }

    /**
     * A mailbox whose block disappears without being broken (a wood mod removed from the world, an
     * editor, a corrupt chunk) is dropped from the delivery service on the next tick while its chunk
     * is loaded, and any mail waiting for it is dropped at its position rather than lost.
     */
    private void vanishedBlock(ClientGameTestContext ctx, TestServerContext server)
    {
        server.runOnServer(s -> {
            ServerLevel level = s.overworld();
            placeAsPlayer(level, player(s), GHOST, MailboxWoods.block("spruce").defaultBlockState());
            DeliveryService service = DeliveryService.get(s).orElseThrow();
            Mailbox ghost = service.getMailboxAtPosition(level, GHOST).orElseThrow();
            // Fill the mailbox so the next package has to wait in the queue
            MailboxBlockEntity entity = (MailboxBlockEntity) level.getBlockEntity(GHOST);
            for(int i = 0; i < entity.getContainerSize(); i++)
                entity.setItem(i, new ItemStack(Items.STONE, 64));
            this.check(service.sendMail(ghost.id(), PackageItem.create(NonNullList.withSize(1, new ItemStack(Items.APPLE)), null, "Test")).success(), "a package queues for a full mailbox");
        });
        ctx.waitTicks(10);
        server.runOnServer(s -> {
            ServerLevel level = s.overworld();
            this.check(DeliveryService.get(s).orElseThrow().getMailboxAtPosition(level, GHOST).map(m -> m.queue().size() == 1).orElse(false), "the package waits in the queue while the mailbox is full");
            // The block and its block entity vanish without the normal break path
            level.getChunkAt(GHOST).removeBlockEntity(GHOST);
            level.setBlock(GHOST, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        });
        ctx.waitTicks(10);
        server.runOnServer(s -> {
            ServerLevel level = s.overworld();
            this.check(DeliveryService.get(s).orElseThrow().getMailboxAtPosition(level, GHOST).isEmpty(), "a mailbox whose block vanished is dropped from the delivery service");
            boolean dropped = level.getEntitiesOfClass(ItemEntity.class, new AABB(GHOST).inflate(3)).stream().anyMatch(e -> e.getItem().is(ModItems.PACKAGE.get()));
            this.check(dropped, "the mail that was waiting for it is dropped at its position, not lost");
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

    private void check(boolean ok, String what)
    {
        if(ok)
        {
            Constants.LOG.info("[test] ok: {}", what);
        }
        else
        {
            Constants.LOG.error("[test] FAILED: {}", what);
            this.failures.add(what);
        }
    }
}
