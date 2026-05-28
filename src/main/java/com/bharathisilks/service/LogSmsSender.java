package com.bharathisilks.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Default no-gateway SMS sender: writes the message to the log. Replace with a
 * provider-backed implementation in production (e.g. annotate the provider bean
 * {@code @Primary} or guard this one with {@code @ConditionalOnMissingBean}).
 */
@Service
public class LogSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(LogSmsSender.class);

    @Override
    public void send(String phone, String message) {
        log.info("[SMS -> {}] {}", phone, message);
    }
}
