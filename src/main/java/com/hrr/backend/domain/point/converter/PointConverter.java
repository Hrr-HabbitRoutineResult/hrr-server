package com.hrr.backend.domain.point.converter;

import org.springframework.stereotype.Component;

import com.hrr.backend.domain.point.dto.PointHistoryResponseDto;
import com.hrr.backend.domain.point.entity.PointHistory;

@Component
public class PointConverter {

    public PointHistoryResponseDto.HistoryDto toHistoryDto(PointHistory history) {
        return PointHistoryResponseDto.HistoryDto.builder()
                .pointType(history.getPointType())
                .title(history.getPointType().getDescription())
                .detail(resolveDetail(history))
                .points(history.getPoints())
                .createdAt(history.getCreatedAt())
                .build();
    }

    private String resolveDetail(PointHistory history) {
        if (history.getRandomMission() != null) {
            return history.getRandomMission().getContent();
        }
        if (history.getChallenge() != null) {
            return history.getChallenge().getTitle();
        }
        return null;
    }
}