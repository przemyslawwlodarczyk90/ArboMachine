package com.example.arbomachine.infrastructure.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;

/** Wysylka maili HTML renderowanych z szablonow Thymeleaf (src/main/resources/templates). */
@Service
public class EmailSenderService {
    private static final String REGISTRATION_CONFIRMATION_TEMPLATE = "registration-confirmation-email";
    private static final String PASSWORD_RESET_TEMPLATE = "password-reset-email";

    private final JavaMailSender javaMailSender;
    private final TemplateEngine thymeleafTemplateEngine;

    @Value("${spring.mail.username:}")
    private String senderEmailAddress;

    @Value("${app.mail.sender-display-name:ArboMachine}")
    private String senderDisplayName = "ArboMachine";

    public EmailSenderService(JavaMailSender javaMailSender, TemplateEngine thymeleafTemplateEngine) {
        this.javaMailSender = javaMailSender;
        this.thymeleafTemplateEngine = thymeleafTemplateEngine;
    }

    public void sendRegistrationConfirmationEmail(String recipientEmail, String subject, String firstName,
                                                  String lastName, String confirmationLink) {
        Context templateVariables = new Context();
        templateVariables.setVariable("firstName", firstName);
        templateVariables.setVariable("lastName", lastName);
        templateVariables.setVariable("confirmationLink", confirmationLink);

        sendHtmlEmail(recipientEmail, subject, REGISTRATION_CONFIRMATION_TEMPLATE, templateVariables);
    }

    public void sendPasswordResetEmail(String recipientEmail, String subject, String firstName, String resetLink,
                                       long resetLinkValidityMinutes) {
        Context templateVariables = new Context();
        templateVariables.setVariable("firstName", firstName);
        templateVariables.setVariable("resetLink", resetLink);
        templateVariables.setVariable("resetLinkValidityMinutes", resetLinkValidityMinutes);

        sendHtmlEmail(recipientEmail, subject, PASSWORD_RESET_TEMPLATE, templateVariables);
    }

    private void sendHtmlEmail(String recipientEmail, String subject, String templateName, Context templateVariables) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        String htmlBody = thymeleafTemplateEngine.process(templateName, templateVariables);

        try {
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, "UTF-8");
            // Bez skonfigurowanego konta SMTP (np. w testach) nadawce ustawia sam serwer.
            if (senderEmailAddress != null && !senderEmailAddress.isBlank()) {
                messageHelper.setFrom(senderEmailAddress, senderDisplayName);
            }
            messageHelper.setTo(recipientEmail);
            messageHelper.setSubject(subject);
            messageHelper.setText(htmlBody, true);
            javaMailSender.send(mimeMessage);
        } catch (MessagingException | UnsupportedEncodingException | MailException sendingException) {
            throw new EmailSendingFailedException("Failed to send email", sendingException);
        }
    }
}
