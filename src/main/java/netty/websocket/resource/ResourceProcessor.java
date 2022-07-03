package netty.websocket.resource;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

/**
 * 资源接口需提供的各项操作定义接口.
 *
 * @param <T> 收到的客户端的数据类型
 *
 * @author ssp
 * @since 1.0
 */
public interface ResourceProcessor<T> {

    void handle(Channel client, T msg);

    default void sendText(Channel client, String text) {
        client.writeAndFlush(new TextWebSocketFrame(text));
    }

}
