package com.maillite.mailliteapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MailliteapiApplication {

	public static void main(String[] args) {
		SpringApplication.run(MailliteapiApplication.class, args);
	}

}
