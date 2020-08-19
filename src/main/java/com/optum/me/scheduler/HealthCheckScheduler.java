package com.optum.me.scheduler;

import com.optum.c360.security.AESCryptoException;
import com.optum.me.service.HealthCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;


@Component
@EnableAutoConfiguration
public class HealthCheckScheduler {

    Logger LOGGER = LoggerFactory.getLogger(HealthCheckScheduler.class);

    @Autowired
    HealthCheckService healthCheckService;

    @Scheduled(fixedRate = 15000)
    //@Scheduled(cron = "${scheduler.threshhold.timeInterval}")
    public void reportCurrentTime() throws UnsupportedEncodingException, InterruptedException, AESCryptoException {

        healthCheckService.notifyEvent();

        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

        LOGGER.info("The time is now {}", dateFormat.format(new Date()));
    }
}
