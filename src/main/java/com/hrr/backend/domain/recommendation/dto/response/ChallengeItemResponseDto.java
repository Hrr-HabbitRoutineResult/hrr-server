package com.hrr.backend.domain.recommendation.dto.response;

import lombok.*;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChallengeItemResponseDto {

    private long challengeId;
    private String title;
    private String description;
    private String category;
    private String cert_time_slots;
    private String goal_text;
    private LocalTime verifyStartTime;
    private LocalTime verifyEndTime;
}
