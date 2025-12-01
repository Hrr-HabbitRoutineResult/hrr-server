package com.hrr.backend.domain.round.repository;

import com.hrr.backend.domain.round.entity.RoundRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoundRecordRepository extends JpaRepository<RoundRecord, Long> {

}