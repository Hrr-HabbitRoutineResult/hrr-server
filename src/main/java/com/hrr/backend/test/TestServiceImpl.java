package com.hrr.backend.test;

import org.springframework.stereotype.Service;

import com.hrr.backend.response.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TestServiceImpl implements TestService {

	@Override
	public void CheckFlag(int flag) {
		if(flag!=1){
			throw new TestHandler(ErrorCode.TEST_ERROR);
		}
	}
}
