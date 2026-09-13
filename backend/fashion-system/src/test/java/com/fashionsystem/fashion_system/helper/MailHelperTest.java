package com.fashionsystem.fashion_system.helper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.helper.mail.MailAttachment;
import com.fashionsystem.fashion_system.helper.mail.MailRequest;
import com.fashionsystem.fashion_system.helper.mail.MailSendException;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.IContext;

class MailHelperTest {
    private JavaMailSender sender;
    private TemplateEngine templateEngine;
    private MailHelper helper;

    @BeforeEach
    void setUp() {
        sender = mock(JavaMailSender.class);
        templateEngine = mock(TemplateEngine.class);
        @SuppressWarnings("unchecked") ObjectProvider<TemplateEngine> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(templateEngine);
        helper = new MailHelper(sender, provider);
        ReflectionTestUtils.setField(helper, "fromAddress", "no-reply@example.com");
        ReflectionTestUtils.setField(helper, "fromName", "Fashion System");
        ReflectionTestUtils.setField(helper, "otpSubject", "OTP");
        when(sender.createMimeMessage()).thenAnswer(ignored -> new MimeMessage(Session.getInstance(new Properties())));
    }

    @Test
    void sendsTextToMultipleRecipientsWithCcAndBcc() throws Exception {
        MailRequest request = new MailRequest(
                List.of("one@example.com", "two@example.com"), List.of("cc@example.com"),
                List.of("bcc@example.com"), "Subject", "Plain body", null,
                "reply@example.com", List.of());

        helper.sendTextMail(request);

        MimeMessage message = sentMessage();
        assertThat(message.getAllRecipients()).hasSize(4);
        assertThat(message.getRecipients(Message.RecipientType.TO)).hasSize(2);
        assertThat(message.getRecipients(Message.RecipientType.CC)).hasSize(1);
        assertThat(message.getRecipients(Message.RecipientType.BCC)).hasSize(1);
        assertThat(message.getReplyTo()).hasSize(1);
    }

    @Test
    void sendsHtmlAndTemplateMail() {
        helper.sendHtmlMail(request("<b>Hello</b>", List.of()));
        when(templateEngine.process(eq("mail/base"), any(IContext.class))).thenReturn("<h1>Rendered</h1>");
        helper.sendTemplateMail(request(null, List.of()), "base", Map.of("title", "Hello"));
        verify(templateEngine).process(eq("mail/base"), any(IContext.class));
    }

    @Test
    void sendsSingleAndMultipleAttachments() throws Exception {
        MailAttachment pdf = new MailAttachment("report.pdf", new byte[] {1}, "application/pdf");
        MailAttachment excel = new MailAttachment("report.xlsx", new byte[] {2},
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        helper.sendMailWithAttachment(request("<b>Attached</b>", List.of()), pdf);
        helper.sendMailWithMultipleAttachments(request("<b>Attached</b>", List.of()), List.of(pdf, excel));
        List<MimeMessage> messages = sentMessages();
        messages.forEach(message -> {
            try { message.saveChanges(); } catch (Exception exception) { throw new AssertionError(exception); }
        });
        assertThat(messages.get(0).getContentType()).contains("multipart");
        assertThat(messages.get(1).getContentType()).contains("multipart");
    }

    @Test
    void wrapsSmtpException() {
        org.mockito.Mockito.doThrow(new org.springframework.mail.MailSendException("smtp"))
                .when(sender).send(any(MimeMessage.class));
        assertThatThrownBy(() -> helper.sendTextMail(new MailRequest(
                List.of("to@example.com"), "Subject", "Body", null)))
                .isInstanceOf(MailSendException.class)
                .hasMessage("Mail delivery failed");
    }

    private MailRequest request(String html, List<MailAttachment> attachments) {
        return new MailRequest(List.of("to@example.com"), List.of(), List.of(), "Subject",
                html == null ? "fallback" : null, html, null, attachments);
    }

    private MimeMessage sentMessage() {
        return sentMessages().getLast();
    }

    private List<MimeMessage> sentMessages() {
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender, org.mockito.Mockito.atLeastOnce()).send(captor.capture());
        return captor.getAllValues();
    }
}
