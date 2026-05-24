package com.example.petlife.mapper;

import com.example.petlife.entity.PasswordResetTokenEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PasswordResetTokenMapper {

    @Insert("""
            INSERT INTO password_reset_tokens(user_id, token, expires_at, created_at)
            VALUES(#{userId}, #{token}, #{expiresAt}, CURRENT_TIMESTAMP)
            """)
    void insert(PasswordResetTokenEntity entity);

    @Select("""
            SELECT id, user_id, token, expires_at, used_at, created_at
            FROM password_reset_tokens
            WHERE token = #{token}
            """)
    PasswordResetTokenEntity findByToken(@Param("token") String token);

    @Update("""
            UPDATE password_reset_tokens
            SET used_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
            """)
    void markUsed(@Param("id") Long id);
}
