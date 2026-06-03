package com.example.petlife.config;

import com.example.petlife.mapper.AuthMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 認証済みリクエストごとに DB の users.status を確認するフィルター。
 * OverdueInvoiceScheduler 等で SUSPENDED に変更されたユーザーを
 * セッション有効期限を待たずに即時ログアウトさせる。
 */
@Component
public class UserStatusCheckFilter extends OncePerRequestFilter {

    private final AuthMapper authMapper;

    public UserStatusCheckFilter(AuthMapper authMapper) {
        this.authMapper = authMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated()
                && auth.getPrincipal() instanceof LoginUser loginUser) {

            String status = authMapper.findStatusById(loginUser.id());
            if (!"ACTIVE".equalsIgnoreCase(status)) {
                SecurityContextHolder.clearContext();
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                response.sendRedirect(request.getContextPath() + "/app/login?suspended=true");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/assets/")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/uploads/")
                || path.startsWith("/images/")
                || path.equals("/app/login")
                || path.equals("/app/logout")
                || path.startsWith("/app/forgot-password")
                || path.startsWith("/app/reset-password");
        // /api/** は除外しない:
        //   - 外部Webhook (/api/slack/events 等) は permitAll でセッションなし → auth が LoginUser にならないため素通り
        //   - /api/appointments は認証必須なので SUSPENDED チェックを適用すべき
    }
}
