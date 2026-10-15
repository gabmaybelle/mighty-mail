# Mighty Mail: Unofficial Port

An unofficial port of MrCrayfish's Mighty Mail (MIT) from Minecraft 1.21.10 to 26.3, for Fabric and
NeoForge. The code is his; the port only changes what the newer game needs. README.md says what
players see. **MIGHTY-MAIL-PORTING.md is the plan and the log: read it at the start of a session and
add a log row at the end.**

## Layout

Multiloader: `common` (all the mod code, compiled against vanilla through ModDevGradle), `fabric`
and `neoforge` (entry points, platform helpers, data generation wiring), `buildSrc` (the shared
Gradle scripts). Branch `26.3` is the port; the `multiloader/*` branches are upstream's.

- `common/src/main/java/com/mrcrayfish/mightymail/`
  - `mail/`: `DeliveryService` (the mailbox registry, a `SavedData` keyed `mighty_mail:delivery_service`,
    with the one-time import of the pre-26.3 file), `Mailbox`, `IMailbox`, `PackageInfo`, `DeliveryResult`.
  - `block/`, `blockentity/`, `item/`, `inventory/`, `network/`: the post box, mailboxes and packages.
  - `client/`: `PostBoxScreen`, `TextInputScreen` (naming a mailbox), `IconButton`, `ClientMailbox`.
  - `datagen/`: block states and models (`FrameworkGenerator`), tags, and `RegistriesGen`, the registry
    set that holds recipes and loot tables (registry entries since 26.x). All of it loops over `MailboxWoods`.
    `CompatPackGen` writes one built-in pack per other wood mod to `resourcepacks/compat_<mod>` (all of
    that mod's mailbox JSON, as plain JSON, because its blocks don't exist while datagen runs).
  - `wood/`: `MailboxWood` (one row: id, name, planks, fence, painted texture, required mod, map colour) and `MailboxWoods`
    (the table, registration, lookups).
  - `platform/`: `IClientHelper` and `IPlatformHelper`, implemented per loader with `ServiceLoader`.
- Textures, the three base block models and the two planks-mapped models (`mail_box_planks*`, used
  with `mail_box_overlay.png` for woods without a painted texture) live in `common/src/main/resources`; everything else
  (block states, generated models, loot tables, recipes, tags) is produced by data generation into
  `fabric/src/generated/resources` and `neoforge/src/generated/resources` (not in git).

## Build and test

Needs JDK 25 and a GitHub token with `read:packages` for MrCrayfish's Maven (Framework and the
optional Refurbished Furniture jar): `GPR_USER` and `GPR_KEY` in the environment, never in the repo.

- `./gradlew :fabric:runDatagen :neoforge:runData`: regenerate the data files (do this after
  touching anything under `datagen/` or adding blocks).
- `./gradlew :fabric:build :neoforge:build`: jars in `fabric/build/libs/` and `neoforge/build/libs/`.
- `./gradlew :fabric:runClient` or `:neoforge:runClient`: a dev game with the mod.
- `./gradlew :fabric:runClientGameTest`: the in-game test (`fabric/src/gametest`, about two minutes,
  opens a real game window). One flat world: place and name a mailbox through the real server
  handlers, open the post box screen on the client, send through the send handler, delivery and
  flag, open the mailbox and the package, wrong inputs, break a mailbox with mail inside, the
  queue limit, then leave and reopen the world twice (a restart with mail waiting, and once more
  with the delivery file moved to its pre-26.3 location, which proves the import). Every
  check logs `[test] ok:` or `[test] FAILED:`; screenshots land in
  `fabric/build/run/clientGameTest/screenshots`. It must pass before a commit, and a new check must
  be shown to fail once with the thing it checks broken. `-PtestBop` runs it with Biomes O' Plenty
  (from Modrinth, version ids in `gradle.properties`), which switches on the wood-mod section: the
  compat mailboxes exist, have a recipe and a loot table, render with a real texture, and take mail.
  CI runs both variants.
- Not covered by the game test (run by hand with Gabby's local tools when `mail/`, `network/` or
  the screens change): two players on a dedicated server, anything on NeoForge, and the wood-mod
  packs on a server. The checklist is in MIGHTY-MAIL-PORTING.md (Phase 3 and Phase 5).
- The game test's save checks can't be "proven" by dropping a `setDirty()` call: the delivery
  service is one saved-data object, and placing a mailbox already marks it dirty. Break the
  codec (or the import) instead.

CI (`.github/workflows/build.yml`) builds both loaders and runs the game test on every push; it
needs the `GPR_TOKEN` repository secret.

## Gotchas

- **Keep the mod id `mighty_mail`.** Worlds, the saved-data import and the migrate command depend on it.
- **Mark port-specific workarounds** with `// PORT-26.3:` and a reason. The current ones: Refurbished
  Furniture has no 26.3 build, so `MigrateCommand` compiles against its 26.2 jar (compile-only, with a
  `requireCapability` hint); the saved-data import in `DeliveryService`.
- **Datagen output on Fabric must go to `src/generated/resources`**, or the Fabric jar ships without
  models (the build packs that folder).
