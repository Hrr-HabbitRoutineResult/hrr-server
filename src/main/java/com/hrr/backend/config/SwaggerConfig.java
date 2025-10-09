package com.hrr.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

@Profile("!test")
@Configuration
public class SwaggerConfig {

	@Value("${springdoc.server-url}")
	private String serverUrl;

	// 공통 OpenAPI - 관리자와 사용자 모두 볼 수 있음
	@Bean
	public OpenAPI openAPI() {
		Info info = new Info()
			.title("Hrr API")
			.description("Hrr 백엔드 API 입니다.")
			.version("1.0");


		return new OpenAPI()
			.info(info)
			.servers(List.of(new Server().url(serverUrl)));
	}

}

