package dev.detpikachu.unpluggedafk.network;

import dev.detpikachu.unpluggedafk.common.network.Protocol;
import dev.detpikachu.unpluggedafk.common.network.codec.MessageDecoder;
import dev.detpikachu.unpluggedafk.common.network.codec.MessageEncoder;
import dev.detpikachu.unpluggedafk.common.network.messages.Goodbye;
import dev.detpikachu.unpluggedafk.config.Options;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.TimeUnit;

import static dev.detpikachu.unpluggedafk.UnpluggedAfk.LOGGER;

@ApiStatus.Internal
public final class LinkClient {

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int HANDSHAKE_TIMEOUT_SECS = 10;
    private static final int WORKER_THREADS = 1;
    private static final int BACKOFF_MIN_SECS = 1;
    private static final int BACKOFF_MAX_SECS = 60;
    private static final int SHUTDOWN_WAIT_SECS = 1;
    private static final String GOODBYE_DISABLED = "Plugin disabled";

    private volatile boolean running;
    private volatile @Nullable Channel channel;

    private @Nullable EventLoopGroup workers;
    private @Nullable Bootstrap bootstrap;

    private boolean quiet;
    private int backoffSecs;

    public void start() {
        if (this.running) {
            return;
        }

        final var options = Options.getInstance().getLink();
        final var group = new MultiThreadIoEventLoopGroup(WORKER_THREADS, NioIoHandler.newFactory());

        this.quiet = false;
        this.backoffSecs = BACKOFF_MIN_SECS;
        this.workers = group;
        this.bootstrap = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MS)
                .remoteAddress(options.getHost(), options.getPort())
                .handler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel socket) {
                        socket.pipeline()
                                .addLast("timeout", new ReadTimeoutHandler(HANDSHAKE_TIMEOUT_SECS))
                                .addLast(
                                        "splitter",
                                        new LengthFieldBasedFrameDecoder(
                                                Protocol.MAX_FRAME_BYTES,
                                                0,
                                                Protocol.LENGTH_FIELD_BYTES,
                                                0,
                                                Protocol.LENGTH_FIELD_BYTES))
                                .addLast("decoder", new MessageDecoder())
                                .addLast("prepender", new LengthFieldPrepender(Protocol.LENGTH_FIELD_BYTES))
                                .addLast("encoder", new MessageEncoder())
                                .addLast("handler", new LinkHandler(LinkClient.this, options));
                    }
                });
        this.running = true;

        connect();
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    public void stop() {
        if (!this.running) {
            return;
        }
        this.running = false;

        final var channel = this.channel;
        if (channel != null && channel.isActive()) {
            final var future = channel.writeAndFlush(new Goodbye(GOODBYE_DISABLED));

            try {
                future.await(SHUTDOWN_WAIT_SECS, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }

        if (this.workers != null) {
            this.workers.shutdownGracefully(0, SHUTDOWN_WAIT_SECS, TimeUnit.SECONDS);
        }

        this.channel = null;
        this.workers = null;
        this.bootstrap = null;
    }

    void established(Channel channel, String serverName) {
        this.channel = channel;
        this.quiet = false;
        this.backoffSecs = BACKOFF_MIN_SECS;
        LOGGER.info("Linked to the proxy at {} as {}.", channel.remoteAddress(), serverName);
    }

    void disconnected(boolean wasReady) {
        this.channel = null;

        if (wasReady) {
            warnOnce("Link to the proxy lost. Reconnecting in the background.");
        }

        scheduleReconnect();
    }

    void warnOnce(String message, Object... arguments) {
        if (this.quiet) {
            return;
        }

        this.quiet = true;
        LOGGER.warn(message, arguments);
    }

    void errorOnce(String message, Object... arguments) {
        if (this.quiet) {
            return;
        }

        this.quiet = true;
        LOGGER.error(message, arguments);
    }

    private void connect() {
        if (this.bootstrap == null || !this.running) {
            return;
        }

        this.bootstrap.connect().addListener(attempt -> {
            if (attempt.isSuccess()) {
                return;
            }

            warnOnce("Cannot reach the proxy link. Retrying in the background.", attempt.cause());
            scheduleReconnect();
        });
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    private void scheduleReconnect() {
        if (this.workers == null || !this.running) {
            return;
        }

        final var delay = this.backoffSecs;
        this.backoffSecs = Math.max(this.backoffSecs * 2, BACKOFF_MAX_SECS);
        this.workers.schedule(this::connect, delay, TimeUnit.SECONDS);
    }
}
