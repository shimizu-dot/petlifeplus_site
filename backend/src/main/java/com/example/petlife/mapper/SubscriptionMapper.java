package com.example.petlife.mapper;

import com.example.petlife.dto.subscription.SubscriptionRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SubscriptionMapper {

    @Select("""
        SELECT UPPER(p.name)
        FROM subscriptions s
        JOIN plans p ON p.id = s.plan_id
        WHERE s.user_id = #{userId}
          AND s.deleted_at IS NULL
          AND s.status = 'ACTIVE'
          AND p.deleted_at IS NULL
          AND p.is_active = TRUE
          AND (s.end_date IS NULL OR s.end_date >= CURRENT_DATE)
        ORDER BY s.start_date DESC, s.id DESC
        LIMIT 1
        """)
    String findActivePlanNameByUserId(@Param("userId") Long userId);

    @Select("""
        SELECT s.id, p.name AS "planName", pet.name AS "petName",
               u.name AS "ownerName",
               s.start_date AS "startDate", s.end_date AS "endDate",
               s.status, s.auto_renew AS "autoRenew"
        FROM subscriptions s
        JOIN plans p ON p.id = s.plan_id
        JOIN pets pet ON pet.id = s.pet_id
        JOIN users u ON u.id = s.user_id
        WHERE s.deleted_at IS NULL AND s.id = #{id} AND s.user_id = #{userId}
        LIMIT 1
        """)
    SubscriptionRow findRowByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Select("""
        SELECT s.id, p.name AS "planName", pet.name AS "petName",
               u.name AS "ownerName",
               s.start_date AS "startDate", s.end_date AS "endDate",
               s.status, s.auto_renew AS "autoRenew"
        FROM subscriptions s
        JOIN plans p ON p.id = s.plan_id
        JOIN pets pet ON pet.id = s.pet_id
        JOIN users u ON u.id = s.user_id
        WHERE s.deleted_at IS NULL AND s.user_id = #{userId}
        ORDER BY s.start_date DESC, s.id DESC
        LIMIT #{limit} OFFSET #{offset}
        """)
    List<SubscriptionRow> findRowsByUserId(@Param("userId") Long userId,
                                           @Param("limit") int limit,
                                           @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM subscriptions WHERE deleted_at IS NULL AND user_id = #{userId}")
    long countByUserId(@Param("userId") Long userId);

    @Select("""
        SELECT s.id, p.name AS "planName", pet.name AS "petName",
               u.name AS "ownerName",
               s.start_date AS "startDate", s.end_date AS "endDate",
               s.status, s.auto_renew AS "autoRenew"
        FROM subscriptions s
        JOIN plans p ON p.id = s.plan_id
        JOIN pets pet ON pet.id = s.pet_id
        JOIN users u ON u.id = s.user_id
        WHERE s.deleted_at IS NULL
        ORDER BY s.start_date DESC, s.id DESC
        LIMIT #{limit} OFFSET #{offset}
        """)
    List<SubscriptionRow> findAllRows(@Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM subscriptions WHERE deleted_at IS NULL")
    long countAll();

    @Select("""
        SELECT s.id, p.name AS "planName", pet.name AS "petName",
               u.name AS "ownerName",
               s.start_date AS "startDate", s.end_date AS "endDate",
               s.status, s.auto_renew AS "autoRenew"
        FROM subscriptions s
        JOIN plans p ON p.id = s.plan_id
        JOIN pets pet ON pet.id = s.pet_id
        JOIN users u ON u.id = s.user_id
        WHERE s.deleted_at IS NULL
          AND s.user_id = #{userId}
          AND s.status = 'ACTIVE'
          AND s.end_date IS NOT NULL
          AND s.end_date BETWEEN CURRENT_DATE AND CURRENT_DATE + INTERVAL '30 days'
        ORDER BY s.end_date
        """)
    List<SubscriptionRow> findUpcomingRenewalsByUserId(@Param("userId") Long userId);
}
