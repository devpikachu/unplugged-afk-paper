package dev.detpikachu.unpluggedafk.velocity.network;

import com.velocitypowered.api.proxy.ProxyServer;
import dev.detpikachu.unpluggedafk.common.Protocol;
import dev.detpikachu.unpluggedafk.velocity.Constants.Link;
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

@ApiStatus.Internal
public final class LinkServer {

    private final ProxyServer proxyServer;
    private final Logger logger;

    private @Nullable EventLoopGroup acceptors;
    private @Nullable EventLoopGroup workers;
    private @Nullable Channel channel;

    public LinkServer(ProxyServer proxyServer, Logger logger) {
        this.proxyServer = proxyServer;
        this.logger = logger;
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    public void start() {
        final var link = Options.getInstance().getLink();
        final var acceptorGroup = new MultiThreadIoEventLoopGroup(Link.ACCEPTOR_THREADS, NioIoHandler.newFactory());
        final var workerGroup = new MultiThreadIoEventLoopGroup(Link.WORKER_THREADS, NioIoHandler.newFactory());

        this.acceptors = acceptorGroup;
        this.workers = workerGroup;

        final var future = new ServerBootstrap().group(acceptorGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel socket) {
                        socket.pipeline()
                                .addLast("timeout", new ReadTimeoutHandler(Link.HANDSHAKE_TIMEOUT_SECS))
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
                                .addLast("handler", new LinkHandler(proxyServer, logger, link.secret()));
                    }
                })
                .bind(link.host(), link.port());

        this.channel = future.channel();
        future.addListener(result -> {
            if (result.isSuccess()) {
                this.logger.info("Link listening on {}:{}.", link.host(), link.port());
                return;
            }

            this.logger.error("Link could not bind {}:{}.", link.host(), link.port(), result.cause());
        });
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    public void stop() {
        if (this.channel != null) {
            this.channel.close();
            this.channel = null;
        }

        if (this.acceptors != null) {
            this.acceptors.shutdownGracefully();
            this.acceptors = null;
        }

        if (this.workers != null) {
            this.workers.shutdownGracefully();
            this.workers = null;
        }
    }
}
