package netty.websocket.resource;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ws接口注解.
 *
 * @author ssp
 *
 * @see Component
 * @since 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface WebSocketEndpoint {

    @AliasFor(annotation = Component.class, attribute = "value")
    String value() default "";

}
