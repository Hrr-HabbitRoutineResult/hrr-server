package com.hrr.backend.test;

import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;

public class TestHandler extends GlobalException {

	public TestHandler(ErrorCode errorCode) {
		super(errorCode);
	}
}
