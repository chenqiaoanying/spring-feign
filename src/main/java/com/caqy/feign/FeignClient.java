package com.caqy.feign;

import feign.codec.Decoder;
import feign.codec.Encoder;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface FeignClient {
    String name() default "";

    String url();

    boolean autoSetCookies() default true;

    Class<? extends Encoder> encoderClass() default Encoder.class;

    Class<? extends Decoder> decoderClass() default Decoder.class;
}
