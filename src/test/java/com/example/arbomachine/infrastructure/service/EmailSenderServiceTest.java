package com.example.arbomachine.infrastructure.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EmailSenderServiceTest {

    private static final String RENDERED_HTML_BODY = "<html><body>Test</body></html>";

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private TemplateEngine thymeleafTemplateEngine;

    @InjectMocks
    private EmailSenderService emailSenderService;

    @Captor
    private ArgumentCaptor<MimeMessage> sentMessageCaptor;

    @BeforeEach
    void initMocks() {
        MockitoAnnotations.openMocks(this);
        // Prawdziwy MimeMessage bez sesji SMTP - wystarczy, zeby odczytac adresata, temat i tresc.
        when(javaMailSender.createMimeMessage()).thenReturn(new MimeMessage((jakarta.mail.Session) null));
        when(thymeleafTemplateEngine.process(anyString(), any(Context.class))).thenReturn(RENDERED_HTML_BODY);
    }

    @Test
    void registrationConfirmationEmailUsesItsTemplateAndReachesRecipient() throws MessagingException, IOException {
        emailSenderService.sendRegistrationConfirmationEmail("test@example.com", "Subject", "Jan", "Kowalski",
                "http://example.com/confirm");

        verify(thymeleafTemplateEngine).process(eq("registration-confirmation-email"), any(Context.class));
        verify(javaMailSender).send(sentMessageCaptor.capture());
        MimeMessage sentMessage = sentMessageCaptor.getValue();
        assertEquals("test@example.com", sentMessage.getRecipients(MimeMessage.RecipientType.TO)[0].toString());
        assertEquals("Subject", sentMessage.getSubject());
        assertEquals(RENDERED_HTML_BODY, sentMessage.getContent().toString().trim());
    }

    @Test
    void passwordResetEmailUsesItsTemplate() {
        emailSenderService.sendPasswordResetEmail("test@example.com", "Reset", "Jan", "http://example.com/reset", 30);

        verify(thymeleafTemplateEngine).process(eq("password-reset-email"), any(Context.class));
        verify(javaMailSender).send(any(MimeMessage.class));
    }

    @Test
    void smtpFailureIsWrappedInDomainException() {
        doThrow(new MailSendException("SMTP down")).when(javaMailSender).send(any(MimeMessage.class));

        assertThrows(EmailSendingFailedException.class, () ->
                emailSenderService.sendPasswordResetEmail("test@example.com", "Reset", "Jan", "http://example.com/reset", 30));
    }
}
