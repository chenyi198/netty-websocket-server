package netty.websocket.server.handler;

import netty.websocket.resource.ResourceDispatcher;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import static io.netty.handler.codec.http.HttpUtil.isKeepAlive;

/**
 * WebSocket Server 核心handler.
 * <h3>主要职责:
 * <li>Websocket通道建立，协议升级应答：对客户端的HTTP#Upgrade:WebSocket请求，响应HTTP#101 Switching Protocols应答，来完成websocket通道的建立.
 * <li>WebSocketServerHandshaker：实际完成HTTP#Upgrade:Websocket请求握手应答，并将对应WebSocket协议版本的encoder和decoder编解码器添加到客户端channel的pipeline中.
 * <li>客户端-请求资源地址映射：channel.attr( ),CHANNEL_ATTR_REQ_RESOURCE_URL.
 *
 * @author ssp
 * @since 1.0
 */
@Slf4j
public class WebSocketServerHandler0 extends SimpleChannelInboundHandler<Object> {

    private static final AttributeKey<String> CHANNEL_ATTR_REQ_RESOURCE_URL = AttributeKey.valueOf("channel_request_resource_url");
    private static final WebSocketServerHandshakerFactory WS_HANDSHAKER_FACTORY = new WebSocketServerHandshakerFactory(null, null, false);

    private WebSocketServerHandshaker handshaker;

    private final ResourceDispatcher resourceDispatcher;

    public WebSocketServerHandler0(ResourceDispatcher resourceDispatcher) {
        this.resourceDispatcher = resourceDispatcher;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof WebSocketFrame) {
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        } else if (msg instanceof FullHttpRequest) {
            httpUpgradeHandshake(ctx, (FullHttpRequest) msg); //处理HTTP#Upgrade
        }
    }

    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        // socket close
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            log.debug("CloseWebSocketFrame! channel:{}", ctx.channel().id());
            return;
        }
        // ping
        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            log.debug("PingWebSocketFrame! channel:{}", ctx.channel().id());
            return;
        }
        // 非文本消息
        if (!(frame instanceof TextWebSocketFrame)) {
            throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass().getName()));
        }

        // to dispatch
        TextWebSocketFrame textWebSocketFrame = (TextWebSocketFrame) frame;

        String text = textWebSocketFrame.text();
        log.debug("收到客户端[{}]的数据:[{}]", ctx.channel().id(), text);

        //save req_resource_url into channel for future
        String url = ctx.channel().attr(CHANNEL_ATTR_REQ_RESOURCE_URL).get();
        resourceDispatcher.dispatch(ctx.channel(), url, text);
    }

    /**
     * 处理第一次的HTTP#Upgrde请求，作出应答
     */
    private void httpUpgradeHandshake(ChannelHandlerContext ctx, FullHttpRequest req) {

        //upgrade is'not websocket
        if (!req.decoderResult().isSuccess() || !req.headers().containsValue(HttpHeaderNames.UPGRADE, HttpHeaderValues.WEBSOCKET, true)) { //response BAD_REQUEST
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }

        handshaker = WS_HANDSHAKER_FACTORY.newHandshaker(req);

        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            //完成HTTP#Upgrade响应，并根据WebSocket协议版本为该channel的pipeline添加对应版本的wsDecoder、wsEncoder
            handshaker.handshake(ctx.channel(), req);

            String url = req.uri();
            ctx.channel().attr(CHANNEL_ATTR_REQ_RESOURCE_URL).set(url);
        }
    }

    /**
     * 拒绝不合法的请求，并返回错误信息
     */
    private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, DefaultFullHttpResponse res) {
        // 返回应答给客户端
        if (res.status().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
        }
        ChannelFuture f = ctx.channel().writeAndFlush(res);
        // 如果是非Keep-Alive，关闭连接
        if (!isKeepAlive(req) || res.status().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.debug("--->客户端[{}]连接建立！", ctx.channel().id());
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.debug("<---客户端[{}]连接断开！", ctx.channel().id());
        ctx.fireChannelInactive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.debug("客户端[{}]链路异常！", ctx.channel().id(), cause);
        ctx.fireExceptionCaught(cause);
    }

}
