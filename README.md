# Unplugged-AFK Paper

**Unplugged-AFK Paper** is a **feature-incomplete** port of
[Unplugged-AFK](https://github.com/sakura-ryoko/unplugged-afk), the Fabric mod by Sakura Ryoko, published under the
LGPL3-or-later license.

It lets players "go unplugged". One command disconnects your client and leaves an unplugged player standing exactly
where you were, holding your spot at a farm. Shut the computer down, save the electricity, keep the farm running. Once
the declared time is up, the unplugged player is kicked and its resources are freed.

> [!IMPORTANT]
> The unplugged player left behind is an interactable, killable version of you. Its inventory is the one you had at the
> moment you unplugged, and anything it picks up will be there when you come back. If it dies, you will be greeted with
> the death screen on rejoining.

## Features

- **Go green:** turn off your computer and leave your unplugged player to AFK for you
- **Inventory carries over:** the unplugged player starts with your inventory, and hands back whatever it collected
- **Configurable limits:** operators set the maximum duration and a global cap on how many unplugged players can exist
  at once, so an AFK crowd cannot exhaust the server
- **Admin control:** commands to inspect who is unplugged, and to debug the plugin's behaviour
- **Proxy aware:** behind Velocity, unplugging disconnects you from the whole network, and returning puts you back on
  the server your unplugged player is on

### Planned

- **Bypass max duration:** a permission letting staff or VIPs unplug for longer than the configured maximum
- **Bypass cap:** a permission letting staff or VIPs unplug even once the cap is reached
- **Configurable messages:** move the plugin's chat messages into the config, so operators can reword or translate them
- **Count unplugged players in the server list:** report them in the player count and hover list on a proxy, the way a
  single server already does, behind a config toggle
- **Historical data:** persist records to disk so staff can review how players have used the plugin when handling
  support requests or reports
- **Re-spawn unplugged players on server reboot:** bring them back automatically when a backend restarts
- **More admin commands:** unplug an online player on their behalf, unplug an offline player, and similar
- **PlaceholderAPI support:** register an expansion with
  [PlaceholderAPI](https://github.com/PlaceholderAPI/PlaceholderAPI), so scoreboards, tab lists and chat plugins can
  show who is unplugged, for how long, and why

## Installation

- **Server:** Paper, or any fork that supports Paper plugins
- **Minecraft version:** 1.21.11 exactly. The plugin refuses to enable on anything else.

Each release ships two JARs:

| JAR                                    | Goes in                         | Required               |
|----------------------------------------|---------------------------------|------------------------|
| `unplugged-afk-<version>.jar`          | every backend's `plugins/`      | Yes                    |
| `unplugged-afk-velocity-<version>.jar` | the Velocity proxy's `plugins/` | Recommended on a proxy |

Install both from the same release. The two sides share a message format that is kept in sync by hand, and is not
version-checked at runtime.

Running behind a proxy also needs two settings that live outside this plugin. See [Proxy Support](#proxy-support).

## Commands

| Command                                           | Description                                                                                         | Permission                  |
|---------------------------------------------------|-----------------------------------------------------------------------------------------------------|-----------------------------|
| `/unplug <duration> <reason>`                     | Disconnects you and leaves an unplugged player in your place for `duration` minutes, with `reason`. | `unplugged-afk.unplug`      |
| `/unplugged info <player>`                        | Shows the unplugged information for `player`.                                                       | `unplugged-afk.admin.info`  |
| `/unplugged list`                                 | Lists every unplugged player, and how many slots of the cap are in use.                             | `unplugged-afk.admin.list`  |
| `/unplugged debug spawn-fake <duration> [reason]` | Spawns a throwaway unplugged player with a random UUID and name. Requires `debug: true`.            | `unplugged-afk.admin.debug` |

## Configuration

| Key                 | Description                                                 | Default | Minimum |
|---------------------|-------------------------------------------------------------|---------|---------|
| debug               | Enables debug functionality. See [Debug Mode](#debug-mode). | `false` |         |
| maxUnpluggedPlayers | How many unplugged players may exist at the same time       | `16`    | `1`     |
| maxDurationMins     | The longest a player may unplug for, in minutes             | `480`   | `1`     |

An invalid value is clamped back to its default, with a warning in the console. The proxy companion has no configuration
of its own.

## Permissions

| Node                        | Grants                                 | Default                       |
|-----------------------------|----------------------------------------|-------------------------------|
| `unplugged-afk.unplug`      | `/unplug`                              | Everyone                      |
| `unplugged-afk.admin`       | Every admin node below                 |                               |
| `unplugged-afk.admin.info`  | `/unplugged info`                      | Held by `unplugged-afk.admin` |
| `unplugged-afk.admin.list`  | `/unplugged list`                      | Held by `unplugged-afk.admin` |
| `unplugged-afk.admin.debug` | `/unplugged debug` and its subcommands | Held by `unplugged-afk.admin` |

## Proxy Support

Requires **Velocity 3.5+**. Two settings outside this plugin have to be enabled, and nothing works without them:

| File                                     | Setting                                |
|------------------------------------------|----------------------------------------|
| `velocity.toml`, under `[advanced]`      | `bungee-plugin-message-channel = true` |
| each backend's `config/paper-global.yml` | `proxies.velocity.enabled: true`       |

Those two are what make `/unplug` disconnect you from the **network** rather than from the one backend you are on.
Without them the kick reaches Velocity as a backend failure, so it forwards you to the next entry in its `try` list
instead of disconnecting you. `/unplug` then only appears to work for players who happened to be on the last server in
that list, and silently relocates everyone else.

The companion JAR on the proxy is optional on top of that, and buys the return trip:

- Reconnecting puts you back on the backend holding your unplugged player, instead of the default lobby.
- That routing survives a proxy restart or crash.

Without the companion the network disconnect still works, you are just sent to the default lobby when you come back.

A routing record is used once, on the next login, and then discarded. It expires on the proxy's own clock, at the
requested duration plus a five minute grace period. Overshooting is deliberate: sending a returning player to the right
server slightly too long beats sending them to the wrong one slightly too early. Records live in
`plugins/unplugged-afk/sessions.json` on the proxy, rewritten on every change and pruned of expired entries at startup.

## Debug Mode

Everything here is gated behind the `debug` configuration flag, which is off by default.

- `/unplugged debug spawn-fake` becomes available. It spawns a throwaway unplugged player with a random UUID and name,
  for testing without tying up a real account.
- Every time a real player unplugs, a text file is written to `plugins/unplugged-afk/dumps/` holding the Unix timestamp,
  position, dimension, inventory contents and more.

## For Plugin Developers

Other plugins can ask who is unplugged. This matters because an unplugged player is a real `Player` on the server
carrying the real player's UUID, so anything that treats join, quit or death as bookkeeping for an account will see a
bot and take it for the account holder. The API is how you tell the two apart.

Add the plugin JAR to your compile classpath and list `unplugged-afk` under `softdepend` in your `plugin.yml`, which
guarantees it has enabled before you first look for it. Then take the service from Bukkit:

```java
var registration = Bukkit.getServicesManager().getRegistration(UnpluggedAfkApi.class);
if (registration != null) {
    UnpluggedAfkApi api = registration.getProvider();
    if (api.isUnplugged(player.getUniqueId())) {
        // A bot is holding this player's spot. Do not treat it as the player logging in.
    }
}
```

A null registration means the plugin is absent or disabled. Treat that as "nobody is unplugged" rather than as an error,
so your plugin still works on servers that do not run this one.

| Method               | Returns                                                           |
|----------------------|-------------------------------------------------------------------|
| `isUnplugged(UUID)`  | Whether a bot is holding that player's spot right now             |
| `isUnplugging(UUID)` | Whether an unplug is in flight, kicked but the bot not yet placed |
| `find(UUID)`         | An `Optional<UnpluggedPlayerInfo>` describing that bot            |
| `all()`              | An immutable snapshot of every bot currently standing in          |

`UnpluggedPlayerInfo` is an immutable record carrying the UUID, name, requested duration, reason, start and expiry
times, and whether it is a throwaway bot from `spawn-fake`. Its `remaining()` recomputes against the current clock on
every call, so it stays accurate for as long as you hold the record.

Four things worth knowing:

- `isUnplugged` and `isUnplugging` are disjoint, and there is a short gap between the kick and the bot appearing. If you
  mean "leave this account alone", check both.
- Every method is safe to call from any thread, and results are point-in-time snapshots.
- The package is [jspecify](https://jspecify.dev) `@NullMarked`, so nothing here accepts or returns `null`. Absence is
  expressed as an empty `Optional` or an empty `List`, never as a null.
- Nothing in the `dev.detpikachu.unpluggedafk.api` package references server internals, so compiling against it does not
  saddle your plugin with this one's exact-Minecraft-version requirement. It is also the only package that is fair game:
  everything else in the JAR is marked `@ApiStatus.Internal` and may be renamed or removed in any release. Do not
  implement `UnpluggedAfkApi` yourself either; it is marked non-extendable because methods will be added to it.

## Acknowledgements

Huge thanks to Sakura Ryoko for [Unplugged-AFK](https://github.com/sakura-ryoko/unplugged-afk), the mod this ports. This
plugin would not exist without their work.

## License

This project is licensed under [LGPL3-or-later](LICENSE).
