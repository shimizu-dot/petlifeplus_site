package com.example.petlife.service;

import com.example.petlife.dto.contact.ContactRequest;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock JavaMailSender mailSender;

    @InjectMocks ContactService contactService;

    @Test
    void sendUsesUtf8EncodedFromHeader() throws Exception {
        ReflectionTestUtils.setField(contactService, "fromEmail", "support@h4mizoo.shop");
        ReflectionTestUtils.setField(contactService, "fromName", "PetLife Plus");
        ReflectionTestUtils.setField(contactService, "contactToEmail", "support@h4mizoo.shop");

        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);

        contactService.send(new ContactRequest("山田 花子", "yamada@example.com", "お問い合わせです。"));

        verify(mailSender).send(any(MimeMessage.class));
        InternetAddress from = (InternetAddress) message.getFrom()[0];
        assertEquals("support@h4mizoo.shop", from.getAddress());
        assertEquals("PetLife Plus", from.getPersonal());
        String rawFrom = message.getHeader("From", null);
        assertNotNull(rawFrom);
        assertEquals("PetLife Plus <support@h4mizoo.shop>", rawFrom);
    }
}
