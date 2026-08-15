# Unplugged-AFK Paper

**Unplugged-AFK Paper** is a **feature-incomplete** port / rewrite of the
amazing [Unplugged-AFK](https://github.com/sakura-ryoko/unplugged-afk) Fabric mod by Sakura Ryoko, published under the
LGPL3-or-later license.

This is an eco-friendly Paper plugin that allows Players to "go unplugged" for a certain amount of time. By executing a
command, a bot (or unplugged player) of themselves is spawned to stand AFK at farms, at the same time disconnecting
their client. This allows players to safely shut down their computers, saving electricity and promoting a green approach
to server farming!

Once the time has elapsed, the unplugged player is automatically kicked from the server to free up resources!

## Prerequisites & Installation

- **Server:** Paper or any fork that supports Paper plugins
- **Minecraft Version**: 1.21.x, tested against 1.21.11

## Features

- **Go green:** Turn off your computer and leave your unplugged player to AFK for you
- **Customizable limits:** Server operators can configure the maximum duration, as well as a global cap of unplugged
  players
- **Admin control:** Server admins have commands allowing them to inspect unplugged players, as well as debug various
  aspects of the plugin

### Planned Features

- **Bypass max duration:** A permission to bypass the max duration to allow staff or VIPs to enjoy longer AFK times
- **Bypass cap:** A permission to bypass the cap to allow staff or VIPs to go unplugged even when the limit has been
  reached
- **Historical data:** Persist historical data to disk to allow staff to review players' interaction with the mod and
  empower them to act on support requests or reports
- **Re-spawn unplugged players on server reboot:** Automatically re-spawn unplugged players on server reboot
- **More admin commands:** Unplug an online player on their behalf, unplug an offline player, etc.

## Commands

### Player Commands

- `/unplug <duration> <reason>` - Disconnects you and spawns an unplugged player in your place for `duration` minutes,
  with `reason`.

### Admin Commands

- `/unplugged info <player>` - Shows the unplugged information for `player`.
- `/unplugged list` - Shows the list of unplugged players, as well as how many slots of the cap are occupied
- `/unplugged debug spawn-fake <duration> [reason]` - Spawns a fake player with a random UUID and name for `duration`
  minutes, with an optional `reason`.

## Configuration

| Key             | Description                                              | Default |
|-----------------|----------------------------------------------------------|---------|
| maxDurationMins | The maximum duration a player can unplug for, in minutes | `480`   |

## Acknowledgements

Huge thanks to Sakura Ryoko for their mod [Unplugged-AFK](https://github.com/sakura-ryoko/unplugged-afk) of which this is a port of. This plugin wouldn't exist without their work.

## License

This project is licensed under [LGPL3-or-later](LICENSE).

