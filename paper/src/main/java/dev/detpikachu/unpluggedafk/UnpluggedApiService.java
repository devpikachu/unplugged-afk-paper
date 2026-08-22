package dev.detpikachu.unpluggedafk;

import dev.detpikachu.unpluggedafk.api.UnpluggedAfkApi;
import dev.detpikachu.unpluggedafk.api.UnpluggedPlayerInfo;
import dev.detpikachu.unpluggedafk.player.UnpluggedServerPlayer;
import org.jetbrains.annotations.ApiStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApiStatus.Internal
public final class UnpluggedApiService implements UnpluggedAfkApi {

    @Override
    public boolean isUnplugging(UUID uuid) {
        return UnpluggedPlayerManager.getInstance().isUnplugging(uuid);
    }

    @Override
    public boolean isUnplugged(UUID uuid) {
        return UnpluggedPlayerManager.getInstance().isUnplugged(uuid);
    }

    @Override
    public Optional<UnpluggedPlayerInfo> find(UUID uuid) {
        return Optional.ofNullable(UnpluggedPlayerManager.getInstance().find(uuid)).map(UnpluggedApiService::map);
    }

    @Override
    public List<UnpluggedPlayerInfo> all() {
        return UnpluggedPlayerManager.getInstance().all().stream().map(UnpluggedApiService::map).toList();
    }

    private static UnpluggedPlayerInfo map(UnpluggedServerPlayer player) {
        return new UnpluggedPlayerInfo(
                player.getUUID(),
                player.getName().getString(),
                player.getDurationMins(),
                player.getReason(),
                Instant.ofEpochMilli(player.getStartAtMillis()),
                Instant.ofEpochMilli(player.getTimeoutAtMillis()),
                player.isFake()
        );
    }
}
