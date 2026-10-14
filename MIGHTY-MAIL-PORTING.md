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
Run each on Fabric and on NeoForge, on a dedicated server with two players (done 2026-10-14 on both; see the log):
- [x] Place a post box and mailboxes in several wood types.
- [x] Name a mailbox; it appears in the post box list.
- [x] Send items with a message; the red flag appears; items and message arrive intact.
- [x] Send to a mailbox in another dimension.
- [x] Fill a mailbox; check the queue limit and the delivery result message.
- [x] Restart the server with mail queued; nothing is lost.
- [x] Upgrade path: stop the server, move `world/dimensions/minecraft/overworld/data/mighty_mail/delivery_service.dat` to `world/data/mighty_mail_delivery_service.dat` (where 1.21.10 kept it), start again; the log says it imported and owners and names are intact.
- [x] Break a mailbox with mail inside; items drop and nothing duplicates.
- [x] Config options (mailbox size, queue limit) still work.
- [x] Logs are free of repeated errors and missing textures.

### Phase 4: Publishing
- [x] Choose a public name that doesn't suggest it's official: **Mighty Mail: Unofficial Port** (Gabby, 2026-10-14).
- [x] README: unofficial notice, credit to MrCrayfish, links to the original mod and Framework.
- [ ] Modrinth: tick the AI-generated content disclosure; write the description yourself; use real screenshots; list Framework and Fabric API as dependencies; upload Fabric and NeoForge jars as separate versions. (Checklist in `docs/MODRINTH.md`.)
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
| 2026-10-14 | Phase 2: `mail` | Reference clone of Refurbished Furniture at `mods-dev/refurbished-furniture-ref` (blobless; `git diff origin/1.21.10 origin/26.2 -- <path>`; no 26.3 branch exists yet). Applied its `mail` diff: `DeliveryService` no longer holds the server (`serverTick(server)`, `Mailbox.tick(service, server)`, `spawnQueueIntoLevel(server)`), codec is a plain constant, `SavedDataType` takes an `Identifier` (`mighty_mail:delivery_service`), `ResourceLocation` → `Identifier` and `.location()` → `.identifier()`; `IMailbox.getOwner(@Nullable server)` with a no-arg default; `Utils.resource` returns `Identifier` (name kept). Touched outside the package only where the interface forced it: `Bootstrap` tick hook, `ClientMailbox.getOwner` signature. `mail` + `util` compile clean; common is down to 88 errors (was over javac's 100 cap). Saved data moves from `data/mighty_mail_delivery_service.dat` to `data/mighty_mail/delivery_service.dat` and vanilla has no fallback; Gabby chose a one-time import over Refurbished's accept-the-loss approach, so `DeliveryService.get` now reads the old file once when the new one is missing (`importLegacyData`, PORT-26.3, old file left as a backup). |
| 2026-10-14 | Phase 2: `blockentity`, `block` | Refurbished's diff for these four files is tiny (import cleanups plus the `PostBoxBlock` owner mapping), and its blocks inherit the rest from its own base classes, so the real 26.3 change came from the game: block codecs are gone (`BlockBehaviour.codec()` / `Block.simpleCodec` no longer exist), so `MailboxBlock` and `PostBoxBlock` drop their `CODEC` and `codec()`. `PostBoxBlock.useWithoutItem` now maps each `Mailbox` to a `ClientMailbox` with `getOwner(server)` before opening the menu (kept typed as `IMailbox` so `PostBoxMenu` is untouched until the `inventory` session; Refurbished retyped `CustomData` to `ClientMailbox`). Block entities needed nothing. Both packages compile; common is at 84 errors. |
| 2026-10-14 | Phase 2: `item` | As in Refurbished: `level.random` is protected now (`level.getRandom()`), and `ItemContainerContents.stream()` became `nonEmptyItemCopyStream()`. The same two renames plus `Vec3.atCenterOf(pos)` (was `pos.getCenter()`) fixed the package dispenser behaviour in `Bootstrap`. `MailboxItem` needed nothing. `inventory` (`PostBoxMenu`, `PostBoxSlot`) already compiles, so it is skipped unless a later package needs Refurbished's `ClientMailbox` retyping. Common is at 80 errors. |
| 2026-10-14 | Phase 2: `network` | Only `ClientPlayHandler` needed a change: the current screen is `mc.gui.screen()` now (the `Minecraft.screen` field is gone), as in Refurbished. Messages and `ServerPlayHandler` compiled as they were. Common is at 78 errors. |
| 2026-10-14 | Phase 2: `client` | The big GUI change. Screens no longer draw with `GuiGraphics`; they fill a `GuiGraphicsExtractor` in `extractRenderState` / `extractLabels` / `extractBackground` (was `render` / `renderLabels` / `renderBg`), text goes through `text()` instead of `drawString()`, and player faces through `PlayerFaceExtractor`. `AbstractContainerScreen` takes the image size in its constructor (the fields are final now). Buttons implement `extractContents` and call `extractDefaultSprite`. Screens are opened with `mc.gui.setScreen`. `GLFW` is no longer on the classpath, so mouse buttons compare against `InputConstants.MOUSE_BUTTON_LEFT`. All of this matches Refurbished 26.2 except that 26.3's container screen already runs the tooltip pass inside `extractRenderState`, so the old extra `renderTooltip` call was dropped. `ScreenHelper` and `ClientMailbox` were fine. Common is at 38 errors: datagen, `MigrateCommand`, `ModCreativeTabs`, `Config`. |
| 2026-10-14 | Phase 2: `datagen`, `core`, `Config`, `command`, loaders | Datagen moved the most since 26.2, so vanilla's own 26.3 sources were the guide rather than Refurbished: recipes and loot tables are registry entries now (`RecipeProvider` takes two `BootstrapContext`s and has no `Runner`; `LootTableProvider` is a `SingleRegistryBootstrap`), collected in a new `RegistriesGen` registry set; `IntrinsicHolderTagsProvider` is gone, so `BlockTagGen` extends `TagsProvider<Block>` and appends registry keys; block state generation takes `Material`s for textures and `Variant` moved to `client.renderer.block.dispatch`. Loader wiring: NeoForge emits the registry set with `DatapackBuiltinEntriesProvider.forReloadableLayer`; Fabric uses `FabricRecipeProvider` and `FabricBlockLootSubProvider` (which reuse `RecipeGen` and the block list from `LootTableGen`) because they filter output to the mod's namespace. `generate` and `buildRecipes` overrides must be public since both loaders widen the vanilla methods. `CreativeModeTab.Output` is protected in vanilla and widened by each loader, so the tab's item lambda moved into a new `IPlatformHelper` service (`Services.PLATFORM.displayItemsAcceptor`, as Refurbished did). `Config` uses `Identifier.tryParse`; `MigrateCommand` uses `net.minecraft.util.Util` and `Commands.LEVEL_GAMEMASTERS.check(source.permissions())` in place of `hasPermission(2)`. Both loader `ClientHandler`s drop their cutout render-layer registration: since 26.x the chunk layer comes from the texture's transparency (Refurbished registers none either). **All three projects compile.** |
| 2026-10-14 | Phase 2 done | Datagen rerun on both loaders (`:fabric:runDatagen`, `:neoforge:runData`): 12 block states, 23 block models, 13 item definitions, 12 loot tables, 12 recipes with advancements and 3 block tags each. Fabric's datagen output folder was `src/generated`, one level above the `src/generated/resources` the build packs, so its jar would have shipped without models; now `src/generated/resources`, as in Refurbished 26.2. `:fabric:build` and `:neoforge:build` produce `mighty_mail-fabric-26.3-1.2.1.jar` and `mighty_mail-neoforge-26.3-1.2.1.jar`. Each jar was run with Framework 0.13.27 in an isolated test folder (Fabric 0.19.5 + Fabric API 0.163.0, NeoForge 26.3.0.77), joined a fresh creative world, and the Mighty Mail creative tab shows all eleven mailboxes and the post box on both loaders; no mod errors or missing models in either log. Phase 3 (dedicated-server tests) is next. |
| 2026-10-14 | Phase 3 done | Each loader: its own dedicated server (Fabric 0.19.5 + Fabric API 0.163.0 / NeoForge 26.3.0.77, Framework 0.13.27) in a scratch folder with two offline clients (one per player), all driven through the local MCPFabric bridge; scripts kept in `mods-dev/tools/mighty-mail-server-test/`. Every checklist item passed on both: post box plus oak, cherry, spruce, crimson and warped mailboxes placed and named through the naming screen; the post box lists all of them with owner faces and names; three diamonds with a message arrive as a package (sender and message verified with `data get`), the red flag shows until the owner opens the box, and opening the package drops the items; delivery into the Nether works; with `mailQueueSize = 2` the third package to an unloaded mailbox is refused with the red "max mail queue" toast and stays in the post box; packages queued before a server stop are delivered after the restart; the pre-26.3 import logs "Imported pre-26.3 delivery service data (N mailboxes)" and the names survive; `mailboxInventoryRows = 2` gives an 18-slot mailbox; breaking a mailbox with packages inside drops exactly that many package items; no mod errors, missing models or textures in any server or client log. Notes for next time: 26.x servers start with the whitelist on; NeoForge keeps a chunk loaded a little longer after `forceload remove`, so wait for `execute in <dim> if loaded x y z` to fail before testing the queue limit. |
| 2026-10-14 | Phase 4 | Gabby's picks: public name **Mighty Mail: Unofficial Port**, version `1.2.1+port.1` (upstream number plus a port counter as semver build metadata, so it still equals 1.2.1 for any dependency check; her pick was `-port.1`, changed to `+` because a pre-release would sort below 1.2.1), keep the icon and the banner (the banner is a package picture, not a logo), GitHub default branch set to `26.3` and description set, repo name kept. Mod id stays `mighty_mail` on purpose. Metadata: display name, description with the unofficial notice, homepage/source/issues point at the fork, MrCrayfish stays the author with the port in contributors/credits. New README, CHANGELOG, CLAUDE.md (shared rules), `docs/MODRINTH.md` checklist, and a plain build-both-loaders CI workflow in place of upstream's signing/publishing one (needs the `GPR_TOKEN` repository secret). `GPR_TOKEN` secret set from the GitHub CLI login; the first CI run (started by hand) built both loaders green; pushes didn't start runs until Actions was switched on for the fork through the API (`gh api -X PUT repos/gabmaybelle/mighty-mail/actions/permissions -F enabled=true -f allowed_actions=all`), after which the next push ran on its own. Screenshots taken 2026-10-14 in the singleplayer test world at 1600x900 (five in `docs/screenshots/`, two embedded in the README). Still Gabby's: the Modrinth description and whether to message MrCrayfish. |
