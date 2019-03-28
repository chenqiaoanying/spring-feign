package com.caqy.feign.decoder;

import com.caqy.feign.Utils;
import feign.FeignException;
import feign.Response;
import feign.Util;
import feign.codec.DecodeException;
import feign.codec.Decoder;
import feign.jackson.JacksonDecoder;
import feign.jaxb.JAXBContextFactory;
import okhttp3.MediaType;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

public class AutoDetectDecoder implements Decoder {

    private Decoder jacksonDecoder;
    private Decoder jaxbDecoder;
    private Decoder protobufDecoder;
    private Decoder htmlDecoder;
    private Decoder defaultDecoder;

    private AutoDetectDecoder() {
        jacksonDecoder = new JacksonDecoder();
        JAXBContextFactory jaxbFactory = new JAXBContextFactory.Builder()
                .withMarshallerJAXBEncoding("UTF-8")
                .withMarshallerFormattedOutput(Boolean.TRUE)
                .build();
        jaxbDecoder = (response, type) -> {
            if (response.status() == 404) return Util.emptyValueOf(type);
            if (response.body() == null) return null;
            if (!(type instanceof Class)) {
                throw new UnsupportedOperationException("JAXB only supports decoding raw types. Found " + type);
            }

            try {
                SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();

                saxParserFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                saxParserFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                saxParserFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
                saxParserFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
                saxParserFactory.setFeature("http://xml.org/sax/features/namespaces", true);

                Source source = new SAXSource(saxParserFactory.newSAXParser().getXMLReader(), new InputSource(response.body().asInputStream()));
                Unmarshaller unmarshaller = jaxbFactory.createUnmarshaller((Class) type);
                return unmarshaller.unmarshal(source);
            } catch (JAXBException | ParserConfigurationException | SAXException e) {
                throw new DecodeException(e.toString(), e);
            } finally {
                if (response.body() != null) {
                    response.body().close();
                }
            }
        };
        protobufDecoder = (response, type) -> {
            Class<?> typeClass = (Class) type;
            try {
                Class<?> messageClass = Class.forName("com.google.protobuf.GeneratedMessageV3");
                if (messageClass != null && messageClass.isAssignableFrom(typeClass))
                    return typeClass.getMethod("parseFrom", InputStream.class).invoke(null, response.body().asInputStream());
                throw new DecodeException(String.format("Fail to decode in protobufDecoder because %s is not a proto class", type.getTypeName()));
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
                throw new DecodeException("Fail to decode in protobufDecoder", e);
            }
        };
        htmlDecoder = new HtmlDecoder();
        defaultDecoder = new Decoder.Default();
    }

    private static AutoDetectDecoder instance;

    public static AutoDetectDecoder getInstance() {
        if (instance == null)
            instance = new AutoDetectDecoder();
        return instance;
    }

    @Override
    public Object decode(Response response, Type type) throws IOException, FeignException {
        MediaType mediaType = Utils.getMediaTypeFromHeaders(response.headers());
        if (mediaType == null) {
            return defaultDecoder.decode(response, type);
        } else {
            switch (mediaType.subtype()) {
                case "json":
                    return jacksonDecoder.decode(response, type);
                case "xml":
                    return jaxbDecoder.decode(response, type);
                case "x-protobuf":
                    return protobufDecoder.decode(response, type);
                case "html":
                    return htmlDecoder.decode(response, type);
                default:
                    return defaultDecoder.decode(response, type);
            }
        }
    }
}
