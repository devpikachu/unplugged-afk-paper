# Unplugged AFK Paper

A **feature-incomplete** Paper port of [Unplugged-AFK](https://github.com/sakura-ryoko/unplugged-afk), the Fabric mod by
Sakura Ryoko, published under the LGPL3-or-later license.

One command disconnects your client and leaves an unplugged player standing where you were, holding your spot at a farm
until the time you declared runs out.

> [!IMPORTANT]
> The unplugged player is an interactable, killable version of you. If it dies, its inventory drops as normal and you
> get the death screen on rejoining.

> [!WARNING]
> The unplugged player joins without the login step permission plugins hook into. LuckPerms is supported directly, so
> with it installed the unplugged player resolves permissions exactly as you do. Every other permission plugin needs
> support added for it specifically, and until then the unplugged player falls back to operator status and plugin
> defaults alone, so an AFK-kick bypass granted through a group will not apply to it.

## Features

- **Go green:** turn off your computer and leave your unplugged player to AFK for you
- **Inventory carries over:** the unplugged player starts with your inventory, and hands back whatever it collected
- **Configurable limits:** operators set the maximum duration and a global cap on how many unplugged players can exist
- **Admin control:** commands to inspect who is unplugged, and to debug the plugin's behaviour
- **Placeholder aware:** with PlaceholderAPI or MiniPlaceholders installed, scoreboards, tab lists and chat plugins can
  show who is unplugged and for how long
- **Proxy aware:** behind Velocity, unplugging disconnects you from the whole network, returning puts you back on the
  server your unplugged player is on, and the server list still counts it

### Planned

- **Bypass max duration:** a permission letting staff or VIPs unplug for longer than the configured maximum
- **Bypass cap:** a permission letting staff or VIPs unplug even once the cap is reached
- **Configurable messages:** move the plugin's chat messages into the config
- **Historical data:** persist records to disk so staff can review how players have used the plugin
- **Re-spawn unplugged players on server reboot:** bring them back automatically when a backend restarts
- **More admin commands:** unplug an online player on their behalf, unplug an offline player, and similar

## Requirements

Both JARs reach into their host's own internals, Minecraft's on the backend and Velocity's on the proxy, so neither is
guaranteed to work on other releases. Anything outside this table is best-effort and logged on startup.

| Unplugged AFK | Minecraft | Velocity |
|---------------|-----------|----------|
| 0.2.0+        | 1.21.11   | 3.5.0    |

## Installation

Each release ships two JARs. Install both from the same release. Mixed versions are not supported.

| JAR                                    | Goes in                         | Required            |
|----------------------------------------|---------------------------------|---------------------|
| `unplugged-afk-<version>.jar`          | every backend's `plugins/`      | Yes                 |
| `unplugged-afk-velocity-<version>.jar` | the Velocity proxy's `plugins/` | Yes, behind a proxy |

## Commands

| Command                                           | Description                                                                             | Permission                  |
|---------------------------------------------------|-----------------------------------------------------------------------------------------|-----------------------------|
| `/unplug <duration> <reason>`                     | Disconnects you and leaves an unplugged player in your place for `duration` minutes     | `unplugged-afk.unplug`      |
| `/unplugged info <player>`                        | Shows the unplugged details for `player`                                                | `unplugged-afk.admin.info`  |
| `/unplugged list`                                 | Lists every unplugged player, and how many slots of the cap are in use                  | `unplugged-afk.admin.list`  |
| `/unplugged debug spawn-fake <duration> [reason]` | Spawns a throwaway unplugged player with a random UUID and name. Requires `debug: true` | `unplugged-afk.admin.debug` |

## Configuration

### Backend

`plugins/unplugged-afk/config.yml`, on every backend.

| Key                   | Description                                                | Default     | Minimum | Maximum |
|-----------------------|------------------------------------------------------------|-------------|---------|---------|
| `debug`               | Enables debug functionality. See [Debug Mode](#debug-mode) | `false`     |         |         |
| `maxUnpluggedPlayers` | How many unplugged players may exist at the same time      | `16`        | `1`     |         |
| `maxDurationMins`     | The longest a player may unplug for, in minutes            | `480`       | `1`     |         |
| `link.host`           | The proxy's address                                        | `127.0.0.1` |         |         |
| `link.port`           | The port the proxy listens on                              | `25580`     | `1`     | `65535` |
| `link.secret`         | Copied from the proxy's config                             |             |         |         |
| `link.serverName`     | This backend's name, exactly as `velocity.toml` spells it  |             |         |         |

Out-of-range values are clamped back to their default, with a warning in the console.

Behind a proxy, `link.secret` and `link.serverName` have no usable default, and the plugin refuses to enable until both
are set.

### Proxy

`plugins/unplugged-afk/config.yml`, on the proxy.

| Key           | Description                                                       | Default     | Minimum | Maximum |
|---------------|-------------------------------------------------------------------|-------------|---------|---------|
| `debug`       | Logs link, presence and relay activity to the console             | `false`     |         |         |
| `link.host`   | The address to listen on. Widen it for backends on other machines | `127.0.0.1` |         |         |
| `link.port`   | The port to listen on                                             | `25580`     | `1`     | `65535` |
| `link.secret` | Generated on first start. Copy it into every backend              |             |         |         |

Out-of-range values are clamped back to their default, with a warning in the console.

> [!WARNING]
> The link port is an internal channel between the proxy and its backends, never a player-facing one. If you widen
> `link.host` past `127.0.0.1`, restrict the port to your backends at the firewall. An open one gets found by server
> scanners.

## Permissions

| Node                        | Grants                                 | Default   |
|-----------------------------|----------------------------------------|-----------|
| `unplugged-afk.unplug`      | `/unplug`                              | Everyone  |
| `unplugged-afk.admin`       | Every admin node below                 | Operators |
| `unplugged-afk.admin.info`  | `/unplugged info`                      | Operators |
| `unplugged-afk.admin.list`  | `/unplugged list`                      | Operators |
| `unplugged-afk.admin.debug` | `/unplugged debug` and its subcommands | Operators |

## Placeholders

### PlaceholderAPI

With [PlaceholderAPI](https://github.com/PlaceholderAPI/PlaceholderAPI) installed, the backend plugin registers an
expansion under the identifier `unplugged-afk`.

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

Every placeholder except `%unplugged-afk_count%` describes one player and renders empty when that player is not
unplugged. `%unplugged-afk_is_unplugged%` renders `false` instead.

### MiniPlaceholders

With [MiniPlaceholders](https://modrinth.com/plugin/miniplaceholders) installed, both JARs register an expansion under
the same name. MiniMessage spells the tags `<unplugged-afk_is_unplugged>` rather than `%unplugged-afk_is_unplugged%`.

Installed on a backend, the tags answer for that server. Installed on the proxy, they answer for the whole network.
Three of them differ between the two sides:

| Tag                       | On a backend                             | On the proxy                          |
|---------------------------|------------------------------------------|---------------------------------------|
| `<unplugged-afk_count>`   | How many this server holds               | How many the network holds            |
| `<unplugged-afk_server>`  | Not registered                           | The server the unplugged player is on |
| `<unplugged-afk_is_fake>` | `true` for `/unplugged debug spawn-fake` | Not registered                        |

The rest carry the meanings listed above and render identically on either side.

## Proxy Support

### Velocity

Behind Velocity, `/unplug` needs to disconnect you from the whole network rather than from the single backend you are
on, and reconnecting needs to send you back to the backend holding your unplugged player. For this to work properly, the
companion JAR is required on the proxy.

Supported Velocity versions are listed under [Requirements](#requirements). Other versions are best-effort, and the
companion logs an error on startup when it cannot resolve the internals it needs.

## Debug Mode

Everything here is gated behind the `debug` configuration flag, which is off by default.

- `/unplugged debug spawn-fake` becomes available. It spawns a throwaway unplugged player with a random UUID and name.
- Every `/unplug` writes a text file to `plugins/unplugged-afk/dumps/` holding the session details and a full snapshot
  of the player, inventory and ender chest included.

The proxy has its own `debug` flag, set independently of the backends'. It gates noisy console logging useful for
debugging.

## Contributing

Contributions are welcome, whether that is a bug report, a compatibility finding from your own server, a feature
request, or a pull request.

Everyone taking part is expected to follow the [Code of Conduct](CODE_OF_CONDUCT.md).

## Acknowledgements

Huge thanks to Sakura Ryoko for [Unplugged-AFK](https://github.com/sakura-ryoko/unplugged-afk), the mod this ports. This
plugin would not exist without their work.

## License

This project is licensed under [LGPL3-or-later](LICENSE).
