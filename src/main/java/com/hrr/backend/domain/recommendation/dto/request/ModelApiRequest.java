package com.hrr.backend.domain.recommendation.dto.request;

import com.hrr.backend.domain.recommendation.dto.response.ChallengeItemDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelApiRequest {

    private String query;
    private List<ChallengeItemDto> items;
    private int top_k;
}
