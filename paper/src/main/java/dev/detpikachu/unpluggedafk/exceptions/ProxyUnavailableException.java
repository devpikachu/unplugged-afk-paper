package dev.detpikachu.unpluggedafk.exceptions;

import org.jetbrains.annotations.ApiStatus;

import java.util.UUID;

@ApiStatus.Internal
public final class ProxyUnavailableException extends UnplugFailedException {

    public ProxyUnavailableException(UUID uuid, String name) {
        super("Refused to unplug " + name + " (" + uuid + "): the link to the proxy is down.");
    }
}
