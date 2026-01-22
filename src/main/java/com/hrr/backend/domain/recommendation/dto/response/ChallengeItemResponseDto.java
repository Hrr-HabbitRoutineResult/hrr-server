package com.hrr.backend.domain.recommendation.dto.response;

import lombok.*;
import java.time.LocalTime;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChallengeItemResponseDto {

    private long challengeId;
    private String title;
    private String description;
    private String category;
    @JsonProperty("cert_time_slots")
    private String certTimeSlots;
    @JsonProperty("goal_text")
    private String goalText;
    private LocalTime verifyStartTime;
    private LocalTime verifyEndTime;
    @JsonProperty("image_key")
    private String imageKey;
    private Boolean likedByMe;
}
