package com.bookfair.backend.listener;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.bookfair.backend.event.audit.SecurityAuditEvent;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AuditListener {

    @Async
    @EventListener
    public void onSecurityAudit(SecurityAuditEvent event) {
        log.info("SECURITY AUDIT | Action: {} | PerformedBy: {} | Timestamp: {} | Details: {}",
                event.action(),
                event.performedBy(),
                event.timestamp(),
                event.details());
    }
}
