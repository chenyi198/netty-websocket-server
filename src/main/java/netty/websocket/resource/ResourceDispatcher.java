package netty.websocket.resource;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Optional;

/**
 * 客户端请求处理调度器.
 * <p>资源接口池.
 * <p>识别请求并将请求分发到对应后置的ResourceProcessor上.
 *
 * @author ssp
 * @since 1.0
 */
@Component
@Slf4j
public class ResourceDispatcher {

    /**
     * url-ResourceProcessor映射.
     */
    @Resource
    private Map<String, ResourceProcessor<?>> resourceProcessorMapping;

    public <T> void dispatch(Channel channel, String url, T text) {
        @SuppressWarnings("unchecked")
        Optional<ResourceProcessor<T>> resourceProcessor = Optional.ofNullable((ResourceProcessor<T>) resourceProcessorMapping.get(url));
        resourceProcessor.ifPresent(processor -> processor.handle(channel, text));
    }

}
