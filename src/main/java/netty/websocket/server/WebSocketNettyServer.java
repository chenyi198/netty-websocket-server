package netty.websocket.server;

import netty.websocket.resource.ResourceDispatcher;
import netty.websocket.server.handler.WebSocketServerHandler0;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebSocketNettyServer启动线程封装类.
 *
 * @author ssp
 * @since 1.0
 */
@Slf4j
public class WebSocketNettyServer extends Thread {

    private static final String THREAD_NAME_PREFIX = "websocket-netty-server-";
    private static final AtomicInteger SEQ_NUM = new AtomicInteger();

    private String host;
    private int port;

    private ResourceDispatcher resourceDispatcher;

    public static WebSocketNettyServer createServer(int port, ResourceDispatcher resourceDispatcher) {
        Assert.notNull(resourceDispatcher, "resourceDispatcher is null!");
        return new WebSocketNettyServer(port, resourceDispatcher);
    }

    public WebSocketNettyServer(int port, ResourceDispatcher resourceDispatcher) {
        this("0.0.0.0", port, resourceDispatcher);
    }

    public WebSocketNettyServer(String host, int port, ResourceDispatcher resourceDispatcher) {
        super(Thread.currentThread().getThreadGroup(), THREAD_NAME_PREFIX + SEQ_NUM.getAndIncrement());
        this.host = host;
        this.port = port;
        this.resourceDispatcher = resourceDispatcher;
    }

    @Override
    public void run() {
        bootstrap();
    }

    private void bootstrap() {

        final EventLoopGroup bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("acceptor-"));
        final EventLoopGroup workerGroup = new NioEventLoopGroup(0, new DefaultThreadFactory("io-worker-"));
        final EventLoopGroup businessGroup = new DefaultEventLoopGroup(0, new DefaultThreadFactory("business-"));

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();

            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new HttpServerCodec())
                                    .addLast(new HttpObjectAggregator(65536))
                                    .addLast(businessGroup, new WebSocketServerHandler0(resourceDispatcher));
                        }
                    });

            Channel serverChannel = bootstrap.bind(host, port).sync().channel();
            serverChannel.closeFuture().sync();
        } catch (Exception e) {
            log.error("WebSocket Server start fail!", e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

}
