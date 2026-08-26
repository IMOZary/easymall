package com.easymall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EasyMallApplication {
    public static void main(String[] args) {
        SpringApplication.run(EasyMallApplication.class, args);
    }
}
