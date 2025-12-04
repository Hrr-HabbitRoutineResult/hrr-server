package com.hrr.backend.domain.recommendation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ChallengeItemDto {

    private long challengeId;
    private String title;
    private String description;
    private String category;
    private String cert_time_slots;
    private String goal_text;
    private LocalTime verifyStartTime;
    private LocalTime verifyEndTime;
    private List<Float> embedding;
}
