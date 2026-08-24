package dev.detpikachu.unpluggedafk.common.network;

import org.jetbrains.annotations.ApiStatus;

import java.io.DataOutput;
import java.io.IOException;

@ApiStatus.Internal
public interface Message {

    MessageType getType();

    void write(DataOutput out) throws IOException;
}
