package com.caqy.feign;

import com.caqy.feign.decoder.AutoDetectDecoder;
import com.caqy.feign.encoder.AutoDetectEncoder;
import feign.*;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.okhttp.OkHttpClient;
import feign.slf4j.Slf4jLogger;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import org.springframework.beans.factory.FactoryBean;

import java.util.ArrayList;
import java.util.List;

public class FeignClientFactoryBean<T> implements FactoryBean<T> {

    private Class<T> feignClientInterface;
    private String url;
    private Feign.Builder feignBuilder;
    private Decoder decoder = AutoDetectDecoder.getInstance();
    private Encoder encoder = AutoDetectEncoder.getInstance();
    private boolean autoSetCookies = false;

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

    public void setAutoSetCookies(boolean autoSetCookies) {
        this.autoSetCookies = autoSetCookies;
    }

    private Feign.Builder getFeignBuilder() {
        if (feignBuilder == null)
            feignBuilder = Feign.builder()
                    .logger(new Slf4jLogger())
                    .logLevel(Logger.Level.FULL)
                    .decode404()
                    .options(new Request.Options(30000, 30000))
                    .retryer(new Retryer.Default(5000, 5000, 3));
        feignBuilder.encoder(encoder)
                .decoder(decoder)
                .client(getClient());
        return feignBuilder;
    }

    private Client getClient() {
        okhttp3.OkHttpClient.Builder clientBuilder = new okhttp3.OkHttpClient.Builder();
        if (autoSetCookies)
            clientBuilder.cookieJar(AutoCookieJar.getInstance());
        return new OkHttpClient(clientBuilder.build());
    }

    @Override
    public T getObject() {
        Feign.Builder builder = getFeignBuilder();
        return builder.target(feignClientInterface, url);
    }

    @Override
    public Class<?> getObjectType() {
        return feignClientInterface;
    }
}
