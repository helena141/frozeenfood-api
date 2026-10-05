package com.lezzat.b2b2c.frozenfood;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
@EnableScheduling
@SpringBootApplication
public class FrozenFoodApplication {

    public static void main(String[] args) {
        SpringApplication.run(FrozenFoodApplication.class, args);
    }
}
