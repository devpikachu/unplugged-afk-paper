package dev.detpikachu.unpluggedafk.common.network.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.util.List;

@ApiStatus.Internal
public final class MessageDecoder extends MessageToMessageDecoder<ByteBuf> {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws IOException {
        try (var stream = new ByteBufInputStream(in)) {
            out.add(MessageCodec.read(stream));
        }
    }
}
