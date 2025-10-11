package com.hrr.backend.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.hrr.backend.response.ApiResponse;
import com.hrr.backend.response.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice(annotations = {RestController.class})
public class ExceptionAdvice extends ResponseEntityExceptionHandler {


	@ExceptionHandler
	public ResponseEntity<Object> validation(ConstraintViolationException e, WebRequest request) {
		String message = e.getConstraintViolations().stream()
			.map(constraintViolation -> constraintViolation.getMessage())
			.findFirst()
			.orElse(null);

		return handleExceptionInternalConstraint(e, ErrorCode.valueOf(message), HttpHeaders.EMPTY,request);
	}

	@Override
	public ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

		Map<String, String> errors = new LinkedHashMap<>();

		e.getBindingResult().getFieldErrors().stream()
			.forEach(fieldError -> {
				String fieldName = fieldError.getField();
				String errorMessage = Optional.ofNullable(fieldError.getDefaultMessage()).orElse("");
				errors.merge(fieldName, errorMessage, (existingErrorMessage, newErrorMessage) -> existingErrorMessage + ", " + newErrorMessage);
			});

		return handleExceptionInternalArgs(e,HttpHeaders.EMPTY,ErrorCode.valueOf("_BAD_REQUEST"),request,errors);
	}

	@ExceptionHandler
	public ResponseEntity<Object> exception(Exception e, WebRequest request) {
		log.error("Unhandled exception occurred: ", e);

		return handleExceptionInternalFalse(e, ErrorCode._INTERNAL_SERVER_ERROR, HttpHeaders.EMPTY, ErrorCode._INTERNAL_SERVER_ERROR.getHttpStatus(),request, "서버에 오류가 발생했습니다");
	}

	@ExceptionHandler(value = GlobalException.class)
	public ResponseEntity onThrowException(GlobalException generalException, HttpServletRequest request) {
		ErrorCode errorCode = generalException.getErrorCode();
		return handleExceptionInternal(generalException, errorCode,null,request);
	}

	private ResponseEntity<Object> handleExceptionInternal(Exception e, ErrorCode errorCode,
		HttpHeaders headers, HttpServletRequest request) {

		ApiResponse<Object> body = ApiResponse.onFailure(errorCode,null);

		WebRequest webRequest = new ServletWebRequest(request);
		return super.handleExceptionInternal(
			e,
			body,
			headers,
			errorCode.getHttpStatus(),
			webRequest
		);
	}

	private ResponseEntity<Object> handleExceptionInternalFalse(Exception e, ErrorCode errorCommonStatus,
		HttpHeaders headers, HttpStatus status, WebRequest request, String errorPoint) {
		ApiResponse<Object> body = ApiResponse.onFailure(errorCommonStatus,errorPoint);
		return super.handleExceptionInternal(
			e,
			body,
			headers,
			status,
			request
		);
	}

	private ResponseEntity<Object> handleExceptionInternalArgs(Exception e, HttpHeaders headers, ErrorCode errorCommonStatus,
		WebRequest request, Map<String, String> errorArgs) {
		ApiResponse<Object> body = ApiResponse.onFailure(errorCommonStatus, errorArgs);
		return super.handleExceptionInternal(
			e,
			body,
			headers,
			errorCommonStatus.getHttpStatus(),
			request
		);
	}

	private ResponseEntity<Object> handleExceptionInternalConstraint(Exception e, ErrorCode errorCommonStatus,
		HttpHeaders headers, WebRequest request) {
		ApiResponse<Void> body = ApiResponse.onFailure(errorCommonStatus, null);
		return super.handleExceptionInternal(
			e,
			body,
			headers,
			errorCommonStatus.getHttpStatus(),
			request
		);
	}
}
