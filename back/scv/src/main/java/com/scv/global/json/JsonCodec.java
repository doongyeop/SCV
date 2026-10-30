package com.scv.global.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Component;

/** JSON stored by the model domain. HTTP response mapping uses Spring's own mapper. */
@Component
public class JsonCodec {
    private final ObjectMapper mapper;

    public JsonCodec(ObjectMapper objectMapper) {
        // Retain registered modules without changing the application's HTTP mapper.
        this.mapper = objectMapper.copy()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    public <T> T read(String json, TypeReference<T> type) {
        if (json == null || json.isBlank()) {
            throw new JsonCodecException(null);
        }
        try {
            T value = mapper.readValue(json, type);
            if (value == null) throw new JsonCodecException(null);
            return value;
        } catch (JsonProcessingException e) {
            throw new JsonCodecException(e);
        }
    }

    public String write(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new JsonCodecException(e);
        }
    }
}
