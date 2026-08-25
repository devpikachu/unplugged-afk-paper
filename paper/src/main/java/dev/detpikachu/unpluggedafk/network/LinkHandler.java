package dev.detpikachu.unpluggedafk.network;

import dev.detpikachu.unpluggedafk.UnpluggedAfk;
import dev.detpikachu.unpluggedafk.common.network.Handshake;
import dev.detpikachu.unpluggedafk.common.network.Message;
import dev.detpikachu.unpluggedafk.common.network.Protocol;
import dev.detpikachu.unpluggedafk.common.network.messages.Auth;
import dev.detpikachu.unpluggedafk.common.network.messages.Challenge;
import dev.detpikachu.unpluggedafk.common.network.messages.Ready;
import dev.detpikachu.unpluggedafk.common.network.messages.Relay;
import dev.detpikachu.unpluggedafk.common.network.messages.SessionAck;
import dev.detpikachu.unpluggedafk.config.LinkOptions;
import dev.detpikachu.unpluggedafk.session.SessionRegistry;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.jetbrains.annotations.ApiStatus;

import static dev.detpikachu.unpluggedafk.UnpluggedAfk.LOGGER;
import static dev.detpikachu.unpluggedafk.UnpluggedAfk.logDebug;

@ApiStatus.Internal
public final class LinkHandler extends SimpleChannelInboundHandler<Message> {

    private final LinkClient client;
    private final LinkOptions options;

    private boolean challenged;
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
    protected void channelRead0(ChannelHandlerContext context, Message message) {
        switch (message.getType()) {
            case CHALLENGE -> onChallenge(context, (Challenge) message);
            case READY -> onReady(context, (Ready) message);
            case RELAY -> {
                if (this.requireReady(context, message)) {
                    onRelay((Relay) message);
                }
            }
            case SESSION_ACK -> {
                if (this.requireReady(context, message)) {
                    this.client.acknowledged((SessionAck) message);
                }
            }
            case HEARTBEAT -> this.requireReady(context, message);
            case AUTH, GOODBYE, SYNC, SESSION_START, SESSION_END ->
                logDebug("Ignoring {} from the proxy. A backend never handles it.", message.getType());
        }
    }

    private boolean requireReady(ChannelHandlerContext context, Message message) {
        if (this.ready) {
            return true;
        }

        this.client.warnOnce("The proxy sent {} before the link was ready. Closing.", message.getType());
        close(context);
        return false;
    }

    private void onChallenge(ChannelHandlerContext context, Challenge challenge) {
        if (this.challenged) {
            this.client.warnOnce("The proxy sent a second CHALLENGE on an open link. Closing.");
            close(context);
            return;
        }

        this.challenged = true;

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

    private void onReady(ChannelHandlerContext context, Ready message) {
        if (!this.challenged || this.ready) {
            this.client.warnOnce("The proxy sent an unexpected READY. Closing.");
            close(context);
            return;
        }

        if (!message.accepted()) {
            this.client.errorOnce("The proxy refused the link. {}", message.reason());
            close(context);
            return;
        }

        context.pipeline()
                .replace(ReadTimeoutHandler.class, "timeout", new ReadTimeoutHandler(Protocol.IDLE_TIMEOUT_SECS));
        this.ready = true;
        this.client.established(context.channel(), this.options.getServerName());
    }

    private static void onRelay(Relay relay) {
        final var plugin = UnpluggedAfk.getInstance();
        plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> deliver(relay));
    }

    private static void deliver(Relay relay) {
        final var bot = SessionRegistry.getInstance().find(relay.getUuid());

        if (bot == null) {
            logDebug("Dropped a relayed message on {}. Bot {} is gone.", relay.getChannel(), relay.getUuid());
            return;
        }

        try {
            logDebug(
                    "Delivering {} byte(s) from the proxy to bot {} on channel {}.",
                    relay.getPayload().length,
                    relay.getUuid(),
                    relay.getChannel());
            UnpluggedAfk.getInstance()
                    .getServer()
                    .getMessenger()
                    .dispatchIncomingMessage(
                            bot.getBukkitEntity().getConnection(), relay.getChannel(), relay.getPayload());
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "The proxy relayed a message to bot {} on the invalid channel {}.",
                    relay.getUuid(),
                    relay.getChannel(),
                    exception);
        }
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
