package netty.websocket;

import netty.websocket.resource.ResourceDispatcher;
import netty.websocket.server.WebSocketNettyServer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;

@SpringBootTest
@RunWith(SpringRunner.class)
public class WebSocketNettyServerTest {

    @Resource
    private ResourceDispatcher resourceDispatcher;

    @Test
    public void startWebSocketServer() throws InterruptedException {
        WebSocketNettyServer.createServer(6680, resourceDispatcher).start();
        new CountDownLatch(1).await();
    }
}
