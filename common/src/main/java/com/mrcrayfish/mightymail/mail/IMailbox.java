package com.mrcrayfish.mightymail.mail;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Author: MrCrayfish
 */
public interface IMailbox
{
    UUID getId();

    /**
     * The name and id of the player that owns the mailbox or empty optional. The server is needed
     * to look the name up; on the client (or when null) only a cached value can be returned.
     */
    Optional<NameAndId> getOwner(@Nullable MinecraftServer server);

    default Optional<NameAndId> getOwner()
    {
        return this.getOwner(null);
    }

    Optional<String> getCustomName();
}
