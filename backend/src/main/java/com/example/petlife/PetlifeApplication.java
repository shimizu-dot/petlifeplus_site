package com.example.petlife;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class PetlifeApplication {

	public static void main(String[] args) {
		SpringApplication.run(PetlifeApplication.class, args);
	}

}
