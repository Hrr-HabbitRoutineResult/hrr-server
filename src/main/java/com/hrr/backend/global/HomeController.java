package com.hrr.backend.global;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;

@RestController
public class HomeController {
	// 도메인 주소 접속 처리용

	@Hidden
    @GetMapping("/")
    public String home() {
        return "Welcome to Hrr-official!";
    }
}

