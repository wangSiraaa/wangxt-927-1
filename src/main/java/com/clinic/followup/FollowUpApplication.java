package com.clinic.followup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FollowUpApplication {
    public static void main(String[] args) {
        SpringApplication.run(FollowUpApplication.class, args);
    }
}
