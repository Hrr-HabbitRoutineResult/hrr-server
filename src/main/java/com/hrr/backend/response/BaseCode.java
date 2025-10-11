package com.hrr.backend.response;

import org.springframework.http.HttpStatus;

public interface BaseCode {
	HttpStatus getHttpStatus();
	String getCode();
	String getMessage();
}
