package com.hrr.backend.domain.round.service;

public interface RoundRecordService {

	// 경고 횟수의 데이터 정합성을 보장하기 위한 계산 확인 메서드
	void synchronizeWarnCount(Long roundRecordId);

}
