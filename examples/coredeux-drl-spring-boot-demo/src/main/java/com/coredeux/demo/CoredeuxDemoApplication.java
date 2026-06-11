package com.coredeux.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.coredeux")
@EnableScheduling
public class CoredeuxDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoredeuxDemoApplication.class, args);
    }
}
