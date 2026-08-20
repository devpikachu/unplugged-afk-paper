# Unplugged-AFK Paper

**Unplugged-AFK Paper** is a **feature-incomplete** port / rewrite of the
amazing [Unplugged-AFK](https://github.com/sakura-ryoko/unplugged-afk) Fabric mod by Sakura Ryoko, published under the
LGPL3-or-later license.

This is an eco-friendly Paper plugin that allows Players to "go unplugged" for a certain amount of time. By executing a
command, a bot (or unplugged player) of themselves is spawned to stand AFK at farms, at the same time disconnecting
their client. This allows players to safely shut down their computers, saving electricity and promoting a green approach
to server farming!

Once the time has elapsed, the unplugged player is automatically kicked from the server to free up resources!

> [!IMPORTANT]
> The unplugged player that is left on the server is an interactable, killable version of the player! Its inventory is
the same as the player at the time of unplugging and any items it picks up will be reflected when the player comes back.
If the unplugged player dies, the player will be greeted with the death screen on rejoining.

## Prerequisites & Installation

- **Server:** Paper or any fork that supports Paper plugins
- **Minecraft Version**: 1.21.11 exactly

Each release ships two jars:

| Jar | Goes in | Required |
|-----|---------|----------|
| `unplugged-afk-<version>.jar` | every backend's `plugins/` | Yes |
| `unplugged-afk-velocity-<version>.jar` | the Velocity proxy's `plugins/` | Only on a proxy network |

Install both from the same release. The two sides share a message format that is kept in sync by hand and is not
version-checked at runtime.

## Proxy Support

Requires **Velocity 3.5+**. The proxy companion is optional: without it `/unplug` still disconnects you from the network
correctly, you just are not routed back to the right backend when you return.

Two settings outside this plugin have to be enabled, and nothing works without them:

| File | Setting |
|------|---------|
| `velocity.toml`, under `[advanced]` | `bungee-plugin-message-channel = true` |
| each backend's `config/paper-global.yml` | `proxies.velocity.enabled: true` |

With those in place:

- `/unplug` disconnects you from the **network** rather than the current backend. A plain kick would make Velocity send
  you to the next server in its `try` list instead of disconnecting you.
- Reconnecting puts you back on the backend holding your unplugged player, instead of the default lobby.
- That routing survives a proxy restart or crash.

### Message format

The backend sends one message on the `unplugged-afk:sessions` channel, immediately before disconnecting the player:

```
writeUTF("SESSION_START")
writeInt(durationMins)
```

Notes for anyone reading the source or writing their own companion:

- Traffic is backend to proxy only, and the proxy ignores anything that does not arrive from a server connection.
- Neither the UUID nor the server name is sent. The proxy takes both from the connection the message arrived on, which
  is what stops a client from forging a session to pin itself to an arbitrary backend.
- The proxy stores `UUID -> (server, expiry)` in `plugins/unplugged-afk/sessions.json`, rewritten on every change and
  pruned of expired entries at startup. An entry routes exactly one login and is then discarded.
- Expiry is the requested duration plus a five minute grace period. Overshooting is intentional.
- There is no `SESSION_END`. The unplugged player sits on a connection that drops every outgoing packet, so it cannot
  report its own death; the proxy expires the entry on its own timer instead.

## Features

- **Go green:** Turn off your computer and leave your unplugged player to AFK for you
- **Customizable limits:** Server operators can configure the maximum duration, as well as a global cap of unplugged
  players
- **Admin control:** Server admins have commands allowing them to inspect unplugged players, as well as debug various
  aspects of the plugin
- **Capped:** Configurable limit to how many unplugged players can exist at the same time, to prevent resource
  exhaustion on AFK players
- **Proxy aware:** Unplugging behind Velocity disconnects you from the whole network, and returning puts you back on the
  server your unplugged player is on

### Planned Features

- **Bypass max duration:** A permission to bypass the max duration to allow staff or VIPs to enjoy longer AFK times
- **Bypass cap:** A permission to bypass the cap to allow staff or VIPs to go unplugged even when the limit has been
  reached
- **Historical data:** Persist historical data to disk to allow staff to review players' interaction with the mod and
  empower them to act on support requests or reports
- **Re-spawn unplugged players on server reboot:** Automatically re-spawn unplugged players when a backend restarts
- **More admin commands:** Unplug an online player on their behalf, unplug an offline player, etc.

## Commands

### Player Commands

- `/unplug <duration> <reason>` - Disconnects you and spawns an unplugged player in your place for `duration` minutes,
  with `reason`.

### Admin Commands

- `/unplugged info <player>` - Shows the unplugged information for `player`.
- `/unplugged list` - Shows the list of unplugged players, as well as how many slots of the cap are occupied

## Configuration

| Key                 | Description                                                                                                                               | Default | Minimum |
|---------------------|-------------------------------------------------------------------------------------------------------------------------------------------|---------|---------|
| debug               | Enables certain debug functionality, such as dumping a bunch of data such as the player's inventory to the server's files when they unplug | `false` |         |
| maxUnpluggedPlayers | The maximum amount of unplugged players that can exist at the same time                                                                   | `16`    | `1`     |
| maxDurationMins     | The maximum duration a player can unplug for, in minutes                                                                                  | `480`   | `1`     |

The proxy companion has no configuration.

## Permissions

| Key                         | Description                                                              | Granted by            |
|-----------------------------|--------------------------------------------------------------------------|-----------------------|
| `unplugged-afk.unplug`      | Gives access to the `/unplug` player command                             | Everyone, by default  |
| `unplugged-afk.admin`       | Gives access to all admin commands                                       |                       |
| `unplugged-afk.admin.info`  | Gives access to the `/unplugged info` admin command                      | `unplugged-afk.admin` |
| `unplugged-afk.admin.list`  | Gives access to the `/unplugged list` admin command                      | `unplugged-afk.admin` |
| `unplugged-afk.admin.debug` | Gives access to the `/unplugged debug` admin command and its subcommands | `unplugged-afk.admin` |

## Debug Functionality

All the functionality described in this section is gated behind the `debug` configuration flag.

### Admin Commands

- `/unplugged debug spawn-fake <duration> [reason]` - Spawns a fake player with a random UUID and name for `duration`
  minutes, with an optional `reason`.

### Functionality

- A text file containing various data such as Unix timestamp, position, dimension, inventory contents, etc. is dumped in
  `plugins/unplugged-afk/dumps/` every time a real player unplugs.

## Acknowledgements

Huge thanks to Sakura Ryoko for their mod [Unplugged-AFK](https://github.com/sakura-ryoko/unplugged-afk) of which this
is a port of. This plugin wouldn't exist without their work.

## License

This project is licensed under [LGPL3-or-later](LICENSE).
