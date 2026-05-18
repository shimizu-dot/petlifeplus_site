package com.example.petlife.mapper;

import com.example.petlife.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserMapper {
    @Select("""
        SELECT id, role_id, name, email, password_hash, phone, status, last_login_at, deleted_at, created_at, updated_at
        FROM users
        WHERE deleted_at IS NULL
        ORDER BY id
        LIMIT #{limit} OFFSET #{offset}
        """)
    List<UserEntity> findAll(@Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM users WHERE deleted_at IS NULL")
    long countAll();

    @Select("""
        SELECT id, role_id, name, email, password_hash, phone, status, last_login_at, deleted_at, created_at, updated_at
        FROM users WHERE id = #{id} AND deleted_at IS NULL
        """)
    UserEntity findById(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM users WHERE email = #{email} AND deleted_at IS NULL")
    int existsByEmail(@Param("email") String email);

    @Select("""
        INSERT INTO users(role_id, name, email, password_hash, phone, status, created_at, updated_at)
        VALUES(#{roleId}, #{name}, #{email}, #{passwordHash}, #{phone}, #{status}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        RETURNING id
        """)
    Long insertReturningId(UserEntity user);

    @Update("""
        UPDATE users
        SET name = #{name}, email = #{email}, phone = #{phone}, status = #{status}, updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id} AND deleted_at IS NULL
        """)
    int update(UserEntity user);

    @Update("""
        UPDATE users
        SET deleted_at = #{deletedAt}, updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id} AND deleted_at IS NULL
        """)
    int softDelete(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);
}
