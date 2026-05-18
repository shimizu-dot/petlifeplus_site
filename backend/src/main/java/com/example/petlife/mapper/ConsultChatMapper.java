package com.example.petlife.mapper;

import com.example.petlife.entity.ConsultChatMessageEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ConsultChatMapper {

    @Select("""
        INSERT INTO consult_chat_messages(user_id, sender_type, message, created_at)
        VALUES(#{userId}, #{senderType}, #{message}, CURRENT_TIMESTAMP)
        RETURNING id
        """)
    Long insertReturningId(ConsultChatMessageEntity row);

    @Select("""
        SELECT id, user_id, sender_type, message, created_at
        FROM consult_chat_messages
        WHERE user_id = #{userId}
        ORDER BY id DESC
        LIMIT #{limit}
        """)
    List<ConsultChatMessageEntity> findRecentByUserId(@Param("userId") Long userId, @Param("limit") int limit);
}
