package com.hrr.backend.test;

import com.hrr.backend.exception.GlobalException;
import com.hrr.backend.response.ErrorCode;

public class TestHandler extends GlobalException {

	public TestHandler(ErrorCode errorCode) {
		super(errorCode);
	}
}
