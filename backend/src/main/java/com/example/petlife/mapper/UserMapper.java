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

    @Select("SELECT COUNT(*) FROM users WHERE email = #{email} AND id <> #{id} AND deleted_at IS NULL")
    int existsByEmailExcludingId(@Param("email") String email, @Param("id") Long id);

    @Select("""
        SELECT UPPER(p.name)
        FROM subscriptions s
        JOIN plans p ON p.id = s.plan_id
        WHERE s.user_id = #{userId}
          AND s.deleted_at IS NULL
          AND s.status = 'ACTIVE'
          AND p.deleted_at IS NULL
          AND p.is_active = TRUE
          AND s.start_date <= CURRENT_DATE
          AND (s.end_date IS NULL OR s.end_date >= CURRENT_DATE)
        ORDER BY s.start_date DESC, s.id DESC
        LIMIT 1
        """)
    String findActivePlanNameByUserId(@Param("userId") Long userId);

    @Select("""
        SELECT id
        FROM plans
        WHERE UPPER(name) = UPPER(#{planName})
          AND deleted_at IS NULL
          AND is_active = TRUE
        LIMIT 1
        """)
    Long findPlanIdByName(@Param("planName") String planName);

    @Update("""
        UPDATE subscriptions
        SET plan_id = #{planId},
            updated_at = CURRENT_TIMESTAMP
        WHERE user_id = #{userId}
          AND deleted_at IS NULL
          AND status = 'ACTIVE'
          AND start_date <= CURRENT_DATE
          AND (end_date IS NULL OR end_date >= CURRENT_DATE)
        """)
    int updateActiveSubscriptionPlanByUserId(@Param("userId") Long userId, @Param("planId") Long planId);

    @Select("""
        INSERT INTO users(role_id, name, email, password_hash, phone, status, created_at, updated_at)
        VALUES(#{roleId}, #{name}, #{email}, #{passwordHash}, #{phone}, #{status}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        RETURNING id
        """)
    Long insertReturningId(UserEntity user);

    @Update("""
        UPDATE users
        SET role_id = #{roleId},
            name = #{name},
            email = #{email},
            phone = #{phone},
            status = #{status},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id} AND deleted_at IS NULL
        """)
    int update(UserEntity user);

    @Update("""
        UPDATE users
        SET deleted_at = #{deletedAt}, updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id} AND deleted_at IS NULL
        """)
    int softDelete(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);

    @Update("""
        UPDATE users
        SET password_hash = #{passwordHash}, updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id} AND deleted_at IS NULL
        """)
    int updatePasswordById(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    @Select("SELECT COUNT(*) FROM users WHERE role_id = #{roleId} AND deleted_at IS NULL")
    long countByRoleId(@Param("roleId") Long roleId);

    @Update("""
        UPDATE users
        SET role_id = #{roleId},
            name = #{name},
            password_hash = #{passwordHash},
            phone = #{phone},
            status = 'ACTIVE',
            updated_at = CURRENT_TIMESTAMP
        WHERE email = #{email} AND deleted_at IS NULL
        """)
    int updateSeedUserByEmail(
            @Param("roleId") Long roleId,
            @Param("name") String name,
            @Param("email") String email,
            @Param("passwordHash") String passwordHash,
            @Param("phone") String phone
    );
}
