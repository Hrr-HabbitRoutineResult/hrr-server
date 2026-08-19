package com.hrr.backend.global.exception;

import com.hrr.backend.global.response.ErrorCode;

import lombok.Getter;

@Getter
public class GlobalException extends RuntimeException {

	private final ErrorCode errorCode;

	public GlobalException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	public GlobalException(ErrorCode errorCode, Throwable cause) {
		super(errorCode.getMessage(), cause);
		this.errorCode = errorCode;
	}
}
