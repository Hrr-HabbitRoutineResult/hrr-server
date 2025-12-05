package com.hrr.backend.domain.user.service;

import com.hrr.backend.domain.user.dto.UserTermRequestDto;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserTerm;
import com.hrr.backend.domain.user.repository.UserRepository;
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
	private final UserRepository userRepository;

	@Transactional
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

        // 3. 약관 저장 (멱등성) ; 전체 조회해서 철회도 가능하게 수정
		List<Term> allTerms = termRepository.findAll();

		for (Term term : allTerms) {

			boolean isAgreed = agreedIds.contains(term.getId());

			userTermRepository.findByUserAndTerm(user, term)
				.ifPresentOrElse(
					// update
					ut -> ut.updateIsAgreed(isAgreed),

					// insert
					() -> {
						if (isAgreed) { // 동의했을 때만 INSERT 시도 (DB에 철회 기록을 남길 필요는 없음)
							userTermRepository.save(
								UserTerm.builder()
									.user(user)
									.term(term)
									.isAgreed(true)
									.build()
							);
						}
					}
				);
		}

        // 4. 유저 상태 업데이트
        user.updateLoginStatus(com.hrr.backend.domain.user.entity.enums.LoginStatus.TERMS_DONE);

		// DB 반영
		userRepository.save(user);

    }
}
