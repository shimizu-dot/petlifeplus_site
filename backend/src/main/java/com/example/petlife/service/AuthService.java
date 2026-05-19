package com.example.petlife.service;

import com.example.petlife.dto.auth.LoginRequest;
import com.example.petlife.dto.auth.LoginResponse;
import com.example.petlife.entity.UserEntity;
import com.example.petlife.exception.BadRequestException;
import com.example.petlife.mapper.AuthMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthMapper authMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(AuthMapper authMapper, BCryptPasswordEncoder passwordEncoder) {
        this.authMapper = authMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest request, HttpSession session) {
        UserEntity user = authMapper.findByEmail(request.email());
        if (user == null || !passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new BadRequestException("Invalid credentials");
        }
        session.setAttribute("userId", user.id());
        session.setAttribute("roleId", user.roleId());
        return new LoginResponse(user.id(), String.valueOf(user.roleId()), "logged_in");
    }

    public void logout(HttpSession session) {
        session.invalidate();
    }
}
