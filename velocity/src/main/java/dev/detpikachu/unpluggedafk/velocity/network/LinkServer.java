package dev.detpikachu.unpluggedafk.velocity.network;

import dev.detpikachu.unpluggedafk.common.network.Protocol;
import dev.detpikachu.unpluggedafk.common.network.codec.MessageDecoder;
import dev.detpikachu.unpluggedafk.common.network.codec.MessageEncoder;
import dev.detpikachu.unpluggedafk.common.network.messages.Relay;
import dev.detpikachu.unpluggedafk.velocity.UnpluggedAfkVelocity;
import dev.detpikachu.unpluggedafk.velocity.compat.tab.TabBridge;
import dev.detpikachu.unpluggedafk.velocity.config.Options;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApiStatus.Internal
public final class LinkServer {

    private static final int HANDSHAKE_TIMEOUT_SECS = 10;
    private static final int ACCEPTOR_THREADS = 1;
    private static final int WORKER_THREADS = 2;
    private static final long BOOT_QUIET_MILLIS = 500;
    private static final long BOOT_CAP_MILLIS = 8000;
    private static final long BOOT_POLL_MILLIS = 50;

    private final Logger logger;
    private final ConcurrentHashMap<String, Channel> links = new ConcurrentHashMap<>();

    private volatile long startedAt;
    private volatile long lastLinkAt;
    private volatile int expectedBackends;

    private volatile @Nullable EventLoopGroup acceptors;
    private volatile @Nullable EventLoopGroup workers;
    private volatile @Nullable Channel channel;

    public LinkServer(Logger logger) {
        this.logger = logger;
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    public void start(UnpluggedAfkVelocity plugin, BotPlayerBridge botPlayerBridge, @Nullable TabBridge tabBridge) {
        final var options = Options.getInstance().getLink();
        final var acceptorGroup = new MultiThreadIoEventLoopGroup(ACCEPTOR_THREADS, NioIoHandler.newFactory());
        final var workerGroup = new MultiThreadIoEventLoopGroup(WORKER_THREADS, NioIoHandler.newFactory());

        this.startedAt = System.currentTimeMillis();
        this.lastLinkAt = this.startedAt;
        this.expectedBackends = plugin.getProxyServer().getAllServers().size();
        this.acceptors = acceptorGroup;
        this.workers = workerGroup;

        final var future = new ServerBootstrap()
                .group(acceptorGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {

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
                                .addLast(
                                        "handler",
                                        new LinkHandler(LinkServer.this, plugin, botPlayerBridge, tabBridge));
                    }
                })
                .bind(options.getHost(), options.getPort());

        this.channel = future.channel();
        future.addListener(result -> {
            if (result.isSuccess()) {
                this.logger.info("Link listening on {}:{}.", options.getHost(), options.getPort());
                return;
            }

            this.logger.error("Link could not bind {}:{}.", options.getHost(), options.getPort(), result.cause());
        });
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    public void stop() {
        this.links.clear();

        final var channel = this.channel;
        if (channel != null) {
            channel.close();
            this.channel = null;
        }

        final var acceptorGroup = this.acceptors;
        if (acceptorGroup != null) {
            acceptorGroup.shutdownGracefully();
            this.acceptors = null;
        }

        final var workerGroup = this.workers;
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
            this.workers = null;
        }
    }

    public void awaitSettled() {
        final var deadline = this.startedAt + BOOT_CAP_MILLIS;

        while (System.currentTimeMillis() < deadline
                && this.links.size() < this.expectedBackends
                && System.currentTimeMillis() - this.lastLinkAt < BOOT_QUIET_MILLIS) {
            try {
                Thread.sleep(BOOT_POLL_MILLIS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    boolean isLinked(String serverName) {
        final var link = this.links.get(serverName);
        return link != null && link.isActive();
    }

    void linked(String serverName, Channel link) {
        this.lastLinkAt = System.currentTimeMillis();
        this.links.put(serverName, link);
    }

    boolean unlinked(String serverName, Channel link) {
        return this.links.remove(serverName, link);
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    public void relay(String serverName, UUID uuid, String channelName, byte[] payload) {
        final var link = this.links.get(serverName);

        if (link == null || !link.isActive()) {
            return;
        }

        if (payload.length > Protocol.MAX_PAYLOAD_BYTES) {
            this.logger.warn(
                    "Dropped a {} byte(s) plugin message on {} for bot {}. The link carries at most {}.",
                    payload.length,
                    channelName,
                    uuid,
                    Protocol.MAX_PAYLOAD_BYTES);
            return;
        }

        link.writeAndFlush(new Relay(uuid, channelName, payload));
    }
}
