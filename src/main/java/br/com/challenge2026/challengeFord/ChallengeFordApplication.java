package br.com.challenge2026.challengeFord;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ChallengeFordApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChallengeFordApplication.class, args);
	}

}
