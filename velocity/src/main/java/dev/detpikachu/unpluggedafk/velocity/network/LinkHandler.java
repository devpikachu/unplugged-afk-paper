package dev.detpikachu.unpluggedafk.velocity.network;

import com.velocitypowered.api.proxy.ProxyServer;
import dev.detpikachu.unpluggedafk.common.network.Handshake;
import dev.detpikachu.unpluggedafk.common.network.Message;
import dev.detpikachu.unpluggedafk.common.network.Protocol;
import dev.detpikachu.unpluggedafk.common.network.messages.Auth;
import dev.detpikachu.unpluggedafk.common.network.messages.Challenge;
import dev.detpikachu.unpluggedafk.common.network.messages.Goodbye;
import dev.detpikachu.unpluggedafk.common.network.messages.Ready;
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
    public void channelActive(ChannelHandlerContext context) {
        this.nonce = Handshake.newNonce();
        send(context, new Challenge(Protocol.VERSION, this.nonce));
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) {
        if (this.serverName == null) {
            return;
        }

        this.logger.info("Backend {} unlinked.", this.serverName);
        this.serverName = null;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        this.logger.warn("Link error from {}. Closing.", context.channel().remoteAddress(), cause);
        close(context);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, Message message) {
        if (message instanceof Auth auth) {
            onAuth(context, auth);
            return;
        }

        if (this.serverName == null) {
            refuse(context, "Expected AUTH first but got " + message.getType() + ".");
            return;
        }

        if (message instanceof Goodbye(String reason)) {
            this.logger.info("Backend {} said goodbye: {}", this.serverName, reason);
            close(context);
            return;
        }

        this.logger.debug(
                "Ignoring {} from backend {}, not carried by this build yet.", message.getType(), this.serverName);
    }

    private void onAuth(ChannelHandlerContext context, Auth auth) {
        if (this.serverName != null) {
            refuse(context, "Backend " + this.serverName + " is already authenticated on this connection.");
            return;
        }

        if (auth.protocolVersion() != Protocol.VERSION) {
            refuse(context, "Protocol version mismatch. Ensure both plugins are the same version.");
            return;
        }

        if (this.nonce == null || !Handshake.verify(this.secret, this.nonce, auth.signature())) {
            refuse(context, "The backend's link.secret does not match this proxy's.");
            return;
        }

        if (this.proxyServer.getServer(auth.serverName()).isEmpty()) {
            refuse(
                    context,
                    "No server named "
                            + auth.serverName()
                            + " is registered on this proxy. Registered: "
                            + registeredNames()
                            + ".");
            return;
        }

        this.nonce = null;
        this.serverName = auth.serverName();
        context.pipeline().remove(ReadTimeoutHandler.class);
        send(context, new Ready(true, ""));
        this.logger.info(
                "Backend {} linked from {}.",
                auth.serverName(),
                context.channel().remoteAddress());
    }

    private void refuse(ChannelHandlerContext context, String reason) {
        this.logger.warn("Refusing a link from {}. {}", context.channel().remoteAddress(), reason);
        sendAndClose(context, new Ready(false, reason));
    }

    private String registeredNames() {
        return this.proxyServer.getAllServers().stream()
                .map(server -> server.getServerInfo().getName())
                .collect(Collectors.joining(", "));
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    private static void send(ChannelHandlerContext context, Message message) {
        context.writeAndFlush(message);
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    private static void sendAndClose(ChannelHandlerContext context, Message message) {
        context.writeAndFlush(message).addListener(ChannelFutureListener.CLOSE);
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    private static void close(ChannelHandlerContext context) {
        context.close();
    }
}
