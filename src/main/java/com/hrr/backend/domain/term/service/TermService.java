package com.hrr.backend.domain.term.service;

import com.hrr.backend.domain.term.dto.TermResponseDto;

import java.util.List;

public interface TermService {

    List<TermResponseDto.TermSummary> getTermList();

    TermResponseDto.TermDetail getTermDetail(Long termId);
}
