package com.smartelderly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.smartelderly.config.AppProperties;

@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
@EnableScheduling
@EnableConfigurationProperties(AppProperties.class)
public class SmartElderlyCareApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartElderlyCareApplication.class, args);
    }
}
