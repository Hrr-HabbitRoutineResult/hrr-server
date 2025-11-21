package com.hrr.backend.domain.term.service;

import com.hrr.backend.domain.term.dto.TermResponseDto;
import com.hrr.backend.domain.term.entity.Term;
import com.hrr.backend.domain.term.repository.TermRepository;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TermServiceImpl implements TermService {

    private final TermRepository termRepository;

    @Override
    public List<TermResponseDto.TermSummary> getTermList() {
        return termRepository.findAllByOrderByIdAsc()
                .stream()
                .map(term -> new TermResponseDto.TermSummary(
                        term.getId(),
                        term.getTitle(),
                        term.getIsRequired()
                ))
                .toList();
    }

    @Override
    public TermResponseDto.TermDetail getTermDetail(Long termId) {
        Term term = termRepository.findById(termId)
                .orElseThrow(() -> new GlobalException(ErrorCode._BAD_REQUEST));

        return new TermResponseDto.TermDetail(
                term.getId(),
                term.getTitle(),
                term.getDescription(),
                term.getIsRequired()
        );
    }
}
