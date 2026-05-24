package com.example.petlife.mapper;

import com.example.petlife.dto.notification.NotificationRow;
import com.example.petlife.dto.subscription.RenewalHistoryRow;
import com.example.petlife.entity.NotificationEntity;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface NotificationMapper {

    @Select("""
        SELECT n.id, n.notification_type AS "notificationType", n.title, n.body,
               n.created_at AS "createdAt", nr.read_at AS "readAt"
        FROM notifications n
        JOIN notification_recipients nr ON nr.notification_id = n.id
        WHERE nr.user_id = #{userId}
          AND n.deleted_at IS NULL
          AND n.delivery_status = 'SENT'
        ORDER BY n.created_at DESC
        LIMIT #{limit} OFFSET #{offset}
        """)
    List<NotificationRow> findByUserId(@Param("userId") Long userId,
                                       @Param("limit") int limit,
                                       @Param("offset") int offset);

    @Select("""
        SELECT COUNT(*)
        FROM notifications n
        JOIN notification_recipients nr ON nr.notification_id = n.id
        WHERE nr.user_id = #{userId}
          AND n.deleted_at IS NULL
          AND n.delivery_status = 'SENT'
        """)
    long countByUserId(@Param("userId") Long userId);

    @Select("""
        SELECT COUNT(*)
        FROM notifications n
        JOIN notification_recipients nr ON nr.notification_id = n.id
        WHERE nr.user_id = #{userId}
          AND n.deleted_at IS NULL
          AND n.delivery_status = 'SENT'
          AND nr.read_at IS NULL
        """)
    long countUnreadByUserId(@Param("userId") Long userId);

    @Update("""
        UPDATE notification_recipients
        SET read_at = CURRENT_TIMESTAMP
        WHERE notification_id = #{notificationId}
          AND user_id = #{userId}
          AND read_at IS NULL
        """)
    int markAsRead(@Param("notificationId") Long notificationId, @Param("userId") Long userId);

    @Update("""
        UPDATE notification_recipients
        SET read_at = CURRENT_TIMESTAMP
        WHERE user_id = #{userId}
          AND read_at IS NULL
        """)
    int markAllAsRead(@Param("userId") Long userId);

    // --- notifications テーブル操作 ---

    @Select("""
        SELECT id, notification_type, title, body, scheduled_at, sent_at,
               delivery_status, created_by_user_id, deleted_at, created_at, updated_at
        FROM notifications WHERE id = #{id} AND deleted_at IS NULL
        """)
    NotificationEntity findById(@Param("id") Long id);

    @Select("""
        INSERT INTO notifications(notification_type, title, body, scheduled_at,
            delivery_status, created_by_user_id, created_at, updated_at)
        VALUES(#{notificationType}, #{title}, #{body}, #{scheduledAt},
            #{deliveryStatus}, #{createdByUserId}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        RETURNING id
        """)
    Long insertReturningId(NotificationEntity notification);

    @Update("""
        UPDATE notifications
        SET delivery_status = #{deliveryStatus}, sent_at = #{sentAt}, updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id} AND deleted_at IS NULL
        """)
    int updateStatus(@Param("id") Long id,
                     @Param("deliveryStatus") String deliveryStatus,
                     @Param("sentAt") LocalDateTime sentAt);

    @Update("""
        UPDATE notifications
        SET deleted_at = #{deletedAt}, updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id} AND deleted_at IS NULL
        """)
    int softDelete(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);

    // --- notification_recipients テーブル操作 ---

    @Insert("""
        INSERT INTO notification_recipients(notification_id, user_id, delivery_status)
        VALUES(#{notificationId}, #{userId}, 'PENDING')
        ON CONFLICT (notification_id, user_id) DO NOTHING
        """)
    int insertRecipient(@Param("notificationId") Long notificationId, @Param("userId") Long userId);

    @Select("""
        SELECT user_id FROM notification_recipients
        WHERE notification_id = #{notificationId}
        """)
    List<Long> findRecipientUserIds(@Param("notificationId") Long notificationId);

    @Update("""
        UPDATE notification_recipients
        SET delivery_status = #{deliveryStatus}
        WHERE notification_id = #{notificationId} AND user_id = #{userId}
        """)
    int updateRecipientStatus(@Param("notificationId") Long notificationId,
                              @Param("userId") Long userId,
                              @Param("deliveryStatus") String deliveryStatus);

    // --- サブスクリプション更新申請 ---

    @Select("""
        SELECT CAST(REGEXP_REPLACE(title, '^サブスクリプション更新申請 #', '') AS BIGINT)
        FROM notifications
        WHERE created_by_user_id = #{userId}
          AND title LIKE 'サブスクリプション更新申請 #%'
          AND deleted_at IS NULL
        """)
    List<Long> findRenewalRequestedSubscriptionIdsByUserId(@Param("userId") Long userId);

    @Select("""
        SELECT CAST(REGEXP_REPLACE(n.title, '^サブスクリプション更新申請 #', '') AS BIGINT) AS "subscriptionId",
               UPPER(p.name) AS "planName",
               n.created_at AS "requestedAt"
        FROM notifications n
        JOIN subscriptions s ON s.id = CAST(REGEXP_REPLACE(n.title, '^サブスクリプション更新申請 #', '') AS BIGINT)
        JOIN plans p ON p.id = s.plan_id
        WHERE n.created_by_user_id = #{userId}
          AND n.title LIKE 'サブスクリプション更新申請 #%'
          AND n.deleted_at IS NULL
        ORDER BY n.created_at DESC
        """)
    List<RenewalHistoryRow> findRenewalHistoryByUserId(@Param("userId") Long userId);
}
