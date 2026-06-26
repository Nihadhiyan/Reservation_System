package com.bookfair.backend.service;

import java.util.Base64;
import java.util.Map;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import static java.util.Objects.requireNonNull;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Async
    public void sendEmail(String to, String subject, String templateName, Map<String, Object> variables,
            String qrBase64) {
        requireNonNull(to, "to cannot be null");
        requireNonNull(templateName, "templateName cannot be null");
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);

            boolean hasQrCode = (qrBase64 != null && !qrBase64.isEmpty());
            variables.put("hasQrCode", hasQrCode);

            Context thymeleafContext = new Context();
            thymeleafContext.setVariables(variables);

            String htmlBody = templateEngine.process("email/" + templateName, thymeleafContext);

            helper.setText(htmlBody, true);

            if (hasQrCode) {
                byte[] decodedImg = Base64.getDecoder().decode(qrBase64);
                helper.addInline("qrCode", new ByteArrayResource(decodedImg), "image/png");
            }

            mailSender.send(message);
            log.info("Email successfully sent to {}", to);

        } catch (Exception e) {

            log.error("CRITICAL: Failed to send email to {}. Reason: {}", to, e.getMessage(), e);
        }
    }
}
