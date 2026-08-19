package com.hrr.backend.global.common.converter;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

class StringListConverterTest {

    private final StringListConverter converter = new StringListConverter();

    @Test
    void convertToEntityAttribute_preservesJsonParsingFailureAsCause() {
        assertThatThrownBy(() -> converter.convertToEntityAttribute("[invalid-json"))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("Failed to convert JSON string to List<String>")
                .hasCauseInstanceOf(JsonProcessingException.class);
    }
}
