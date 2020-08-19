package com.optum.me;

import com.optum.c360.configuration.email.EmailConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAutoConfiguration
@ComponentScan({"com.optum.me"})
@EnableConfigurationProperties
@Import(EmailConfiguration.class)
public class HealthCheckApplication {

    Logger LOGGER = LoggerFactory.getLogger(HealthCheckApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(HealthCheckApplication.class, args);
    }
}
