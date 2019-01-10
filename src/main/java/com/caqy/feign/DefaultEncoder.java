package com.caqy.feign;

import feign.RequestTemplate;
import feign.codec.EncodeException;
import feign.codec.Encoder;
import feign.jackson.JacksonEncoder;
import feign.jaxb.JAXBContextFactory;
import feign.jaxb.JAXBEncoder;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.Collection;

class DefaultEncoder implements Encoder {

    private Encoder jacksonEncoder;
    private Encoder jaxbEncoder;
    private Encoder protoEncoder;
    private Encoder defaultEncoder;

    private DefaultEncoder() {
        jacksonEncoder = new JacksonEncoder();
        JAXBContextFactory jaxbFactory = new JAXBContextFactory.Builder()
                .withMarshallerJAXBEncoding("UTF-8")
                .withMarshallerFormattedOutput(Boolean.TRUE)
                .build();
        jaxbEncoder = new JAXBEncoder(jaxbFactory);
        protoEncoder = (object, body, template) -> {
            Class<?> typeClass = (Class) body;
            try {
                Class<?> messageClass = Class.forName("com.google.protobuf.GeneratedMessageV3");
                if (messageClass != null && messageClass.isAssignableFrom(typeClass)) {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    typeClass.getMethod("writeTo", OutputStream.class).invoke(object, outputStream);
                    template.body(outputStream.toByteArray(), null);
                }
                throw new EncodeException(String.format("Fail to encode in protobufEncoder because %s is not a proto class", body.getTypeName()));
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
                throw new EncodeException("Fail to encode in protobufEncoder", e);
            }
        };
        defaultEncoder = new Encoder.Default();
    }

    private static DefaultEncoder instance;

    static DefaultEncoder getInstance() {
        if (instance == null)
            instance = new DefaultEncoder();
        return instance;
    }

    @Override
    public void encode(Object object, Type bodyType, RequestTemplate template) throws EncodeException {
        if (template.headers().containsKey("Content-Type")) {
            Collection<String> contentTypes = template.headers().get("Content-Type");
            if (contentTypes.stream().anyMatch(s -> s.endsWith("/json"))) {
                jacksonEncoder.encode(object, bodyType, template);
                return;
            } else if (contentTypes.stream().anyMatch(s -> s.endsWith("/xml"))) {
                jaxbEncoder.encode(object, bodyType, template);
                return;
            } else if (contentTypes.stream().anyMatch(s -> s.endsWith("/x-protobuf"))) {
                protoEncoder.encode(object, bodyType, template);
                return;
            }
        }
        defaultEncoder.encode(object, bodyType, template);
    }
}
