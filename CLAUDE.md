# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

CPECraft is a Fabric server mod for a CPE (Computer Engineering department)
Minecraft server: LuckPerms-gated admin commands, a student-ID whitelist
verification flow that freezes unverified players, an admin-settable MOTD
with light markdown formatting, and CPE batch → LuckPerms group binding.

Target stack: Minecraft 26.2, Fabric Loader >=0.19.3, Fabric API, Java 25,
**Mojang's official mappings** (Fabric dropped Yarn as of MC 26.1 — code
uses Mojang-mapped names like `ServerPlayer`/`CommandSourceStack`, not Yarn
names like `ServerPlayerEntity`/`ServerCommandSource`). Minecraft itself
moved to CalVer (`26.1`, `26.2`, ...) instead of `1.x` versioning.

## Commands

```
./gradlew build        # compile + jar; output at build/libs/cpecraft-<version>.jar
./gradlew runServer     # boots a dedicated dev server in run/ with the mod loaded
./gradlew genSources    # decompile Minecraft sources into Loom's cache, for looking up
                         # exact Mojang-mapped class/method names not already used in this repo
```

First `runServer` run requires accepting Mojang's EULA in `run/eula.txt`. There
are no automated tests (`compileTestJava`/`test` are `NO-SOURCE`) — verification
is done by booting the dev server and driving commands from the console, or the
`main`-method self-check in `Markdown.java` (`java -cp ... com.cpesu.cpecraft.motd.Markdown`).

CI: `.github/workflows/build.yml` runs `./gradlew build` on push/PR to `main`
and uploads `build/libs/*.jar` as a workflow artifact.

### Looking up Mojang-mapped API names

This is the single most important workflow quirk in this repo: **do not
guess Minecraft/Fabric class or method names** — Mojang mappings changed a
lot of familiar Yarn-era names, and guesses compile-fail or silently target
the wrong overload. Verify against the actual jars before writing code that
calls a new API:

```
# Merged, deobfuscated Minecraft jar (Mojang mappings):
javap -classpath ~/.gradle/caches/fabric-loom/26.2/minecraft-merged.jar <fully.qualified.ClassName>

# Any Fabric API module jar (find the exact one first):
find ~/.gradle/caches -iname "fabric-<module>-*.jar" | grep -v sources
javap -classpath <jar> <fully.qualified.ClassName>
```

Every non-trivial class used in this codebase (event interfaces, argument
types, `ChestMenu`, `PermissionLevel`, LuckPerms API, etc.) was confirmed
this way rather than assumed — follow the same discipline when adding new
Minecraft/Fabric API calls.

## Architecture

Root package `com.cpesu.cpecraft`, mod ID `cpecraft`. Everything is wired
together from `Cpecraft.onInitialize()` (the `ModInitializer` entrypoint),
which constructs the singletons below and exposes them as static accessors
(`Cpecraft.studentRepository()`, `.batchRepository()`, `.verificationService()`,
`.motdService()`) — there's no DI framework; commands and listeners just
call these statics directly.

- **`command/`** — one class per Brigadier command, each with a static
  `register(CommandDispatcher<CommandSourceStack>)`. `CommandRegistrar`
  is the single `CommandRegistrationCallback.EVENT.register(...)` call
  that dispatches to all of them. Permission-gated commands use
  `CpecraftPermissions.requires(NODE)` in `.requires(...)`; `/verify` and
  bare `/motd` are intentionally registered with no gate.
- **`permission/CpecraftPermissions`** — permission node string constants
  + a helper wrapping `me.lucko`'s `fabric-permissions-api`
  (`Permissions.require(node, PermissionLevel)`), which bridges to
  LuckPerms when installed and falls back to vanilla op-level otherwise.
  Default fallback is `PermissionLevel.GAMEMASTERS` (op level 2).
- **`db/`** — `Database` owns one long-lived SQLite `Connection` (opened at
  `config/cpecraft/data.db`, schema ensured via `CREATE TABLE IF NOT EXISTS`
  on startup, closed on `ServerLifecycleEvents.SERVER_STOPPING`).
  `StudentRepository`/`BatchRepository` are plain blocking JDBC repositories
  over that one connection — callers must not call them from a context that
  can't tolerate a brief blocking JDBC call (see `VerificationService` for
  how that's handled off the network-callback thread).
- **`freeze/`** — `FreezeManager` is a static in-memory `Map<UUID, Vec3>` of
  currently-frozen players and their anchor position.
  `FreezeEventListeners` freezes a player on join if they have no
  `students` row, then enforces it purely via stable Fabric API events
  (tick-based teleport-back for movement, `PlayerBlockBreakEvents`/
  `AttackBlockCallback`/`UseBlockCallback`/`UseItemCallback`/
  `ServerMessageEvents.ALLOW_CHAT_MESSAGE` for everything else). **No
  mixins are used anywhere in this mod** — that was a deliberate choice
  (see freeze-mechanism reasoning in git history / README) to avoid
  depending on unstable, version-fragile injection targets; inventory-slot
  manipulation is left unblocked for the same reason (no Fabric API event
  exists for it without a mixin).
