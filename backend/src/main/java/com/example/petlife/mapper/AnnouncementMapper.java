package com.example.petlife.mapper;

import com.example.petlife.entity.AnnouncementEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AnnouncementMapper {

    @Select("""
        SELECT id, title, body, is_active AS "isActive", created_by_user_id AS "createdByUserId",
               created_at AS "createdAt", updated_at AS "updatedAt"
        FROM announcements
        WHERE is_active = TRUE
        ORDER BY created_at DESC
        """)
    List<AnnouncementEntity> findActive();

    @Select("""
        SELECT id, title, body, is_active AS "isActive", created_by_user_id AS "createdByUserId",
               created_at AS "createdAt", updated_at AS "updatedAt"
        FROM announcements
        ORDER BY created_at DESC
        """)
    List<AnnouncementEntity> findAll();

    // INSERT...RETURNING は結果セットを返すため @Select を使用（@Insert では Long 戻り値に写像されない）
    @Select("""
        INSERT INTO announcements(title, body, is_active, created_by_user_id, created_at, updated_at)
        VALUES(#{title}, #{body}, TRUE, #{createdByUserId}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        RETURNING id
        """)
    Long insertReturningId(AnnouncementEntity row);

    @Update("""
        UPDATE announcements SET is_active = #{isActive}, updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id}
        """)
    int updateIsActive(@Param("id") Long id, @Param("isActive") boolean isActive);

    @Update("DELETE FROM announcements WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
