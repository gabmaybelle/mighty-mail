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
    set that holds recipes and loot tables (registry entries since 26.x).
  - `platform/`: `IClientHelper` and `IPlatformHelper`, implemented per loader with `ServiceLoader`.
- Textures and the three base block models live in `common/src/main/resources`; everything else
  (block states, generated models, loot tables, recipes, tags) is produced by data generation into
  `fabric/src/generated/resources` and `neoforge/src/generated/resources` (not in git).

## Build and test

Needs JDK 25 and a GitHub token with `read:packages` for MrCrayfish's Maven (Framework and the
optional Refurbished Furniture jar): `GPR_USER` and `GPR_KEY` in the environment, never in the repo.

- `./gradlew :fabric:runDatagen :neoforge:runData`: regenerate the data files (do this after
  touching anything under `datagen/` or adding blocks).
- `./gradlew :fabric:build :neoforge:build`: jars in `fabric/build/libs/` and `neoforge/build/libs/`.
- `./gradlew :fabric:runClient` or `:neoforge:runClient`: a dev game with the mod.
- There is no automated game test. The Phase 3 checklist in MIGHTY-MAIL-PORTING.md (two players on
  a dedicated server, both loaders) was run by hand and must be repeated after any change to
  `mail/`, `network/` or the screens. Gabby's local tools drive it.

CI (`.github/workflows/build.yml`) builds both loaders on every push; it needs the `GPR_TOKEN`
repository secret.

## Gotchas

- **Keep the mod id `mighty_mail`.** Worlds, the saved-data import and the migrate command depend on it.
- **Mark port-specific workarounds** with `// PORT-26.3:` and a reason. The current ones: Refurbished
  Furniture has no 26.3 build, so `MigrateCommand` compiles against its 26.2 jar (compile-only, with a
  `requireCapability` hint); the saved-data import in `DeliveryService`.
- **Datagen output on Fabric must go to `src/generated/resources`**, or the Fabric jar ships without
  models (the build packs that folder).
- **`generate()` and `buildRecipes()` overrides in `datagen/` must be `public`**: both loaders widen
  the vanilla methods, and the common sources are compiled again inside each loader project.
- **`CreativeModeTab.Output` is protected in vanilla.** The creative tab fills its items through
  `IPlatformHelper.displayItemsAcceptor`, implemented in each loader where the type is widened.
- **No render-layer registration.** Since 26.x the chunk layer comes from the texture's transparency.
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
