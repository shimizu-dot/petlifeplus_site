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
        // 一般ユーザーは契約情報の有無で機能を制御する。
        // ログイン自体は status で判定し、契約未設定でも Light 扱いで入れるようにする。
        boolean enabled = statusActive;

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
