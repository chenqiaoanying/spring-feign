package com.caqy.feign;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AutoCookieJar implements CookieJar {

    private static final AutoCookieJar instance = new AutoCookieJar();

    private AutoCookieJar() {
    }

    public static AutoCookieJar getInstance() {
        return instance;
    }

    private final List<Cookie> cookies = new ArrayList<>();

    @Override
    public void saveFromResponse(HttpUrl httpUrl, List<Cookie> list) {
        for (Cookie cookie : list) {
            long currentTime = System.currentTimeMillis();
            cookies.removeIf(existedCookie -> existedCookie.expiresAt() < currentTime
                    || cookie.hostOnly() == existedCookie.hostOnly()
                    && cookie.domain().equals(existedCookie.domain())
                    && cookie.path().equals(existedCookie.path())
                    && cookie.name().equals(existedCookie.name()));
            if (cookie.expiresAt() > currentTime)
                cookies.add(cookie);
        }
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl httpUrl) {
        return cookies.stream().filter(cookie -> cookie.matches(httpUrl)).collect(Collectors.toList());
    }
}
