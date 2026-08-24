package dev.detpikachu.unpluggedafk.common.network.codec;

import dev.detpikachu.unpluggedafk.common.network.Message;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;

@ApiStatus.Internal
public final class MessageEncoder extends MessageToByteEncoder<Message> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Message message, ByteBuf out) throws IOException {
        try (var stream = new ByteBufOutputStream(out)) {
            MessageCodec.write(message, stream);
        }
    }
}
