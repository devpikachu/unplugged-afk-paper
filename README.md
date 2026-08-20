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

## Installation

- **Server:** Paper, or any fork that supports Paper plugins
- **Minecraft version:** 1.21.11 exactly. The plugin refuses to enable on anything else.

Each release ships two JARs:

| JAR                                    | Goes in                         | Required                |
|----------------------------------------|---------------------------------|-------------------------|
| `unplugged-afk-<version>.jar`          | every backend's `plugins/`      | Yes                     |
| `unplugged-afk-velocity-<version>.jar` | the Velocity proxy's `plugins/` | Only on a proxy network |

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

Requires **Velocity 3.5+** and the companion JAR on the proxy. The companion is optional: without it `/unplug` still
disconnects you from the network correctly, you just are not routed back to the right backend when you return.

Two settings outside this plugin have to be enabled, and nothing works without them:

| File                                     | Setting                                |
|------------------------------------------|----------------------------------------|
| `velocity.toml`, under `[advanced]`      | `bungee-plugin-message-channel = true` |
| each backend's `config/paper-global.yml` | `proxies.velocity.enabled: true`       |

With those in place:

- `/unplug` disconnects you from the **network**, not just the backend you are on. Without the companion's forced
  disconnect, Velocity reads the kick as a failed backend and sends you to the next server in its `try` list.
- Reconnecting puts you back on the backend holding your unplugged player, instead of the default lobby.
- That routing survives a proxy restart or crash.

### Message format

The backend sends one message on the `unplugged-afk:sessions` channel, immediately before disconnecting the player:

```
writeUTF("SESSION_START")
writeInt(durationMins)
```

Worth knowing if you are reading the source, or writing your own companion:

- Traffic is backend to proxy only, and the proxy ignores anything that did not arrive over a server connection.
- Neither the UUID nor the server name is sent. The proxy takes both from the connection the message came in on, which
  is what stops a client forging a message to pin itself to an arbitrary backend.
- The proxy keeps `UUID -> (server, expiry)` in `plugins/unplugged-afk/sessions.json`, rewritten on every change and
  pruned of expired entries at startup. An entry routes exactly one login, and is then discarded.
- Expiry is the requested duration plus a five minute grace period. Overshooting is deliberate: sending a returning
  player to the right server slightly too long beats sending them to the wrong one slightly too early.
- There is no `SESSION_END`. The unplugged player sits on a connection that discards every outgoing packet, so it cannot
  report its own death. The proxy expires the entry on its own timer instead.

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