- **`verification/`** — `StudentApiClient` is the interface both
  implementations satisfy. `HttpStudentApiClient` (wired up by default in
  `Cpecraft.onInitialize()`) calls the external "YB" student API —
  `GET {ybApiBaseUrl}/api/yb/get-info?id={studentId}`, auth via a raw
  `Authorization: <key>` header (**no** `Bearer` prefix — that's this
  API's convention, not a bug), response parsed with Jackson as
  `{"data": {"id", "eng_name", "eng_nick", "gen"}}` mapping to
  `StudentInfo(studentId, name, nickName, batch)` (`gen` = the student's
  batch/cohort year). `MockStudentApiClient` (a small hardcoded
  ID→`StudentInfo` map) is kept around for local testing without hitting
  the real API or needing credentials — swap it back into
  `Cpecraft.onInitialize()` for that. `VerificationService` orchestrates
  the whole flow: call the API off the main thread → persist to
  `StudentRepository` → resolve batch→LuckPerms group via
  `BatchRepository`/`LuckPermsService` → hop back to the main thread via
  `server.execute(...)` only for the final player-facing step (unfreeze +
  chat message), since touching player/world state off-thread isn't safe.
  `VerificationService.unlink(...)` is the inverse: deletes the
  `students` row (freeing the student ID), revokes the batch's LuckPerms
  group via the same `resolveGroupForBatch` helper `assignBatchGroup`
  uses, and re-freezes + notifies the player if they're currently online.
  `UnlinkCommand` resolves its target via `GameProfileArgument` (not
  `EntityArgument.player()` like the other admin commands) specifically
  so it works on offline players too. `VerificationService.overrideVerify(...)`
  is the admin escape hatch (`/overrideverification`): saves a `students`
  row without calling the YB API at all, for students not yet in YB's
  database - `batch` is optional there (null/blank skips group assignment
  entirely, vs. the normal flow where a batch always comes from the API).
- **`config/CpecraftConfig`** — a record loaded once at startup from
  `config/cpecraft/config.json` (`ybApiBaseUrl`, `ybApiKey` for the YB
  API). If the file is missing, a placeholder default is written and a
  warning logged; same warning if the loaded file still equals the
  placeholder. No in-game command reads or writes this — it holds a real
  secret, so it's file-only by design.
- **`luckperms/LuckPermsService`** — thin wrapper around
  `LuckPermsProvider.get()` / `UserManager.modifyUser(...)`. `net.luckperms:api`
  is `compileOnly` (see below), so when LuckPerms Fabric isn't installed,
  its classes genuinely aren't on the runtime classpath - **this bit us
  in production-shaped testing**: `LuckPermsProvider.get()` throwing
  `IllegalStateException` only covers the "registered but no-op" case.
  Merely *resolving* `LuckPermsService`'s own class (triggered by the
  caller's `invokestatic` to any of its methods) throws
  `NoClassDefFoundError` when LuckPerms is absent, and that happens
  before `LuckPermsService`'s own try/catch ever runs - a try/catch
  *inside* `LuckPermsService` cannot catch it. The fix lives in the
  caller: `VerificationService.assignBatchGroup`/`revokeBatchGroup` wrap
  their calls into `LuckPermsService` in `try { ... } catch (LinkageError e)`.
  Any new call site into `LuckPermsService` needs the same guard.
- **`motd/`** — `Markdown` is a standalone, Minecraft-API-light
  tokenizer/renderer for a deliberately small format (`**bold**`,
  `*italic*`, `__underline__`, `~~strikethrough~~`; no nesting/escaping/
  links). `MotdService` persists the raw markdown string to
  `config/cpecraft/motd.txt` and caches it in memory.
- **`welcome/WelcomeEventListeners`** — separate `ServerPlayConnectionEvents.JOIN`
  registration from `FreezeEventListeners` (deliberately: this is
  presentation, not freeze logic). Sends a join title (verify-instructions
  vs. welcome-back, based on the same `StudentRepository` check freeze
  uses) and the parsed MOTD to chat. Title sending itself lives in
  `util/Titles` (shared, so `VerificationService` or other future
  call sites can send a title without duplicating the
  `ClientboundSetTitleTextPacket`/`ClientboundSetSubtitleTextPacket` pair).

### Dependency bundling model

- `me.lucko:fabric-permissions-api`, `org.xerial:sqlite-jdbc`, and the
  three Jackson artifacts (`jackson-databind`/`-core`/`-annotations`, used
  for `config.json` and YB API response parsing) are all `implementation`
  **and** `include`d — they're bundled into the mod jar via Fabric Loom's
  jar-in-jar (nested under `META-INF/jars/`, referenced from
  `fabric.mod.json`'s `jars` array — verify with `jar tf build/libs/*.jar`
  after adding a new runtime dependency) so server admins don't need to
  install anything beyond this mod + Fabric API. `implementation` alone is
  **not enough** — it compiles fine locally but throws
  `NoClassDefFoundError` at runtime on a real/dev server, since Fabric
  mods don't share a common classpath beyond what's bundled.
- `net.luckperms:api` is `compileOnly` only — never bundled. It must stay
  that way; the real implementation comes from the separately-installed
  LuckPerms Fabric plugin at runtime.
- No Yarn mappings block in `build.gradle` — Mojang mappings are the
  default/only option from MC 26.1 onward, so there's nothing to configure.

### Data model (SQLite, `config/cpecraft/data.db`)

- `students(uuid PK, username, student_id, name, batch, verified_at)`
- `batches(batch_number PK, luckperms_group, display_name)`

No foreign key between them — `VerificationService` matches
`students.batch` against `batches.batch_number` at verify-time by parsing
it as an int, and just logs + skips group assignment on any mismatch
(non-numeric batch, or no matching `/createbatch` entry) rather than
failing verification.

See `README.md` for the full command table and permission nodes.
