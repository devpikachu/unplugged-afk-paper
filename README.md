# Unplugged AFK Paper

**Unplugged AFK Paper** is a **feature-incomplete** port of
[Unplugged-AFK](https://github.com/sakura-ryoko/unplugged-afk), the Fabric mod by Sakura Ryoko, published under the
LGPL3-or-later license.

It lets players "go unplugged". One command disconnects your client and leaves an unplugged player standing exactly
where you were, holding your spot at a farm. Shut the computer down, save the electricity, keep the farm running. Once
the declared time is up, the unplugged player is kicked and its resources are freed.

> [!IMPORTANT]
> The unplugged player left behind is an interactable, killable version of you. Its inventory is the one you had at the
> moment you unplugged, and anything it picks up will be there when you come back. If it dies, you will be greeted with
> the death screen on rejoining.

> [!WARNING]
> Your unplugged player does not inherit permissions granted through a permission plugin such as LuckPerms. It
> resolves permissions from operator status and plugin defaults alone, because it joins without going through the
> login step those plugins hook into. So if a server relies on a permission to keep AFK players from being kicked,
> and that permission comes from a group rather than from operator status, the unplugged player is kicked as though
> it had no bypass at all. This may be addressed in a future release.

## Features

- **Go green:** turn off your computer and leave your unplugged player to AFK for you
- **Inventory carries over:** the unplugged player starts with your inventory, and hands back whatever it collected
- **Configurable limits:** operators set the maximum duration and a global cap on how many unplugged players can exist
  at once, so an AFK crowd cannot exhaust the server
- **Admin control:** commands to inspect who is unplugged, and to debug the plugin's behaviour
- **Placeholder aware:** with PlaceholderAPI installed, scoreboards, tab lists and chat plugins can show who is
  unplugged, for how long, and why
- **Proxy aware:** behind Velocity, unplugging disconnects you from the whole network, returning puts you back on the
  server your unplugged player is on, and the server list still counts the players you left behind

### Planned

- **Bypass max duration:** a permission letting staff or VIPs unplug for longer than the configured maximum
- **Bypass cap:** a permission letting staff or VIPs unplug even once the cap is reached
- **Configurable messages:** move the plugin's chat messages into the config, so operators can reword or translate them
- **Historical data:** persist records to disk so staff can review how players have used the plugin when handling
  support requests or reports
- **Re-spawn unplugged players on server reboot:** bring them back automatically when a backend restarts
- **More admin commands:** unplug an online player on their behalf, unplug an offline player, and similar
- **[MiniPlaceholders](https://modrinth.com/plugin/miniplaceholders) support:** support MiniPlaceholders like we do PAPI

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

## Placeholders

With [PlaceholderAPI](https://github.com/PlaceholderAPI/PlaceholderAPI) installed, the plugin registers an expansion
under the identifier `unplugged-afk`, so scoreboards, tab lists and chat plugins can show who is unplugged, for how
long, and why. There is nothing to configure, and nothing is registered at all when PlaceholderAPI is absent.

| Placeholder                      | Gives                                                             |
|----------------------------------|-------------------------------------------------------------------|
| `%unplugged-afk_is_unplugged%`   | `true` or `false`                                                 |
| `%unplugged-afk_duration_mins%`  | The window the player asked for, in minutes                       |
| `%unplugged-afk_reason%`         | The reason given to `/unplug`                                     |
| `%unplugged-afk_started%`        | How long ago they unplugged, such as `2 hour(s) 5 minute(s)`      |
| `%unplugged-afk_expires%`        | How long is left, in the same form                                |
| `%unplugged-afk_remaining_mins%` | How long is left, as a plain number of minutes                    |
| `%unplugged-afk_is_fake%`        | `true` for an unplugged player from `/unplugged debug spawn-fake` |
| `%unplugged-afk_count%`          | How many this server holds, matching the `/unplugged list` total  |

Every placeholder except `%unplugged-afk_count%` describes one player, and renders empty for a player who is not
unplugged. `%unplugged-afk_is_unplugged%` is the exception to that: it renders `false`.

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

The companion also corrects the **server list player count**. A single server counts unplugged players already, since
they are ordinary players as far as it is concerned, but a proxy answers the ping itself and can only see the players
connected to it, so unplugged players drop out of the number. With the companion installed the count goes back to what
a single server would have reported. There is nothing to configure.

A few things worth knowing about that number:

- Only the count is adjusted, never the hover list of names. Velocity's `sample-players-in-ping` is off by default and
  a plugin cannot read that setting, so there is no way to tell an operator who deliberately hides names from one who
  simply has nobody online.
- Unplugged players created by `/unplugged debug spawn-fake` are never counted, because they belong to no real account.
- The count can exceed `show-max-players`, in the same way a single server reports players who joined over its own
  limit.
- Keep `ping-passthrough = "DISABLED"` in `velocity.toml`, which is the default. On any other setting the backend
  supplies the count, it already includes that backend's unplugged players, and they are then counted twice.
- Other network-wide player views are not covered. A tab list plugin that collects players from every backend through
  the proxy, such as TAB, builds its list from proxy connections, so it cannot see unplugged players either.

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

## Acknowledgements

Huge thanks to Sakura Ryoko for [Unplugged-AFK](https://github.com/sakura-ryoko/unplugged-afk), the mod this ports. This
plugin would not exist without their work.

## License

This project is licensed under [LGPL3-or-later](LICENSE).
