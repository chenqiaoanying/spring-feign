package com.caqy.feign;

import feign.FeignException;
import feign.Response;
import feign.Util;
import feign.codec.DecodeException;
import feign.codec.Decoder;
import feign.jackson.JacksonDecoder;
import feign.jaxb.JAXBContextFactory;
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
import java.util.Collection;

class DefaultDecoder implements Decoder {

    private Decoder jacksonDecoder;
    private Decoder jaxbDecoder;
    private Decoder protobufDecoder;
    private Decoder defaultDecoder;

    private DefaultDecoder() {
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
        defaultDecoder = new Decoder.Default();
    }

    private static DefaultDecoder instance;

    static DefaultDecoder getInstance() {
        if (instance == null)
            instance = new DefaultDecoder();
        return instance;
    }

    @Override
    public Object decode(Response response, Type type) throws IOException, FeignException {
        if (response.headers().containsKey("Content-Type")) {
            Collection<String> contentTypes = response.headers().get("Content-Type");
            if (contentTypes.stream().anyMatch(s -> s.contains("/json"))) {
                return jacksonDecoder.decode(response, type);
            } else if (contentTypes.stream().anyMatch(s -> s.contains("/xml"))) {
                return jaxbDecoder.decode(response, type);
            } else if (contentTypes.stream().anyMatch(s -> s.contains("/x-protobuf"))) {
                return protobufDecoder.decode(response, type);
            }
        }
        return defaultDecoder.decode(response, type);
    }
}
