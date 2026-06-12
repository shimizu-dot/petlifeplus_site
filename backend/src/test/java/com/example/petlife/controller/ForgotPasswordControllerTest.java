package com.example.petlife.controller;

import com.example.petlife.service.PasswordResetService;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ForgotPasswordControllerTest {

    @Test
    void forgotSubmitRedirectsToSentPageAfterInitiatingReset() {
        PasswordResetService passwordResetService = mock(PasswordResetService.class);
        ForgotPasswordController controller = new ForgotPasswordController(passwordResetService);

        String view = controller.forgotSubmit("owner@example.com", new RedirectAttributesModelMap());

        verify(passwordResetService).initiateReset("owner@example.com");
        assertEquals("redirect:/app/forgot-password/sent", view);
    }
}
