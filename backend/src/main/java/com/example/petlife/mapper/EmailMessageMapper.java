package com.example.petlife.mapper;

import com.example.petlife.entity.EmailMessageEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface EmailMessageMapper {

    @Select("""
        SELECT id, template_id, recipient_user_id, pet_id, appointment_id, invoice_id,
               subject, body, send_timing_at, status, error_message, created_at, sent_at
        FROM email_messages
        WHERE recipient_user_id = #{userId}
        ORDER BY created_at DESC
        LIMIT #{limit} OFFSET #{offset}
        """)
    List<EmailMessageEntity> findByUserId(@Param("userId") Long userId,
                                          @Param("limit") int limit,
                                          @Param("offset") int offset);

    @Select("""
        SELECT id, template_id, recipient_user_id, pet_id, appointment_id, invoice_id,
               subject, body, send_timing_at, status, error_message, created_at, sent_at
        FROM email_messages
        WHERE status = 'QUEUED'
          AND (send_timing_at IS NULL OR send_timing_at <= CURRENT_TIMESTAMP)
        ORDER BY created_at
        LIMIT #{limit}
        """)
    List<EmailMessageEntity> findPending(@Param("limit") int limit);

    @Select("""
        INSERT INTO email_messages(template_id, recipient_user_id, pet_id, appointment_id, invoice_id,
            subject, body, send_timing_at, status, created_at)
        VALUES(#{templateId}, #{recipientUserId}, #{petId}, #{appointmentId}, #{invoiceId},
            #{subject}, #{body}, #{sendTimingAt}, 'QUEUED', CURRENT_TIMESTAMP)
        RETURNING id
        """)
    Long insertReturningId(EmailMessageEntity message);

    @Update("""
        UPDATE email_messages
        SET status = #{status}, error_message = #{errorMessage},
            sent_at = CASE WHEN #{status} = 'SENT' THEN CURRENT_TIMESTAMP ELSE NULL END
        WHERE id = #{id}
        """)
    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("errorMessage") String errorMessage);
}
