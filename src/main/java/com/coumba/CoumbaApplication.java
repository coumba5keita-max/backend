package com.coumba;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CoumbaApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoumbaApplication.class, args);
    }

}