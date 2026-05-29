package com.ft.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class FintrackingAccountApplication {

    public static void main(String[] args) {
        SpringApplication.run(FintrackingAccountApplication.class, args);
    }

}
