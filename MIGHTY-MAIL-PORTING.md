# Porting Mighty Mail to Minecraft 26.3 (Fabric + NeoForge)

Unofficial port of Mighty Mail by MrCrayfish. Public on GitHub. A small warm-up project before Botania Bifrost.

This file is the plan and the log. Claude Code should read it at the start of every session and update the log at the end.

---

## Licence

- Mighty Mail is MIT. Keep `LICENSE` and the "Copyright (c) 2023 MrCrayfish" notice in every copy.
- Credit MrCrayfish in the README and mod metadata, and say clearly that this is an unofficial port.
- Framework (required library) is LGPL-2.1. Depend on it as a separate mod; don't bundle it inside the jar.
- MrCrayfish's Furniture Mod: Refurbished is also MIT. Its code may be used as a reference, and any code copied from it keeps its MIT notice ("Copyright (c) 2024 MrCrayfish").

---

## Starting point

Upstream: https://github.com/MrCrayfish/MightyMail, branch `multiloader/1.21.10` (last commit 16 Oct 2025, mod version 1.2.1).

| Area | What's there |
|---|---|
| Minecraft / Java | 1.21.10, Java 21 |
| Layout | `common`, `fabric`, `neoforge`, with `buildSrc` multiloader scripts |
| Code size | common 50 Java files, fabric 4, neoforge 3; no mixins; 15 textures |
| Framework | `0.13.1`, from GitHub Packages (`maven.pkg.github.com/MrCrayfish/Maven`) |
| Loaders | Fabric API `0.135.0+1.21.10`, NeoForge `21.10.16-beta` |

## Reference implementation

MrCrayfish's Furniture Mod: Refurbished contains the same mail system, already updated to 26.2.

- Repo: https://github.com/MrCrayfish/MrCrayfishFurnitureMod-Refurbished
- Compare branch `1.21.10` with branch `26.2` to see exactly how each file changed.
- Between those branches, about 218 of roughly 2,000 lines of mail code changed.

| Mighty Mail (`com/mrcrayfish/mightymail/`) | Refurbished (`com/mrcrayfish/furniture/refurbished/`) |
|---|---|
| `mail/*` (DeliveryService, Mailbox, IMailbox, PackageInfo, DeliveryResult) | `mail/*` |
| `blockentity/MailboxBlockEntity`, `PostBoxBlockEntity` | `blockentity/` same names |
| `block/MailboxBlock`, `PostBoxBlock` | `block/` same names |
| `item/PackageItem`, `MailboxItem` | `item/` same names |
| `inventory/PostBoxMenu`, `PostBoxSlot` | `inventory/PostBoxMenu`, `inventory/slot/PostBoxSlot` |
| `client/gui/screen/PostBoxScreen` | `client/gui/screen/PostBoxScreen` |
| `client/ClientMailbox` | `client/ClientMailbox` |
| `network/message/MessageSendPackage`, `MessageShowDeliveryResult` | same names |
| `network/message/MessageSetMailboxName` | `network/message/MessageNameMailbox` |

Main kinds of change seen in Refurbished's 26.x mail code:
- `ResourceLocation` renamed to `Identifier`.
- Saved world data (`SavedDataType`) registered more simply; `DeliveryService` no longer holds a server reference.
- GUI drawing moved from `GuiGraphics` to `GuiGraphicsExtractor`. `PostBoxScreen` has the most changes.
- `IMailbox.getOwner()` takes an optional server for owner lookup.

---

## Target toolchain (checked 14 Oct 2026)

| Piece | Version |
|---|---|
| Minecraft | 26.3 |
| Java | 25 |
| NeoForge | `26.3.0.77` |
| Fabric Loader | `0.19.5` |
| Fabric API | `0.163.0+26.3` |
| Framework | `0.13.27` for 26.3, Fabric and NeoForge (released 4 Oct 2026) |
| Build plugins | Fabric Loom `1.18.4` (plugin id `net.fabricmc.fabric-loom`), ModDevGradle `2.0.148` for both `common` and `neoforge`, Gradle `9.7.1`, foojay resolver `1.0.0` |
| Refurbished Furniture | no 26.3 build exists; newest is `1.0.25+26.2`. Compile-only against that jar for now (`PORT-26.3` markers in `gradle.properties` and the three build scripts). |

