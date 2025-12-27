package com.hrr.backend.domain.recommendation.dto.request;

import com.hrr.backend.global.common.enums.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChallengeRecommendRequest {

    private Long userId;
    private Gender gender;
    private AgeGroup ageGroup;
    private Job job;
    private List<AvailableTime> availableTime;
    private List<Category> category;
    private Goal goal;
}

