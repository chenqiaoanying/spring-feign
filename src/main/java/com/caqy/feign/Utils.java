package com.caqy.feign;

import okhttp3.MediaType;

import java.util.Collection;
import java.util.Map;

class Utils {
    static MediaType getMediaTypeFromHeaders(Map<String, Collection<String>> headers) {
        return headers.entrySet().stream()
                .filter(entry -> entry.getKey().toLowerCase().equals("content-type"))
                .findFirst()
                .map(Map.Entry::getValue)
                .map(Collection::iterator)
                .map(iterator -> iterator.hasNext() ? iterator.next() : null)
                .map(MediaType::parse)
                .orElse(null);
    }
}
