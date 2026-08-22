package dev.detpikachu.unpluggedafk.player;

import com.mojang.authlib.GameProfile;
import org.jetbrains.annotations.ApiStatus;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@ApiStatus.Internal
public record FakeIdentity(UUID uuid, String name) {

    private static final String NAME_PREFIX = "Fakeson_";
    private static final String SUFFIX_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int SUFFIX_LENGTH = 4;

    public static FakeIdentity random() {
        final var random = ThreadLocalRandom.current();
        final var name = new StringBuilder(NAME_PREFIX);

        for (var i = 0; i < SUFFIX_LENGTH; i++) {
            name.append(SUFFIX_ALPHABET.charAt(random.nextInt(SUFFIX_ALPHABET.length())));
        }

        return new FakeIdentity(UUID.randomUUID(), name.toString());
    }

    public GameProfile toProfile() {
        return new GameProfile(this.uuid, this.name);
    }
}
