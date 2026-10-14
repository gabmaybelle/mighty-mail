# Mighty Mail: Unofficial Port

An **unofficial** port of [Mighty Mail](https://github.com/MrCrayfish/MightyMail) by
[MrCrayfish](https://mrcrayfish.com) to Minecraft **26.3**, for **Fabric** and **NeoForge**.

MrCrayfish wrote the mod. This repository only carries it forward to a newer Minecraft version.
It is not affiliated with or endorsed by him, and the original author is not responsible for
anything here. If the original mod gets its own 26.3 release, use that.

## What it does

Mighty Mail adds a post box and mailboxes in every wood type. Put items in the post box, pick a
mailbox from the list, add an optional message, and a package is delivered to that mailbox, even
in another dimension. A little red flag shows when mail is waiting. Server owners can change the
mailbox size, the delivery queue limit, banned items and allowed dimensions in the config.

## Requirements

- Minecraft 26.3
- [Framework](https://github.com/MrCrayfish/Framework) 0.13.27 or newer (MrCrayfish's library mod)
- On Fabric: [Fabric API](https://github.com/FabricMC/fabric)

Install the jar for your loader from the releases page together with Framework (and Fabric API on
Fabric). The Fabric and NeoForge jars are separate files.

## Upgrading a world from the original mod

The mod id is still `mighty_mail`, so the port is a drop-in replacement. Minecraft 26.x changed
where saved data lives; the first time a world from 1.21.10 or older starts with this port, its
mailbox registry (owners, names and any queued mail) is imported from the old file automatically
and the old file is left in place as a backup.

## Config

`config/mighty_mail.server.toml` is created on first start (server side, synced to clients):

| Option | Default | Meaning |
|---|---|---|
| `maxMailboxesPerPlayer` | 16 | mailboxes a player may own |
| `mailboxInventoryRows` | 1 | rows of slots in a mailbox (1 to 6) |
| `mailQueueSize` | 18 | packages that may wait for one mailbox |
| `banSendingItemsWithInventories` | true | refuse shulker boxes and the like |
| `bannedItems` | `[]` | item ids that may not be sent |
| `allowedDimensions` | `[]` | dimensions mailboxes may be placed in; empty means all |

## Building

```
./gradlew :fabric:build :neoforge:build
```

Needs Java 25. Framework comes from MrCrayfish's GitHub Packages Maven, which requires a GitHub
token with `read:packages` in the `GPR_USER` and `GPR_KEY` environment variables (or `gpr.user`
and `gpr.key` in your user `gradle.properties`, never in the repo). The porting log and the plan
are in [MIGHTY-MAIL-PORTING.md](MIGHTY-MAIL-PORTING.md).

## Credits and licence

- Mighty Mail, its code, models and textures: [MrCrayfish](https://github.com/MrCrayfish),
  MIT licence, Copyright (c) 2023 MrCrayfish. See [LICENSE](LICENSE).
- Framework: MrCrayfish, LGPL-2.1. It is a separate download and is not bundled here.
- The 26.3 port was made by Gabby with help from Claude, an AI assistant. The port keeps the
  MIT licence.
