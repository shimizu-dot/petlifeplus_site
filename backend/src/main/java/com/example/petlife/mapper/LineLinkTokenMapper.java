package com.example.petlife.mapper;

import com.example.petlife.entity.LineLinkTokenEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface LineLinkTokenMapper {

    @Insert("""
            INSERT INTO line_link_tokens(user_id, token, expires_at, created_at)
            VALUES(#{userId}, #{token}, #{expiresAt}, CURRENT_TIMESTAMP)
            """)
    void insert(LineLinkTokenEntity entity);

    // INSERT...RETURNING は結果セットを返すため @Select を使用（@Insert では Long 戻り値に写像されない）
    @Select("""
            SELECT id, user_id, token, expires_at, used_at, created_at
            FROM line_link_tokens
            WHERE token = #{token}
              AND used_at IS NULL
              AND expires_at > CURRENT_TIMESTAMP
            """)
    LineLinkTokenEntity findValidByToken(@Param("token") String token);

    @Update("UPDATE line_link_tokens SET used_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    void markUsed(@Param("id") Long id);

    @Update("""
            UPDATE line_link_tokens SET used_at = CURRENT_TIMESTAMP
            WHERE user_id = #{userId} AND used_at IS NULL
            """)
    void invalidateByUserId(@Param("userId") Long userId);
}
