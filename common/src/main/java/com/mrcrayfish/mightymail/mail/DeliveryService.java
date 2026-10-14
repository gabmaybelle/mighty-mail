package com.mrcrayfish.mightymail.mail;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mrcrayfish.mightymail.Config;
import com.mrcrayfish.mightymail.Constants;
import com.mrcrayfish.mightymail.blockentity.MailboxBlockEntity;
import com.mrcrayfish.mightymail.util.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.SavedDataStorage;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Author: MrCrayfish
 */
public class DeliveryService extends SavedData
{
    private static final Codec<DeliveryService> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
        Mailbox.CODEC.listOf().fieldOf("Mailboxes").forGetter(service -> List.copyOf(service.mailboxes.values()))
    ).apply(instance, mailboxes -> {
        DeliveryService service = new DeliveryService();
        mailboxes.forEach(service::addMailbox);
        return service;
    }));

    @SuppressWarnings("DataFlowIssue")
    // PORT-26.3: saved data is now keyed by an Identifier, so the file moved from
    // data/mighty_mail_delivery_service.dat (1.21.10 and older) to data/mighty_mail/delivery_service.dat.
    // Vanilla never reads the old location, so get() imports it once; see importLegacyData.
    private static final SavedDataType<DeliveryService> TYPE = new SavedDataType<>(Utils.resource("delivery_service"), DeliveryService::new, CODEC, null);
    private static final String LEGACY_FILE_NAME = "mighty_mail_delivery_service.dat";

    public static Optional<DeliveryService> get(MinecraftServer server)
    {
        ServerLevel level = server.getLevel(Level.OVERWORLD);
        if(level != null)
        {
            SavedDataStorage storage = level.getDataStorage();
            DeliveryService service = storage.get(TYPE);
            if(service == null)
            {
                // Nothing saved under the 26.3 name yet: carry over a pre-26.3 file if there is one
                service = importLegacyData(server).orElseGet(DeliveryService::new);
                storage.set(TYPE, service); // marks it dirty, so it is written to the new file on the next save
            }
            return Optional.of(service);
        }
        return Optional.empty();
    }

    /**
     * PORT-26.3: reads the delivery service saved by Mighty Mail on Minecraft 1.21.10 and older,
     * which lived at data/mighty_mail_delivery_service.dat. The old file is left in place as a
     * backup; once the new file exists this method is never called again for that world.
     */
    private static Optional<DeliveryService> importLegacyData(MinecraftServer server)
    {
        Path legacyFile = server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(LEGACY_FILE_NAME);
        if(!Files.isRegularFile(legacyFile))
            return Optional.empty();

        try
        {
            CompoundTag tag = NbtIo.readCompressed(legacyFile, NbtAccounter.unlimitedHeap());
            RegistryOps<Tag> ops = server.registryAccess().createSerializationContext(NbtOps.INSTANCE);
            Optional<DeliveryService> service = CODEC.parse(ops, tag.get("data"))
                .resultOrPartial(error -> Constants.LOG.error("Failed to import pre-26.3 delivery service data from {}: {}", legacyFile, error));
            service.ifPresent(s -> Constants.LOG.info("Imported pre-26.3 delivery service data ({} mailboxes) from {}", s.mailboxes.size(), legacyFile));
            return service;
        }
        catch(IOException e)
        {
            Constants.LOG.error("Failed to read pre-26.3 delivery service data from {}", legacyFile, e);
            return Optional.empty();
        }
    }

    private final Map<Pair<Identifier, BlockPos>, Mailbox> locator = new HashMap<>();
    private final Map<UUID, Mailbox> mailboxes = new ConcurrentHashMap<>();
    private final Queue<Mailbox> removal = new ArrayDeque<>();
    private final Map<UUID, Pair<Identifier, BlockPos>> pendingNames = new HashMap<>();

    /**
     * Called every tick on the logical server
     */
    public void serverTick(MinecraftServer server)
    {
        // Checks for mailboxes that need to be removed and spawn their queue into the level
        while(!this.removal.isEmpty())
        {
            Mailbox mailbox = this.removal.poll();
            mailbox.spawnQueueIntoLevel(server);
            this.mailboxes.remove(mailbox.id());
            this.locator.remove(Pair.of(mailbox.levelKey().identifier(), mailbox.pos()));
            this.setDirty();
        }

        // Try to deliver mail from queues to the block entity in the level
        this.mailboxes.forEach((uuid, mailbox) -> mailbox.tick(this, server));
    }

    /**
     * Adds an ItemStack to the queue of the mailbox matching the given id.
     *
     * @param id the identifier of the mailbox
     * @param stack the ItemStack to send
     * @return An optional string. If an error occurs, the optional will contain a translation key
     */
    public DeliveryResult sendMail(UUID id, ItemStack stack)
    {
        if(stack.isEmpty())
            return DeliveryResult.createFail("delivery_service.invalid_item");

        Mailbox mailbox = this.mailboxes.get(id);

        // Check if the mailbox exists
        if(mailbox == null)
            return DeliveryResult.createFail(Utils.translationKey("gui", "delivery_service.unknown_mailbox"));

        // Check if the queue is not full
        if(mailbox.queue().size() >= Config.SERVER.deliveryQueueSize.get())
            return DeliveryResult.createFail(Utils.translationKey("gui", "delivery_service.mailbox_queue_full"));

        // Check if mailbox is in a deliverable dimension
        if(!isDeliverableDimension(mailbox.levelKey()))
            return DeliveryResult.createFail(Utils.translationKey("gui", "delivery_service.undeliverable_dimension"));

        // Push to queue and mark as dirty to ensure it's saved
        mailbox.queue().offer(stack);
        this.setDirty();

        // Empty optional means successful
        return DeliveryResult.createSuccess(Utils.translationKey("gui", "delivery_service.package_sent"));
    }

    /**
     * Marks the given mailbox for removal
     *
     * @param mailbox the mailbox to remove
     */
    void removeMailbox(Mailbox mailbox)
    {
        this.removal.offer(mailbox);
    }

    /**
     * Marks the given mailbox for removal
     *
     * @param mailboxId the id of the mailbox to remove
     */
    public void removeMailbox(UUID mailboxId)
    {
        Mailbox mailbox = this.mailboxes.get(mailboxId);
        if(mailbox != null)
        {
            this.removal.offer(mailbox);
        }
    }

    /**
     * Determines if the given player can create/place a mailbox. Mailboxes are limited per player,
     * as specified by a maximum count in the config.
     *
     * @param player the player to test
     * @return True if the player can place a mailbox
     */
    public boolean canCreateMailbox(Player player)
    {
        long count = this.mailboxes.values().stream().filter(box -> {
            return box.owner().stream().anyMatch(uuid -> uuid.equals(player.getUUID()));
        }).count();
        return Config.SERVER.maxMailboxesPerPlayer.get() > count;
    }

    /**
     * Gets an existing or creates a new mailbox for the given mailbox block entity. This method is
     * responsible for registering mailboxes into the delivery system and is called when a player
     * placing a new mailbox block. The mailbox is initially unclaimed but is immediately claimed
     * by the placing player.
     *
     * @param entity the mailbox block entity
     * @return A {@link Mailbox} instance or null if block entity is invalid (aka removed)
     */
    public Mailbox getOrCreateMailBox(MailboxBlockEntity entity)
    {
        if(entity.isRemoved())
            return null;

        this.duplicateIdCheck(entity);

        Mailbox mailbox = this.mailboxes.get(entity.getId());
        if(mailbox != null)
            return this.removal.contains(mailbox) ? null : mailbox;

        ResourceKey<Level> levelKey = entity.getLevel().dimension();
        BlockPos pos = entity.getBlockPos();
        return this.addMailbox(new Mailbox(entity.getId(), levelKey, pos));
    }

    private Mailbox addMailbox(Mailbox mailbox)
    {
        this.mailboxes.put(mailbox.id(), mailbox);
        this.locator.put(Pair.of(mailbox.levelKey().identifier(), mailbox.pos()), mailbox);
        mailbox.setService(this);
        this.setDirty();
        return mailbox;
    }

    /**
     * Regenerates the id of the mailbox block entity if another mailbox with that id already exists.
     * A collision with a random UUID is very unlikely, however a player can pick a block with NBT
     * and place down a mailbox with the same UUID. Naturally, regenerating the UUID won't affect
     * the mailbox that already exists with the UUID that collided with the given blockEntity arg.
     *
     * @param blockEntity the mailbox block entity to check
     */
    private void duplicateIdCheck(MailboxBlockEntity blockEntity)
    {
        Mailbox box = this.mailboxes.get(blockEntity.getId());
        if(box != null && !box.pos().equals(blockEntity.getBlockPos()))
        {
            blockEntity.regenerateId();
        }
    }

    /**
     * Gets the mailbox at the given block position in the level. If no mailbox exists,
     * an empty optional will be returned.
     *
     * @param level the level where the mailbox exists
     * @param pos the block position of the mailbox
     * @return an optional mailbox
     */
    public Optional<Mailbox> getMailboxAtPosition(Level level, BlockPos pos)
    {
        return Optional.ofNullable(this.locator.get(Pair.of(level.dimension().identifier(), pos)));
    }

    /**
     * Marks a mailbox as expecting to be renamed in the future. As mailboxes are designed to only
     * be named when initially placing them down, we don't want to allow the ability to rename the
     * mailbox again. To prevent this, the player and the location of the mailbox is recorded upon
     * placing a mailbox as a valid mailbox that can be renamed.
     *
     * @param player the player who placed the mailbox
     * @param level  the level that contains the mailbox
     * @param pos    the block position of the mailbox
     */
    public void markMailboxAsPendingName(Player player, Level level, BlockPos pos)
    {
        this.pendingNames.put(player.getUUID(), Pair.of(level.dimension().identifier(), pos));
    }

    /**
     * Renames the mailbox at the given position with the custom name. This method will
     * not rename the mailbox if any of these conditions are true, the player is not the owner
     * of the mailbox, the level/pos combination doesn't link to a mailbox in the level, or
     * the mailbox is not expecting to be renamed. The custom name can also not be longer
     * than 32 characters.
     *
     * @param player     the player owner of the mailbox
     * @param level      the level the mailbox is located
     * @param pos        the block position of the mailbox
     * @param customName the new name for the mailbox
     * @return True if the mailbox was successfully renamed
     */
    public boolean renameMailbox(Player player, Level level, BlockPos pos, String customName)
    {
        Pair<Identifier, BlockPos> pendingLocation = this.pendingNames.remove(player.getUUID());
        return this.getMailboxAtPosition(level, pos).map(mailbox -> {
            if(!Objects.equals(mailbox.owner().orElse(null), player.getUUID()))
                return false;
            Pair<Identifier, BlockPos> location = Pair.of(level.dimension().identifier(), pos);
            return Objects.equals(location, pendingLocation) && mailbox.rename(customName);
        }).orElse(false);
    }

    public Collection<IMailbox> getMailboxes()
    {
        return Collections.unmodifiableCollection(this.mailboxes.values());
    }

    /**
     * Creates a ResourceKey for a level with the given key. This method checks for vanilla keys
     * since a reference for them already exists.
     *
     * @param levelKey a resource location (as a string) of the level key
     * @return A resource key of the level or null if the key was invalid
     */
    @Nullable
    private static ResourceKey<Level> createLevelKey(String levelKey)
    {
        return levelKey.isBlank() ? null : switch(levelKey)
        {
            case "minecraft:overworld" -> Level.OVERWORLD;
            case "minecraft:the_nether" -> Level.NETHER;
            case "minecraft:the_end" -> Level.END;
            default -> ResourceKey.create(Registries.DIMENSION, Identifier.parse(levelKey));
        };
    }

    /**
     * Determines if the given ItemStack is an item that can't be sent through mail. By default,
     * items that have inventory are banned (such as Shulker Boxes) unless explicitly disabled in
     * mod's configuration. An item is also blocked if the item id is contained in the banned items
     * list, which again is defined in the mod's configuration. Banned items are mainly to prevent
     * creating large NBT on a single item, which can affect servers/world saves.
     * <p>
     * This method can only be called while in a game/server since it depends on a configuration
     * sent from the server.
     *
     * @param stack the ItemStack to check if it is banned
     * @return True if the ItemStack is banned
     */
    public static boolean isBannedItem(ItemStack stack)
    {
        // Check if the item can fit inside container items
        if(Config.SERVER.banSendingItemsWithInventories.get() && !stack.getItem().canFitInsideContainerItems())
        {
            return true;
        }

        // Check if the item is not on the banned item list
        String name = stack.getItem().getDescriptionId();
        return Config.SERVER.bannedItems.get().contains(name);
    }

    /**
     * Determines if the given level allows mailboxes to be placed
     *
     * @param level the level to test
     * @return True if mailboxes are allowed to be placed
     */
    public static boolean isDeliverableDimension(Level level)
    {
        return isDeliverableDimension(level.dimension());
    }

    /**
     * Determines if the given level allows mailboxes to be placed
     *
     * @param key the level key to test
     * @return True if mailboxes are allowed to be placed
     */
    public static boolean isDeliverableDimension(ResourceKey<Level> key)
    {
        List<String> validDimensions = Config.SERVER.allowedDimensions.get();
        if(!validDimensions.isEmpty())
        {
            return validDimensions.contains(key.identifier().toString());
        }
        return true;
    }
}