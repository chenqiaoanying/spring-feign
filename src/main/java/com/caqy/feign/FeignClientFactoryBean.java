package com.caqy.feign;

import feign.*;
import feign.codec.Decoder;
import feign.codec.Encoder;
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
    private Decoder decoder = DefaultDecoder.getInstance();
    private Encoder encoder = DefaultEncoder.getInstance();
    private boolean autoSetCookies = false;
    private static final List<Cookie> COOKIES = new ArrayList<>();

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
                    .options(new Request.Options(1000, 3500))
                    .retryer(new Retryer.Default(5000, 5000, 3));
        feignBuilder.encoder(encoder)
                .decoder(decoder)
                .client(getClient());
        return feignBuilder;
    }

    private Client getClient() {
        okhttp3.OkHttpClient.Builder clientBuilder = new okhttp3.OkHttpClient.Builder();
        if (autoSetCookies) {
            clientBuilder.cookieJar(new CookieJar() {
                @Override
                public void saveFromResponse(HttpUrl httpUrl, List<Cookie> list) {
                    COOKIES.addAll(list);
                }

                @Override
                public List<Cookie> loadForRequest(HttpUrl httpUrl) {
                    List<Cookie> cookies = new ArrayList<>();
                    for (Cookie cookie : COOKIES) {
                        if (cookie.matches(httpUrl)) {
                            cookies.add(cookie);
                        }
                    }
                    return cookies;
                }
            });
        }
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
