package com.hrr.backend.domain.round.service;

import java.time.LocalDate;

public interface RoundLifecycleService {
    void processRoundsEndedAt(LocalDate endDate);
}
