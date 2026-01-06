package com.hrr.backend.domain.recommendation.dto.request;

import com.hrr.backend.domain.recommendation.dto.response.ChallengeItemDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelApiRequest {

    @NotNull
    private String query;
    @NotEmpty
    private List<ChallengeItemDto> items;
    @Positive
    @JsonProperty("top_k")
    private int topK;

    private String userGender;
    private String userAgeGroup;
    private String userJob;
    private List<String> userAvailableTime;
}