- **Run datagen before building, everywhere.** The generated folders are not in git, so a fresh
  checkout (CI included) builds hollow jars unless `:fabric:runDatagen :neoforge:runData` ran first.
  Found 2026-10-14: every CI artifact until then had no block states or recipes. Now the `jar`
  tasks refuse without the generated folders, CI has a datagen step, and the game test checks
  oak's recipe, loot table and model. Release jars are built locally after a fresh datagen.
- **`generate()` and `buildRecipes()` overrides in `datagen/` must be `public`**: both loaders widen
  the vanilla methods, and the common sources are compiled again inside each loader project.
- **`CreativeModeTab.Output` is protected in vanilla.** The creative tab fills its items through
  `IPlatformHelper.displayItemsAcceptor`, implemented in each loader where the type is widened.
- **No render-layer registration.** Since 26.x the chunk layer comes from the texture's transparency.
- **Mailboxes of other wood mods** are rows in `MailboxWoods.COMPAT` with a `requiredMod`. They register
  only when that mod is loaded, and their JSON lives in a built-in pack the loaders enable only then
  (Fabric `ResourceLoader.registerBuiltinPack`, NeoForge `AddPackFindersEvent`), so a missing mod never
  produces errors. Adding a mod: add rows, run datagen, test with the mod's jar in a game and once
  without it. The compat JSON mirrors what the vanilla providers write for oak; if a vanilla format
  changes, compare with the generated oak files.
- **Framework scans our registry classes during its own startup**, before our entry point, so
  `MailboxWoods.ensureRegistered()` runs from `ModBlockEntities`' static initialiser. Registering
  later crashes every placed mailbox with "Invalid block entity".
- **In the game test**, the current screen is `mc.gui.screen()` (26.x), block use goes through
  `BlockState.useWithoutItem(level, player, hit)` (the block's own method is protected), and a
  block is placed "as the player" with `setBlock` plus `Block.setPlacedBy`, which is what sets the
  owner and the pending name.
- **Owner names in the post box list** are resolved on the server in `PostBoxBlock` with
  `getOwner(server)`; `IMailbox.getOwner()` without a server is empty server-side.
- **Reference for future updates:** MrCrayfish's Furniture Mod: Refurbished has the same mail code.
  Diff its branches for the next Minecraft version before porting by hand.

## Conventions

- Upstream's style: four spaces, braces on their own line, `this.` on fields. Keep diffs small and
  don't refactor what porting doesn't need. Never remove the licence or MrCrayfish's credits.
- Every string players see goes in `assets/mighty_mail/lang/en_us.json`.
- The README and CHANGELOG are written for players: plain and friendly, no jargon. Add player-facing
  changes under `## Unreleased` at the top of `CHANGELOG.md` (start that heading again after a
  release is numbered).
- The mod is credited to MrCrayfish; the port "was made by Gabby with help from Claude, an AI
  assistant" (README). Keep both.
- Versions are upstream's number plus a port counter as build metadata (`1.2.1+port.1`), so the
  jar still counts as 1.2.1 to anything that checks.

## Working on Gabby's mods (the same in every repo)

Gabby makes these mods with help from Claude. Several Claude sessions work on them: one on her
computer and others in the cloud. These rules keep their work fitting together.

**One session per repo at a time.**
- A cloud session works on a `claude/...` branch and never pushes to `main`. Gabby's local
  session reviews the branch, runs its tests and merges it as one commit.
- Start from the latest `main` (`git pull`), and say in the branch's last commit or a short note
  what is finished and what isn't.

**Commits.**
- Author and committer are always `gabmaybelle <211247938+gabmaybelle@users.noreply.github.com>`:
  `git -c user.name=gabmaybelle -c user.email=211247938+gabmaybelle@users.noreply.github.com commit ...`.
  Never a personal email, and never `Claude <noreply@anthropic.com>` as the author. Claude goes in
  a `Co-Authored-By:` line at the end of the message.
- Ask Gabby before committing or pushing when she's around. Finish the build and the tests first.
- Commit messages say what changed for players, in plain sentences.

**Privacy.** These repos are public. Don't add personal email addresses, real names other than
"Gabby", or anything about her other private projects and modpacks: not in code, docs, commits
or test names. Check the diff for these before every push.

**Tests.**
- `./gradlew build` and `./gradlew runClientGameTest` (where the repo has one) must pass before
  a commit. The game test opens a real game window for a minute or two.
- A new or changed check should fail with the feature switched off: run it that way once, then
  put the code back. A check that compares against the setting it tests proves nothing.
- On Linux without a screen, run game tests under `xvfb-run -a`. 26.3 also needs
  `SDL_VIDEO_FORCE_EGL=1` with `libegl1 libegl-mesa0` installed, or the game hangs at startup.

**Writing.**
- README and CHANGELOG are for players: plain, friendly, no jargon. New player-facing changes
  go in the CHANGELOG's newest section.
- Keep the credit line: "made by Gabby with help from Claude, an AI assistant".
- Gabby publishes on Modrinth herself. Don't upload anything there.

**Minecraft.** Fabric on Minecraft 26.3 (Mojang names; Loom 1.18.4, Java 25). Vanilla names
changed a lot in 26.x: check the merged jar rather than older docs (for example `iron_chain`,
`pink_petals[flower_amount=N]`, timelines instead of `time query daytime`).
