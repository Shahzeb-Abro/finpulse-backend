package com.finpulse.controller;

import com.finpulse.util.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

//@Profile(value = "local")
@RestController
@RequestMapping("/v1/test")
@RequiredArgsConstructor
public class TestController {
    private final EmailService emailService;

    @GetMapping("/send-welcome-email")
    public void sendWelcomeEmail() {
        emailService.sendTemplatedEmail(
                "shahzebaliabro12345@gmail.com",
                "WELCOME",
                Map.of("name", "Shahzeb")
        );
    }
}
