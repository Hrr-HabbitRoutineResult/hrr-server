package com.hrr.backend.global.response;

import org.springframework.http.HttpStatus;

public interface BaseCode {
	HttpStatus getHttpStatus();
	String getCode();
	String getMessage();
}
