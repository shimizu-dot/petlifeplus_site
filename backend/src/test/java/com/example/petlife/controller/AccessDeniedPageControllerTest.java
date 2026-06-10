package com.example.petlife.controller;

import com.example.petlife.config.LoginUser;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessDeniedPageControllerTest {

    @Test
    void accessDeniedTemplateRendersForAuthenticatedUser() {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setTemplateMode("HTML");
        resolver.setCacheable(false);
        engine.setTemplateResolver(resolver);

        LoginUser currentUser = new LoginUser(
                1L, 3L, "オーナー", "owner@example.com", "hash", true);
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setContextPath("");
        request.setRequestURI("/app/access-denied");
        request.setServletPath("/app/access-denied");
        MockHttpServletResponse response = new MockHttpServletResponse();
        WebContext context = new WebContext(
                JakartaServletWebApplication.buildApplication(servletContext).buildExchange(request, response));
        context.setVariable("currentUser", currentUser);
        context.setVariable("requestURI", "/app/access-denied");
        context.setVariable("bodyClass", "role-user");
        context.setVariable("planLabel", "ライト");
        context.setVariable("canUseAiSymptom", false);
        context.setVariable("canUsePrioritySupport", false);

        String html = assertDoesNotThrow(() -> engine.process("error/access-denied", context));
        assertTrue(html.contains("この機能は利用できません"));
    }
}
