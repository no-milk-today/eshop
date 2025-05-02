package com.yandex.reactive.testcontainers.payment;

import org.springframework.boot.SpringApplication;

public class TestPaymentApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(PaymentApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
