# CPECraft

Fabric server mod for the CPE Minecraft server: LuckPerms-gated admin
commands, student-ID whitelist verification with a freeze-until-verified
flow, and CPE batch → LuckPerms group binding.

## Requirements

- Minecraft 26.2, Fabric Loader >=0.19.3, Fabric API, Java 25
- [LuckPerms for Fabric](https://luckperms.net/download) installed on the
  server (recommended, not required — without it, commands fall back to
  vanilla op-level 2 (GAMEMASTERS) and batch group auto-assignment is
  skipped with a logged warning)

## Build & run

```
./gradlew build
./gradlew runServer   # boots a dev server in run/
```

## Config

`config/cpecraft/config.json` 

```json
{
  "ybApiBaseUrl" : "https://example.com",
  "ybApiKey" : "changeme"
}
```

## Data

SQLite database at `config/cpecraft/data.db`, holding:
- `students` — verified players (UUID, claimed student ID, name/batch from
  the verification API, verified-at timestamp)
- `batches` — batch number → LuckPerms group bindings, created via
  `/createbatch`
- `home` — per-player named homes (position, rotation, dimension, default flag)
- `config` — misc key/value settings (e.g. `max_home_quota`, `spawn_x/y/z`)

## Commands

| Command | Permission node | Notes |
|---|---|---|
| `/gm <mode> [player]` | `cpecraft.command.gm` | Gamemode shortcut |
| `/invsee <player>` | `cpecraft.command.invsee` | Live view/edit of the target's main 36 inventory slots (armor/offhand not included) |
| `/userstats <player>` | `cpecraft.command.userstats` | Play time, deaths, mob/player kills |
| `/userinfo <player>` | `cpecraft.command.userinfo` | UUID + this mod's verification record |
| `/createbatch <batchNumber> <luckpermsGroup> <displayName>` | `cpecraft.command.createbatch` | |
| `/listbatches` | `cpecraft.command.listbatches` | |
| `/deletebatch <batchNumber>` | `cpecraft.command.deletebatch` | |
| `/verify <studentId>` | none — always usable, including while frozen | |
| `/motd` | none — viewable by anyone | Shows the current MOTD |
| `/motd set <message>` | `cpecraft.command.motd` | Updates the MOTD, persisted to `config/cpecraft/motd.txt` |
| `/unlink <player>` | `cpecraft.command.unlink` | Unverifies a player (works offline too), freeing their student ID for reuse; re-freezes them and revokes their batch's LuckPerms group if online |
| `/overrideverification <player> <studentId> [batch]` | `cpecraft.command.overrideverification` | Manually marks a player verified without calling the YB API (for students not in the YB database yet); works offline too. `batch` is optional - omit to skip LuckPerms group assignment |
| `/sethome [name]` | none | Saves your current position as a home (default name `home`); capped by the admin-set `max_home_quota` config value unless you have `cpecraft.command.home.quotabypass` |
| `/home [name]` (alias `/h`) | none | Teleports to a named home, or your default home, or your oldest home if no default is set; requires standing still for 3 seconds first, to stop it being used to escape combat |
| `/listhome` (alias `/lh`) | none | Lists your homes, tagging the default one |
| `/defaulthome <name>` | none | Marks a home as your default; only one default at a time, setting a new one moves the flag |
| `/delhome <name>` | none | Deletes a home |

