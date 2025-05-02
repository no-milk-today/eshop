package com.yandex.reactive.testcontainers.reshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class MainReactive {

	public static void main(String[] args) {
		SpringApplication.run(MainReactive.class, args);
	}

}
