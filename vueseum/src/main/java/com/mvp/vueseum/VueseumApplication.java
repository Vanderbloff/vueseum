package com.mvp.vueseum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.mvp.vueseum")
@EnableScheduling
public class VueseumApplication {
	public static void main(String[] args) {
		SpringApplication.run(VueseumApplication.class, args);
	}
}
