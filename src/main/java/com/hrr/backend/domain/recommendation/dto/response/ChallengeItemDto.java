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
    private String imageKey;
    private List<Float> embedding;

    private Integer participants_male_pct;
    private Integer participants_female_pct;

    private Integer age_10s_pct;
    private Integer age_20s_pct;
    private Integer age_30s_pct;
    private Integer age_40s_pct;
    private Integer age_50p_pct;

    private Integer job_student_middle_high_pct;
    private Integer job_student_university_pct;
    private Integer job_job_seeker_pct;
    private Integer job_employee_pct;
    private Integer job_homemaker_pct;
    private Integer job_etc_pct;
}
