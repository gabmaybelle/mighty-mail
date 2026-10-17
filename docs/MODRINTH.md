# Modrinth listing

Everything to paste when creating the Modrinth project. Gabby uploads; nothing here is published by
itself. The jars come from the GitHub releases (`v26.3-1.2.1+port.1`, `v26.3-1.2.1+port.2`), not from a
local build.

## Checklist

- [ ] Project name: **Mighty Mail: Unofficial Port**. Slug suggestion: `mighty-mail-unofficial-port`.
- [ ] Tick the **AI-generated content** disclosure (the port's code changes were made with Claude Code).
- [ ] Licence: MIT. Source: https://github.com/gabmaybelle/mighty-mail. Issues: the repo's issue tracker.
- [ ] Links to the original: https://github.com/MrCrayfish/MightyMail and MrCrayfish's page https://mrcrayfish.com/mods?id=mighty_mail.
- [ ] Dependencies: **Fabric API** (required, Fabric only). **Framework** is required on both loaders but is only on CurseForge, so Modrinth can't link it: the description tells players to get it there.
- [ ] Two versions per release: the Fabric jar and the NeoForge jar, each tagged with its loader and Minecraft 26.3.
- [x] Real screenshots from a normal world, in `docs/screenshots/`: `mailboxes-by-the-lake.png` (gallery lead), `post-box.png`, `post-box-sent.png`, `naming-a-mailbox.png`, `mailbox-with-packages.png`. 1600x900, no player names in frame.
- [ ] Gallery/icon: the package icon from the jar is fine; no MrCrayfish branding.
- [ ] Jars to upload: the two jars attached to each GitHub release (not the `-sources` or `-javadoc` jars).
- [x] Messaging MrCrayfish: a note was drafted; sending it is optional.

## Form fields

| Field | Value |
|---|---|
| Name | Mighty Mail: Unofficial Port |
| Summary | Unofficial port of MrCrayfish's Mighty Mail to Minecraft 26.3 for Fabric and NeoForge: a post box and mailboxes for sending items and messages to other players. |
| Categories | Decoration, Utility, Social (pick up to three; "Social" fits the sending-to-players part) |
| Loaders | Fabric, NeoForge |
| Game version | 26.3 |
| Environment | Client and server (both required) |
| Licence | MIT |
| Source | https://github.com/gabmaybelle/mighty-mail |
| Issues | https://github.com/gabmaybelle/mighty-mail/issues |
| Dependencies | Framework (required), Fabric API (required on Fabric only) |
| Gallery lead | `docs/screenshots/mailboxes-by-the-lake.png` |

## Description (Markdown)

```markdown
# Mighty Mail: Unofficial Port

An **unofficial** port of [Mighty Mail](https://github.com/MrCrayfish/MightyMail) by [MrCrayfish](https://mrcrayfish.com) to Minecraft **26.3**, for **Fabric** and **NeoForge**.

MrCrayfish wrote the mod. This project only carries it forward to a newer Minecraft version. It is not affiliated with or endorsed by him, and he is not responsible for anything here. If the original mod gets its own 26.3 release, use that. Problems with the port go to [this project's issues](https://github.com/gabmaybelle/mighty-mail/issues).

![Mailboxes in several wood types and a post box on a hillside by a lake](https://raw.githubusercontent.com/gabmaybelle/mighty-mail/26.3/docs/screenshots/mailboxes-by-the-lake.png)

## What it does

Mighty Mail adds a post box and mailboxes in every wood type. Put items in the post box, pick a mailbox from the list, add an optional message, and a package is delivered to that mailbox, even in another dimension. A little red flag shows when mail is waiting. Server owners can change the mailbox size, the delivery queue limit, banned items and allowed dimensions in the config.

With Biomes O' Plenty, Ecologics or Wilder Wild installed, their woods get mailboxes too, crafted the same way from that wood's planks and fence.

![The post box screen: a list of mailboxes, a message and the items to send](https://raw.githubusercontent.com/gabmaybelle/mighty-mail/26.3/docs/screenshots/post-box.png)

## Requirements

- Minecraft 26.3
- [Framework](https://www.curseforge.com/minecraft/mc-mods/framework) 0.13.27 or newer (MrCrayfish's library mod, from CurseForge; the Modrinth app can't add it for you)
- On Fabric: [Fabric API](https://modrinth.com/mod/fabric-api)

Install the file for your loader together with Framework (and Fabric API on Fabric).

## Upgrading a world from the original mod

The mod id is still `mighty_mail`, so the port is a drop-in replacement. The first time a world from 1.21.10 or older starts with this port, its mailbox registry (owners, names and any queued mail) is imported automatically, and the old file is left in place as a backup.

## Config

`config/mighty_mail.server.toml` is created on first start (server side, synced to clients): mailboxes per player, rows of slots in a mailbox, the delivery queue size, whether items with inventories may be sent, banned items, and allowed dimensions.

## Credits and licence

- Mighty Mail, its code, models and textures: [MrCrayfish](https://github.com/MrCrayfish), MIT licence, Copyright (c) 2023 MrCrayfish.
- Framework: MrCrayfish, LGPL-2.1. Separate download, not bundled.
- The 26.3 port was made by Gabby with help from Claude, an AI assistant. The port keeps the MIT licence. Source: https://github.com/gabmaybelle/mighty-mail
```

## Version notes

Version number on Modrinth: `1.2.1+port.1` and `1.2.1+port.2` (the same as the jars). Version title:
"Mighty Mail: Unofficial Port 1.2.1+port.N". Release channel: Release. One version entry per loader,
same notes on both.

### 1.2.1+port.1

```markdown
The first release of the unofficial port. It is Mighty Mail 1.2.1 by MrCrayfish, brought to Minecraft 26.3 for Fabric and NeoForge.

- Everything the original does: a post box, mailboxes in every wood type, packages with messages, delivery across dimensions, the red flag for waiting mail, and the server config options.
- Worlds coming from the original mod on 1.21.10 or older keep their mailboxes, names, owners and queued mail: the port imports the old mailbox registry the first time the world starts.
- The mod id is unchanged (`mighty_mail`), so the port is a drop-in replacement.

Needs Framework 0.13.27 or newer, and Fabric API on Fabric.
```

### 1.2.1+port.2

```markdown
Mailboxes for the woods of Biomes O' Plenty, Ecologics and Wilder Wild. They appear only when that mod is installed, with the usual recipe (that wood's planks, a chest and its fence) and a look made from the wood's planks. Nothing changes for worlds without those mods.

Woods covered: Biomes O' Plenty (dead, empyreal, fir, hellbark, jacaranda, magic, mahogany, maple, origin oak, palm, pine, redwood, umbran, willow), Ecologics (azalea, coconut, flowering azalea, maple, walnut), Wilder Wild (baobab, cypress, maple, palm, willow).

Replaces 1.2.1+port.1; existing worlds carry on as they are. Needs Framework 0.13.27 or newer, and Fabric API on Fabric.
```
