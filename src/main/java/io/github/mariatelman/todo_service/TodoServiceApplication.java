package io.github.mariatelman.todo_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@SpringBootApplication
public class TodoServiceApplication {

	public static void main(final String[] args) {
		SpringApplication.run(TodoServiceApplication.class, args);
	}

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }

}
