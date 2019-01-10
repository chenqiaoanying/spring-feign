package com.caqy.feign;

import feign.Feign;
import feign.Logger;
import feign.Request;
import feign.Retryer;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.okhttp.OkHttpClient;
import feign.slf4j.Slf4jLogger;
import org.springframework.beans.factory.FactoryBean;

public class FeignClientFactoryBean<T> implements FactoryBean<T> {

    private Class<T> feignClientInterface;
    private String url;
    private Feign.Builder feignBuilder;
    private Decoder decoder;
    private Encoder encoder;

    public FeignClientFactoryBean(Class<T> feignClientInterface) {
        this.feignClientInterface = feignClientInterface;
    }

    public void setFeignBuilder(Feign.Builder feignBuilder) {
        this.feignBuilder = feignBuilder;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setDecoder(Decoder decoder) {
        this.decoder = decoder;
    }

    public void setEncoder(Encoder encoder) {
        this.encoder = encoder;
    }

    private Feign.Builder getFeignBuilder() {
        if (feignBuilder == null)
            feignBuilder = Feign.builder()
                    .encoder(DefaultEncoder.getInstance())
                    .decoder(DefaultDecoder.getInstance())
                    .client(new OkHttpClient())
                    .logger(new Slf4jLogger())
                    .logLevel(Logger.Level.FULL)
                    .decode404()
                    .options(new Request.Options(1000, 3500))
                    .retryer(new Retryer.Default(5000, 5000, 3));
        return feignBuilder;
    }

    private void configureFeignBuilder(Feign.Builder builder) {
        if (encoder != null)
            builder.encoder(encoder);
        if (decoder != null)
            builder.decoder(decoder);
    }

    @Override
    public T getObject() {
        Feign.Builder builder = getFeignBuilder();
        configureFeignBuilder(builder);
        return builder.target(feignClientInterface, url);
    }

    @Override
    public Class<?> getObjectType() {
        return feignClientInterface;
    }
}
