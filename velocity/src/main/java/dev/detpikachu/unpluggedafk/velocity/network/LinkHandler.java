package dev.detpikachu.unpluggedafk.velocity.network;

import com.velocitypowered.api.proxy.ProxyServer;
import dev.detpikachu.unpluggedafk.common.Handshake;
import dev.detpikachu.unpluggedafk.common.Message;
import dev.detpikachu.unpluggedafk.common.Protocol;
import dev.detpikachu.unpluggedafk.common.messages.Auth;
import dev.detpikachu.unpluggedafk.common.messages.Challenge;
import dev.detpikachu.unpluggedafk.common.messages.Goodbye;
import dev.detpikachu.unpluggedafk.common.messages.Ready;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.stream.Collectors;

@ApiStatus.Internal
public final class LinkHandler extends SimpleChannelInboundHandler<Message> {

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final String secret;

    private @Nullable String nonce;
    private @Nullable String serverName;

    public LinkHandler(ProxyServer proxyServer, Logger logger, String secret) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.secret = secret;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        final var challenge = Handshake.newNonce();

        this.nonce = challenge;
        send(ctx, new Challenge(Protocol.VERSION, challenge));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (this.serverName == null) {
            return;
        }

        this.logger.info("Backend {} unlinked.", this.serverName);
        this.serverName = null;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        this.logger.warn("Link error from {}. Closing.", ctx.channel().remoteAddress(), cause);
        close(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message message) {
        if (message instanceof Auth auth) {
            onAuth(ctx, auth);
            return;
        }

        if (this.serverName == null) {
            refuse(ctx, "Expected AUTH first but got " + message.getType() + ".");
            return;
        }

        if (message instanceof Goodbye(String reason)) {
            this.logger.info("Backend {} said goodbye: {}", this.serverName, reason);
            close(ctx);
            return;
        }

        this.logger.debug(
                "Ignoring {} from backend {}, not carried by this build yet.",
                message.getType(),
                this.serverName);
    }

    private void onAuth(ChannelHandlerContext ctx, Auth auth) {
        if (this.serverName != null) {
            refuse(ctx, "Backend " + this.serverName + " is already authenticated on this connection.");
            return;
        }

        if (auth.protocolVersion() != Protocol.VERSION) {
            refuse(
                    ctx,
                    "Protocol version mismatch. This proxy speaks "
                            + Protocol.VERSION
                            + ", the backend speaks "
                            + auth.protocolVersion()
                            + ". Update whichever side is older.");
            return;
        }

        final var challenge = this.nonce;
        final var name = auth.serverName();

        if (challenge == null || !Handshake.verify(this.secret, challenge, auth.signature())) {
            refuse(ctx, "The backend's link.secret does not match this proxy's.");
            return;
        }

        if (this.proxyServer.getServer(name).isEmpty()) {
            refuse(
                    ctx,
                    "No server named " + name + " is registered on this proxy. Registered: " + registeredNames() + ".");
            return;
        }

        this.nonce = null;
        this.serverName = name;
        ctx.pipeline().remove(ReadTimeoutHandler.class);
        send(ctx, new Ready(true, ""));
        this.logger.info("Backend {} linked from {}.", name, ctx.channel().remoteAddress());
    }

    private void refuse(ChannelHandlerContext ctx, String reason) {
        this.logger.warn("Refusing a link from {}. {}", ctx.channel().remoteAddress(), reason);
        sendAndClose(ctx, new Ready(false, reason));
    }

    private String registeredNames() {
        return this.proxyServer.getAllServers()
                .stream()
                .map(server -> server.getServerInfo().getName())
                .collect(Collectors.joining(", "));
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    private static void send(ChannelHandlerContext ctx, Message message) {
        ctx.writeAndFlush(message);
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    private static void sendAndClose(ChannelHandlerContext ctx, Message message) {
        ctx.writeAndFlush(message).addListener(ChannelFutureListener.CLOSE);
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    private static void close(ChannelHandlerContext ctx) {
        ctx.close();
    }
}
