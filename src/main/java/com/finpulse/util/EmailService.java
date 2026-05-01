package com.finpulse.util;

import com.finpulse.entity.EmailTemplate;
import com.finpulse.repository.EmailTemplateRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;
    private final EmailTemplateRepository emailTemplateRepository;

    @Qualifier("stringTemplateEngine")
    private final SpringTemplateEngine templateEngine;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Async
    public void sendTemplatedEmail(String to, String templateCode, Map<String, Object> variables) {
        EmailTemplate template = emailTemplateRepository.findByCodeAndActiveFlagTrue(templateCode)
                .orElseThrow(() -> new IllegalArgumentException("Email template not found: " + templateCode));

        Context context = new Context();
        context.setVariables(variables);

        String subject = templateEngine.process(template.getSubject(), context);
        String body = templateEngine.process(template.getBody(), context);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            messageHelper.setFrom(fromAddress);
            messageHelper.setTo(to);
            messageHelper.setSubject(subject);
            messageHelper.setText(body, true);
            mailSender.send(mimeMessage);

            log.info("Email sent to {} with template {}", to, templateCode);
        } catch (MessagingException | MailException e) {
            log.error("Error sending email: {}", e.getMessage());
        }
    }
}
