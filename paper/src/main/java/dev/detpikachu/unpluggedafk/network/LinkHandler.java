package dev.detpikachu.unpluggedafk.network;

import dev.detpikachu.unpluggedafk.common.network.Handshake;
import dev.detpikachu.unpluggedafk.common.network.Message;
import dev.detpikachu.unpluggedafk.common.network.Protocol;
import dev.detpikachu.unpluggedafk.common.network.messages.Auth;
import dev.detpikachu.unpluggedafk.common.network.messages.Challenge;
import dev.detpikachu.unpluggedafk.common.network.messages.Ready;
import dev.detpikachu.unpluggedafk.config.LinkOptions;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.jetbrains.annotations.ApiStatus;

import static dev.detpikachu.unpluggedafk.UnpluggedAfk.logDebug;

@ApiStatus.Internal
public final class LinkHandler extends SimpleChannelInboundHandler<Message> {

    private final LinkClient client;
    private final LinkOptions options;

    private boolean ready;

    public LinkHandler(LinkClient client, LinkOptions options) {
        this.client = client;
        this.options = options;
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) {
        final var wasReady = this.ready;
        this.ready = false;
        this.client.disconnected(wasReady);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        this.client.warnOnce("Link error talking to the proxy. Retrying in the background.", cause);
        close(context);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, Message message) throws Exception {
        if (message instanceof Challenge challenge) {
            onChallenge(context, challenge);
            return;
        }

        if (message instanceof Ready(boolean accepted, String reason)) {
            onReady(context, accepted, reason);
            return;
        }

        if (!this.ready) {
            this.client.warnOnce("The proxy sent {} before the link was ready. Closing.", message.getType());
            close(context);
            return;
        }

        logDebug("Ignoring {} from the proxy. Unknown message type.", message.getType());
    }

    private void onChallenge(ChannelHandlerContext context, Challenge challenge) {
        if (challenge.protocolVersion() != Protocol.VERSION) {
            this.client.errorOnce(
                    "Link protocol mismatch. This backend uses {}, the proxy uses {}. Use the same version of both plugins.",
                    Protocol.VERSION,
                    challenge.protocolVersion());
            close(context);
            return;
        }

        final var signature = Handshake.sign(this.options.getSecret(), challenge.nonce());
        send(context, new Auth(Protocol.VERSION, this.options.getServerName(), signature));
    }

    private void onReady(ChannelHandlerContext context, boolean accepted, String reason) {
        if (!accepted) {
            this.client.errorOnce("The proxy refused the link. {}", reason);
            close(context);
            return;
        }

        context.pipeline().remove(ReadTimeoutHandler.class);
        this.ready = true;
        this.client.established(context.channel(), this.options.getServerName());
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    private static void send(ChannelHandlerContext context, Message message) {
        context.writeAndFlush(message);
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    private static void close(ChannelHandlerContext context) {
        context.close();
    }
}
