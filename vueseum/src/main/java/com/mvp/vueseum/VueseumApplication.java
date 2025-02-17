package com.mvp.vueseum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication(scanBasePackages = "com.mvp.vueseum")
@EnableScheduling
@EnableMethodSecurity
public class VueseumApplication {
	public static void main(String[] args) {
		SpringApplication.run(VueseumApplication.class, args);
	}
}