Re-checked 14 Oct 2026 at the start of Phase 1: everything above confirmed against maven.fabricmc.net, maven.neoforged.net, MrCrayfish's GitHub Maven and services.gradle.org. NeoForge 26.3.0.77 is a full release, not a beta. Gradle 9.8.1 exists but 9.7.1 is what the other 26.3 mods on this machine use.

### Getting Framework

Upstream downloads Framework from GitHub Packages, which needs a token even for public packages.

- **Option A (upstream's way):** create a GitHub personal access token (classic) with only `read:packages`. Put it in `~/.gradle/gradle.properties` on your computer, never in the repo.
- **Option B:** switch the dependency to CurseMaven (`curse.maven:framework-549225:<file id>`), which needs no token.

Pick one in Phase 0 and record it below.

**Chosen (2026-10-14): Option A, using the GitHub CLI's login instead of a hand-made token.** `gh auth refresh -s read:packages` added the `read:packages` scope to the existing `gh` login (device-code flow, approved by Gabby). Nothing is stored in `~/.gradle/gradle.properties`; each build passes the token in through the `GPR_USER` / `GPR_KEY` environment variables that `buildSrc/multiloader-common.gradle` already reads. The same token also covers the optional Refurbished Furniture dependency, which lives on the same Maven.

### Building locally

Since Phase 1 the build runs on the system Java 25 (Temurin, the default `java`). In PowerShell:

```powershell
$env:GPR_USER = "gabmaybelle"
$env:GPR_KEY = (gh auth token)
.\gradlew :fabric:build :neoforge:build
```

Swap the last line for `.\gradlew :fabric:runClient` or `.\gradlew :neoforge:runClient` to launch a dev client, or `:fabric:compileJava :neoforge:compileJava --continue` to just see compiler errors. (Phase 0, on the old Gradle 8.14, needed `JAVA_HOME` pointed at Gradle's auto-provisioned Java 21 under `~/.gradle/jdks`; its first build took about 45 minutes.)

---

## Phases

### Phase 0: Baseline
1. Fork upstream as `mighty-mail` (rename later if you choose a new public name). Create branch `26.3` from `multiloader/1.21.10`.
2. Set up Framework access (Option A or B).
3. Build both loaders on Java 21 with no changes. Launch each once.

**Done when:** the untouched 1.21.10 version builds and runs on both loaders.

### Phase 1: Toolchain
1. Update versions to the target table, Java 25, Framework 0.13.27.
2. Update the build plugins in `buildSrc` as needed.

**Done when:** Gradle syncs and starts compiling (errors in Java code are expected).

### Phase 2: Code
1. Port one package at a time: `mail`, then `blockentity` and `block`, `item`, `inventory`, `network`, `client`, `datagen`, `core`, `platform`.
2. For each file with a Refurbished counterpart, compare Refurbished's `1.21.10` and `26.2` versions and apply the same kind of change. Then fix anything 26.2 → 26.3 changed.
3. Rerun datagen.

**Done when:** both loaders build, the game reaches a world, and the post box and mailboxes appear in the creative tab.

### Phase 3: Testing
Run each on Fabric and on NeoForge, on a dedicated server with two players:
- [ ] Place a post box and mailboxes in several wood types.
- [ ] Name a mailbox; it appears in the post box list.
- [ ] Send items with a message; the red flag appears; items and message arrive intact.
- [ ] Send to a mailbox in another dimension.
- [ ] Fill a mailbox; check the queue limit and the delivery result message.
- [ ] Restart the server with mail queued; nothing is lost.
- [ ] Break a mailbox with mail inside; items drop and nothing duplicates.
- [ ] Config options (mailbox size, queue limit) still work.
- [ ] Logs are free of repeated errors and missing textures.

### Phase 4: Publishing
- [ ] Choose a public name that doesn't suggest it's official.
- [ ] README: unofficial notice, credit to MrCrayfish, links to the original mod and Framework.
- [ ] Modrinth: tick the AI-generated content disclosure; write the description yourself; use real screenshots; list Framework and Fabric API as dependencies; upload Fabric and NeoForge jars as separate versions.
- [ ] Consider messaging MrCrayfish before publishing.

---

## Working rules for Claude Code

- Read this file first. One phase or one package per session.
- Small commits with clear messages. No refactors beyond what porting needs.
- Mark temporary workarounds with `// PORT-26.3:` and a short reason.
- Never commit tokens or `gradle.properties` files containing credentials.
- Explain changes in plain language; Gabby is learning the 26.x toolchain through this project.
- End each session by updating the log.

---

## Log

| Date | Phase | Notes |
|---|---|---|
| 2026-10-14 | Planning | Inspected upstream `multiloader/1.21.10` and Refurbished `1.21.10` vs `26.2` mail code. Confirmed Framework 0.13.27 exists for 26.3. |
| 2026-10-14 | Phase 0 | Forked upstream as `gabmaybelle/mighty-mail`, cloned to `mods-dev/mighty-mail`, added `upstream` remote, branch `26.3` from `upstream/multiloader/1.21.10` (`8a7ee32`). Framework access: Option A via `gh` login with `read:packages` (see above). Built `:fabric:build` and `:neoforge:build` on Java 21 with no code changes: both succeed (jars `mighty_mail-fabric-1.21.10-1.2.1.jar`, `mighty_mail-neoforge-1.21.10-1.2.1.jar`). Launched each dev client once; both reach the title screen, Framework 0.13.1 and Mighty Mail 1.2.1 load. Expected noise in the dev logs: "Missing model" / "No model loaded" warnings because block and item models come from datagen and `src/generated` is not in git (run datagen in Phase 2); Fabric warns that Refurbished Furniture "conflicts" with Mighty Mail (that mod's own metadata; it is only on the classpath in dev runs); one javadoc warning about a stale `Item#appendHoverText` reference in `ScreenHelper`. Phase 0 done. |
| 2026-10-14 | Phase 1 | Re-checked every version (see table). Toolchain bumped: Java 25, MC 26.3, NeoForm `26.3-1`, Fabric Loader 0.19.5 / API 0.163.0+26.3, NeoForge 26.3.0.77, Framework 0.13.27, Gradle 9.7.1, Loom 1.18.4, ModDevGradle 2.0.148 (replaces NeoGradle in `neoforge`, as MrCrayfish did for Refurbished 26.2; `neoforge/build.gradle` rewritten from his 26.2 version). Framework coordinates are now `<version>+<mc>`. Three toolchain-only fixes found by compiling: Loom rejects `officialMojangMappings()` on an unobfuscated game (line removed); the access widener header must say `official` not `named`; the Refurbished Furniture 26.2 package needs `requireCapability("com.mrcrayfish:refurbished_furniture")` because it publishes capability names with the MC version in them. Result: Gradle syncs and `:common:compileJava` runs against 26.3 and stops at javac's 100-error cap, all in mod code. Error map for Phase 2: `ResourceLocation` → `Identifier` (about a third of all errors, 17 files); `GuiGraphics` gone from `PostBoxScreen`, `TextInputScreen`, `IconButton` (drawing API change); `DeliveryService` saved-data API (15 errors); datagen providers (`BlockStatesGen` `Variant`, `BlockTagGen` `IntrinsicHolderTagsProvider`/`tag()`, `RecipeGen`, `LootTableGen`, `ItemModelsGen`); `Block.simpleCodec` in both blocks; `CreativeModeTab.Output` access; `MigrateCommand` `hasPermission`; `Utils`/`ScreenHooks` small bits (`Util`, `GLFW`, `setScreen`). Phase 1 done. |
| 2026-10-14 | Phase 2: `mail` | Reference clone of Refurbished Furniture at `mods-dev/refurbished-furniture-ref` (blobless; `git diff origin/1.21.10 origin/26.2 -- <path>`; no 26.3 branch exists yet). Applied its `mail` diff: `DeliveryService` no longer holds the server (`serverTick(server)`, `Mailbox.tick(service, server)`, `spawnQueueIntoLevel(server)`), codec is a plain constant, `SavedDataType` takes an `Identifier` (`mighty_mail:delivery_service`), `ResourceLocation` → `Identifier` and `.location()` → `.identifier()`; `IMailbox.getOwner(@Nullable server)` with a no-arg default; `Utils.resource` returns `Identifier` (name kept). Touched outside the package only where the interface forced it: `Bootstrap` tick hook, `ClientMailbox.getOwner` signature. `mail` + `util` compile clean; common is down to 88 errors (was over javac's 100 cap). **Open:** (1) saved data moves from `data/mighty_mail_delivery_service.dat` to `data/mighty_mail/delivery_service.dat` and vanilla has no fallback, so 1.21.10 worlds would come up with unowned, unnamed mailboxes and lose queued mail unless we add a one-time import of the old file (Refurbished did not). (2) `PostBoxBlock` must send `new ClientMailbox(id, mailbox.getOwner(server), name)` (see `PORT-26.3` note in `ClientMailbox`). |
