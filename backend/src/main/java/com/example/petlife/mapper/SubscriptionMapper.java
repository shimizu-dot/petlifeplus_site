package com.example.petlife.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
}
