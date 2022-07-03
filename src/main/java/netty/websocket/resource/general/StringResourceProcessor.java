package netty.websocket.resource.general;

import netty.websocket.resource.ResourceProcessor;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;

/**
 * String text ResourceProcessor.
 */
@Slf4j
public class StringResourceProcessor implements ResourceProcessor<String> {

    @Override
    public void handle(Channel client, String msg) {
        log.debug("client[{}] ---> processor:{}", client.id(), msg);
        sendText(client, "ack:" + msg);
    }

    @Override
    public void sendText(Channel client, String text) {
        log.debug("processor ---> client[{}]:{}", client.id(), text);
        client.writeAndFlush(new TextWebSocketFrame(text));
    }

}
