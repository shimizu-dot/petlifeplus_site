package com.example.petlife;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@ConfigurationPropertiesScan("com.example.petlife.config")
public class PetlifeApplication {

	public static void main(String[] args) {
		SpringApplication.run(PetlifeApplication.class, args);
	}

}
