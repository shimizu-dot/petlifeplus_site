package com.example.petlife.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserStatusCheckFilter userStatusCheckFilter;

    public SecurityConfig(UserStatusCheckFilter userStatusCheckFilter) {
        this.userStatusCheckFilter = userStatusCheckFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/slack/events", "/api/line/events")
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/", "/index.html", "/webapp.html",
                    "/f_contact.html", "/f_flow.html", "/f_info.html", "/f_service.html",
                    "/css/**", "/js/**", "/assets/**", "/images/**", "/uploads/**"
                ).permitAll()
                .requestMatchers("/api/slack/events", "/api/line/events").permitAll()
                .requestMatchers("/app/login").permitAll()
                .requestMatchers("/app/forgot-password", "/app/forgot-password/**").permitAll()
                .requestMatchers("/app/reset-password", "/app/reset-password/**").permitAll()
                // /app/admin/** の中で VET・STAFF にも開放するパスを先に列挙（順序重要）
                .requestMatchers("/app/admin/users", "/app/admin/users/**")
                    .hasAnyRole("ADMIN", "SUPER", "VET", "STAFF")
                .requestMatchers("/app/admin/announcements", "/app/admin/announcements/**")
                    .hasAnyRole("ADMIN", "SUPER", "STAFF")
                .requestMatchers("/app/admin/appointment-slots", "/app/admin/appointment-slots/**")
                    .hasAnyRole("ADMIN", "SUPER", "STAFF")
                // 残りの admin・reports は ADMIN のみ
                .requestMatchers("/app/admin/**", "/app/reports", "/app/reports/**")
                    .hasAnyRole("ADMIN", "SUPER")
                // 診療記録: VET・STAFF のみ（ADMIN は閲覧不可）
                .requestMatchers("/app/consultations", "/app/consultations/**")
                    .hasAnyRole("SUPER", "VET", "STAFF")
                // 診療予約・カレンダー: USER(一般)・VET・STAFF のみ（ADMIN は閲覧不可）
                .requestMatchers("/app/appointments", "/app/appointments/**")
                    .hasAnyRole("SUPER", "VET", "STAFF", "USER")
                .requestMatchers("/app/calendar", "/app/calendar/**")
                    .hasAnyRole("SUPER", "VET", "STAFF", "USER")
                .requestMatchers("/app/**").authenticated()
                .anyRequest().permitAll()
            )
            .formLogin(form -> form
                .loginPage("/app/login")
                .loginProcessingUrl("/app/login")
                .defaultSuccessUrl("/app/dashboard", true)
                .failureUrl("/app/login?error=true")
                .usernameParameter("email")
                .passwordParameter("password")
            )
            .logout(logout -> logout
                .logoutUrl("/app/logout")
                .logoutSuccessUrl("/app/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            )
            .exceptionHandling(e -> e
                .accessDeniedPage("/app/access-denied")
            )
            .addFilterAfter(userStatusCheckFilter, SecurityContextHolderFilter.class);
        return http.build();
    }

    @Bean
    BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
