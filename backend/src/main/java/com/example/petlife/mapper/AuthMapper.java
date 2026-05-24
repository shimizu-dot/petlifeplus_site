package com.example.petlife.mapper;

import com.example.petlife.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AuthMapper {

    @Select("""
        SELECT id, role_id, name, email, password_hash, phone, slack_user_id, line_user_id, status, last_login_at, deleted_at, created_at, updated_at
        FROM users
        WHERE email = #{email} AND deleted_at IS NULL
        """)
    UserEntity findByEmail(@Param("email") String email);

    @Select("""
        SELECT COUNT(*)
        FROM subscriptions
        WHERE user_id = #{userId}
          AND deleted_at IS NULL
          AND status = 'ACTIVE'
          AND start_date <= CURRENT_DATE
          AND (end_date IS NULL OR end_date >= CURRENT_DATE)
        """)
    long countEffectiveSubscriptions(@Param("userId") Long userId);
}
