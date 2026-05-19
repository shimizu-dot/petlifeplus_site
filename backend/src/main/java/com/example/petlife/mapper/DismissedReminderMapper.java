package com.example.petlife.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Set;

@Mapper
public interface DismissedReminderMapper {

    @Insert("""
        INSERT INTO dismissed_reminders(user_id, reminder_key)
        VALUES(#{userId}, #{reminderKey})
        ON CONFLICT (user_id, reminder_key) DO NOTHING
        """)
    int insert(@Param("userId") Long userId, @Param("reminderKey") String reminderKey);

    @Select("SELECT reminder_key FROM dismissed_reminders WHERE user_id = #{userId}")
    Set<String> findKeysByUserId(@Param("userId") Long userId);
}
