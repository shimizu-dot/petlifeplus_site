package com.example.petlife.config;

import com.example.petlife.entity.UserEntity;
import com.example.petlife.mapper.AuthMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final AuthMapper authMapper;

    public UserDetailsServiceImpl(AuthMapper authMapper) {
        this.authMapper = authMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserEntity user = authMapper.findByEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + email);
        }

        boolean statusActive = "ACTIVE".equalsIgnoreCase(user.status());
        boolean subscriptionActive = user.roleId() != null && user.roleId() == 3L
                ? authMapper.countEffectiveSubscriptions(user.id()) > 0
                : true;
        boolean enabled = statusActive && subscriptionActive;

        return new LoginUser(
                user.id(),
                user.roleId(),
                user.name(),
                user.email(),
                user.passwordHash(),
                enabled
        );
    }
}
