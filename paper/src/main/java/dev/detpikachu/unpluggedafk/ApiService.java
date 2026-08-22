package dev.detpikachu.unpluggedafk;

import dev.detpikachu.unpluggedafk.api.UnpluggedAfkApi;
import dev.detpikachu.unpluggedafk.api.UnpluggedPlayerInfo;
import dev.detpikachu.unpluggedafk.player.UnpluggedServerPlayer;
import dev.detpikachu.unpluggedafk.session.SessionRegistry;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApiStatus.Internal
final class ApiService implements UnpluggedAfkApi {

    @Override
    public boolean isUnplugging(UUID uuid) {
        return SessionRegistry.getInstance().isUnplugging(uuid);
    }

    @Override
    public boolean isUnplugged(UUID uuid) {
        return SessionRegistry.getInstance().isUnplugged(uuid);
    }

    @Override
    public Optional<UnpluggedPlayerInfo> find(UUID uuid) {
        return Optional.ofNullable(SessionRegistry.getInstance().find(uuid)).map(UnpluggedServerPlayer::toInfo);
    }

    @Override
    public List<UnpluggedPlayerInfo> all() {
        return SessionRegistry.getInstance().all().stream().map(UnpluggedServerPlayer::toInfo).toList();
    }
}
