package com.example.petlife.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.TimeUnit;

@Configuration
public class WebResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(
                        "/webapp.html",
                        "/maintenance.html",
                        "/index.html",
                        "/f_contact.html",
                        "/f_flow.html",
                        "/f_info.html",
                        "/f_service.html"
                )
                .addResourceLocations(
                        "file:frontend/public/",
                        "file:../frontend/public/"
                );

        registry.addResourceHandler("/assets/**")
                .addResourceLocations(
                        "file:frontend/public/assets/",
                        "file:../frontend/public/assets/"
                )
                .setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic());
    }
}
