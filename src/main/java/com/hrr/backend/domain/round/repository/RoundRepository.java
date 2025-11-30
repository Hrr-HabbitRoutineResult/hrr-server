package com.hrr.backend.domain.round.repository;

import com.hrr.backend.domain.round.entity.Round;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoundRepository extends JpaRepository<Round, Long> {

}