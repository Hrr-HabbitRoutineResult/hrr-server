package com.hrr.backend.domain.report.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ReportRequestDtoTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void otherReason_requiresDescription() throws Exception {
        ReportRequestDto request = request("OTHER", "   ");

        Set<ConstraintViolation<ReportRequestDto>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("기타 신고 사유를 입력해 주세요.");
    }

    @Test
    void description_cannotExceed200Characters() throws Exception {
        ReportRequestDto request = request("OTHER", "가".repeat(201));

        Set<ConstraintViolation<ReportRequestDto>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("상세 신고 사유는 최대 200자까지 입력 가능합니다.");
    }

    @Test
    void otherReason_withDescription_isValid() throws Exception {
        ReportRequestDto request = request("OTHER", "상세 신고 사유");

        assertThat(validator.validate(request)).isEmpty();
    }

    private ReportRequestDto request(String reason, String description) throws Exception {
        String json = """
                {
                  "targetId": 1,
                  "reason": "%s",
                  "description": "%s"
                }
                """.formatted(reason, description);
        return OBJECT_MAPPER.readValue(json, ReportRequestDto.class);
    }
}
