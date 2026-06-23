package com.bookfair.backend.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailNotificationChannel implements NotificationChannel {

    private final EmailService emailService;

    @Override
    public void send(String recipient, String subject, String template, Map<String, Object> variables) {
        String qrCodeBase64 = null;
        if (variables != null && variables.containsKey("qrCodeBase64")) {
            qrCodeBase64 = variables.get("qrCodeBase64");
        }
        emailService.sendEmail(recipient, subject, template, variables, qrCodeBase64);
    }

    @Override
    public boolean supports(String channelType) {
        return "EMAIL".equalsIgnoreCase(channelType);
    }
}
