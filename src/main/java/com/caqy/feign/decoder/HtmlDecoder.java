package com.caqy.feign.decoder;

import feign.FeignException;
import feign.Response;
import feign.Util;
import feign.codec.DecodeException;
import feign.codec.Decoder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.lang.reflect.Type;

import static java.lang.String.format;

public class HtmlDecoder implements Decoder {
    @Override
    public Object decode(Response response, Type type) throws IOException, FeignException {
        if (Document.class.isAssignableFrom((Class<?>) type))
            return Jsoup.parse(Util.toString(response.body().asReader()), response.request().url());
        throw new DecodeException(format("%s is not a type supported by this decoder.", type));
    }
}
