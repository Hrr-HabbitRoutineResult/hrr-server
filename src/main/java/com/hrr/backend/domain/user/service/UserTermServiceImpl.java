package com.hrr.backend.domain.user.service;

import com.hrr.backend.domain.user.dto.UserTermRequestDto;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserTerm;
import com.hrr.backend.domain.user.repository.UserTermRepository;
import com.hrr.backend.domain.term.entity.Term;
import com.hrr.backend.domain.term.repository.TermRepository;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserTermServiceImpl implements UserTermService {

    private final UserTermRepository userTermRepository;
    private final TermRepository termRepository;

    @Override
    public void saveUserTerms(User user, UserTermRequestDto.AgreeRequest request) {

        List<Long> agreedIds = request.agreedTermIds();

        // 1. 모든 term ID 유효성 검증
        List<Term> terms = termRepository.findAllById(agreedIds);
        if (terms.size() != agreedIds.size()) {
            throw new GlobalException(ErrorCode.INVALID_TERM_ID_IN_REQUEST);
        }

        // 2. 필수 약관 동의했는지 확인
        List<Term> requiredTerms = termRepository.findAll()
                .stream()
                .filter(Term::getIsRequired)
                .toList();

        boolean requiredAllAgreed = requiredTerms.stream()
                .allMatch(term -> agreedIds.contains(term.getId()));

        if (!requiredAllAgreed) {
            throw new GlobalException(ErrorCode.REQUIRED_TERM_NOT_AGREED);
        }

        // 3. 약관 저장 (멱등성)
        for (Term term : terms) {
            userTermRepository.findByUserAndTerm(user, term)
                    .ifPresentOrElse(
                            ut -> ut.updateIsAgreed(true),   // update
                            () -> userTermRepository.save(
                                    UserTerm.builder()
                                            .user(user)
                                            .term(term)
                                            .isAgreed(true)
                                            .build()
                            )
                    );
        }

        // 4. 유저 상태 업데이트
        user.updateLoginStatus(com.hrr.backend.domain.user.entity.enums.LoginStatus.TERMS_DONE);
    }
}
