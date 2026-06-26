package com.bookfair.backend.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.bookfair.backend.integration.notification.NotificationChannel;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final List<NotificationChannel> channels;

    public void notify(String recipient, String subject, String template, Map<String, Object> vars) {
        // Iterate through all injected channels and trigger send().
        // In the future, this can be filtered by channel.supports("EMAIL") or "SMS"
        // based on user preferences.
        for (NotificationChannel channel : channels) {
            channel.send(recipient, subject, template, vars);
        }
    }
}