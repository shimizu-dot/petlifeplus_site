package com.example.petlife.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Set;

@Mapper
public interface PlanFeatureMapper {

    @Select("""
        SELECT pf.feature_code
        FROM subscriptions s
        JOIN plans p ON p.id = s.plan_id
        JOIN plan_features pf ON pf.plan_id = p.id
        WHERE s.user_id = #{userId}
          AND s.deleted_at IS NULL
          AND s.status = 'ACTIVE'
          AND p.deleted_at IS NULL
          AND p.is_active = TRUE
          AND s.start_date <= CURRENT_DATE
          AND (s.end_date IS NULL OR s.end_date >= CURRENT_DATE)
        ORDER BY s.start_date DESC, s.id DESC
        """)
    Set<String> findActiveFeatureCodesByUserId(@Param("userId") Long userId);
}
