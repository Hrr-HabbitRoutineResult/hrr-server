package com.hrr.backend.domain.recommendation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelApiResponse {

    private List<ModelRecommendItem> recommendations;
    private String modelVersion;
    private int latencyMs;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ModelRecommendItem {
        private long challengeId;
        private String title;
        private String description;
        private double matchScore;
    }
}

