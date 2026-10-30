package com.scv.global.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.scv.domain.version.dto.layer.LayerDTO;
import com.scv.domain.version.dto.layer.convolution.Conv2dDTO;
import com.scv.global.error.ErrorCode;
import com.scv.global.exception.ScvExceptionHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class JsonCodecTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final JsonCodec codec = new JsonCodec(mapper);
    private static final TypeReference<List<LayerDTO>> LAYERS = new TypeReference<>() {};

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "null", "[", "[] {}", "[] trailing", "{}", "[null trailing]"})
    void rejectsInvalidStoredLayerDocuments(String json) {
        assertThatThrownBy(() -> codec.read(json, LAYERS)).isInstanceOfSatisfying(JsonCodecException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.JSON_PROCESSING_ERROR));
    }

    @Test
    void emptyLayerArrayIsValid() {
        assertThat(codec.read("[]", LAYERS)).isEmpty();
    }

    @Test
    void preservesPolymorphicLayersAndIgnoresUnknownFields() {
        var layers = codec.read("""
                [{"name":"Conv2d","in_channels":3,"out_channels":16,"kernel_size":3,"futureField":true}]
                """, LAYERS);
        assertThat(layers.getFirst()).isInstanceOfSatisfying(Conv2dDTO.class, layer -> {
            assertThat(layer.getInChannels()).isEqualTo(3);
            assertThat(layer.getOutChannels()).isEqualTo(16);
            assertThat(layer.getKernelSize()).isEqualTo(3);
        });
        var restored = codec.read(codec.write(layers), LAYERS);
        assertThat(restored).usingRecursiveComparison().isEqualTo(layers);
    }

    @Test
    void rejectsUnknownLayerTypes() {
        assertThatThrownBy(() -> codec.read("[{\"name\":\"UnknownLayer\"}]", LAYERS))
                .isInstanceOf(JsonCodecException.class);
    }

    @Test
    void preservesNestedGenericTypes() {
        Map<String, List<Integer>> value = codec.read("{\"params\":[1,2]}", new TypeReference<>() {});
        assertThat(value.get("params")).containsExactly(1, 2);
    }

    @Test
    void preservesEscapedCodeAndImageStrings() throws Exception {
        String code = "print(\"안녕\")\n# C:\\models";
        assertThat(mapper.readTree(codec.write(code)).asText()).isEqualTo(code);
        assertThat(mapper.readTree(codec.write("data:image/png;base64,abc==")).asText())
                .isEqualTo("data:image/png;base64,abc==");
        assertThat(codec.write(null)).isEqualTo("null");
    }

    @Test
    void inheritsRegisteredModulesWithoutMutatingSharedMapper() {
        mapper.registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        JsonCodec configured = new JsonCodec(mapper);
        LocalDate date = LocalDate.of(2026, 10, 31);
        assertThat(configured.read(configured.write(date), new TypeReference<LocalDate>() {})).isEqualTo(date);
        assertThat(mapper.isEnabled(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)).isFalse();
        assertThat(mapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)).isTrue();
        assertThat(mapper.isEnabled(SerializationFeature.INDENT_OUTPUT)).isFalse();
    }

    @Test
    void wrapsSerializationFailureAndKeepsItsCause() {
        assertThatThrownBy(() -> codec.write(new BrokenValue())).isInstanceOfSatisfying(JsonCodecException.class,
                error -> {
                    assertThat(error.getCause()).isNotNull();
                    assertThat(error.getMessage()).doesNotContain("private payload");
                });
    }

    @Test
    void storedJsonErrorsProduceServerErrorWithoutExposingInput() throws Exception {
        var exception = catchThrowableOfType(() -> codec.read("private payload", LAYERS), JsonCodecException.class);
        var response = new ScvExceptionHandler().exceptionHandle(exception);
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(mapper.writeValueAsString(response.getBody())).contains("JSON_PROCESSING_ERROR")
                .doesNotContain("private payload");
    }

    public static class BrokenValue {
        public String getValue() { throw new IllegalStateException("private payload"); }
    }
}
