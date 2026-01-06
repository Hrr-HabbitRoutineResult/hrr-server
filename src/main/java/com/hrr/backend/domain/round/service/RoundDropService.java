package com.hrr.backend.domain.round.service;

import java.time.LocalDate;

public interface RoundDropService {
    void dropNonContinuersAt(LocalDate endDate);
}
