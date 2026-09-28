package dev.detpikachu.unpluggedafk.player;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
@ChannelHandler.Sharable
final class OutboundSink extends ChannelOutboundHandlerAdapter {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        ReferenceCountUtil.release(msg);
        promise.trySuccess();
    }
}
