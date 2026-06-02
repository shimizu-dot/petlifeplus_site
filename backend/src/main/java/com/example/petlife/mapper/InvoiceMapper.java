package com.example.petlife.mapper;

import com.example.petlife.dto.billing.InvoiceRow;
import com.example.petlife.entity.InvoiceEntity;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface InvoiceMapper {

    @Select("""
        SELECT id, subscription_id, invoice_number, invoice_date, due_date, amount,
               payment_status, issued_at, paid_at, deleted_at, created_at, updated_at
        FROM invoices
        WHERE deleted_at IS NULL
        ORDER BY invoice_date DESC, id DESC
        LIMIT #{limit} OFFSET #{offset}
        """)
    List<InvoiceEntity> findAll(@Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM invoices WHERE deleted_at IS NULL")
    long countAll();

    @Select("""
        SELECT id, subscription_id, invoice_number, invoice_date, due_date, amount,
               payment_status, issued_at, paid_at, deleted_at, created_at, updated_at
        FROM invoices
        WHERE deleted_at IS NULL AND subscription_id = #{subscriptionId}
        ORDER BY invoice_date DESC
        """)
    List<InvoiceEntity> findBySubscriptionId(@Param("subscriptionId") Long subscriptionId);

    @Select("""
        SELECT id, subscription_id, invoice_number, invoice_date, due_date, amount,
               payment_status, issued_at, paid_at, deleted_at, created_at, updated_at
        FROM invoices WHERE id = #{id} AND deleted_at IS NULL
        """)
    InvoiceEntity findById(@Param("id") Long id);

    // INSERT...RETURNING は結果セットを返すため @Select を使用（@Insert では Long 戻り値に写像されない）
    @Select("""
        INSERT INTO invoices(subscription_id, invoice_number, invoice_date, due_date,
            amount, payment_status, issued_at, created_at, updated_at)
        VALUES(#{subscriptionId}, #{invoiceNumber}, #{invoiceDate}, #{dueDate},
            #{amount}, #{paymentStatus}, #{issuedAt}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        RETURNING id
        """)
    Long insertReturningId(InvoiceEntity invoice);

    @Update("""
        UPDATE invoices
        SET payment_status = #{paymentStatus}, paid_at = #{paidAt}, updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id} AND deleted_at IS NULL
        """)
    int updatePaymentStatus(InvoiceEntity invoice);

    @Update("""
        UPDATE invoices
        SET deleted_at = #{deletedAt}, updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id} AND deleted_at IS NULL
        """)
    int softDelete(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);

    @Select("""
        SELECT i.id             AS "invoiceId",
               i.invoice_number,
               i.invoice_date,
               i.due_date,
               i.amount,
               i.payment_status,
               i.issued_at,
               i.paid_at,
               i.subscription_id,
               p.name           AS "planName",
               p.monthly_fee    AS "monthlyFee",
               u.id             AS "ownerUserId",
               u.name           AS "ownerName",
               u.email          AS "ownerEmail",
               u.line_user_id   AS "lineUserId",
               s.end_date       AS "subscriptionEndDate"
        FROM invoices i
        JOIN subscriptions s ON s.id = i.subscription_id AND s.deleted_at IS NULL
        JOIN plans p ON p.id = s.plan_id
        JOIN users u ON u.id = s.user_id AND u.deleted_at IS NULL
        WHERE i.deleted_at IS NULL
        ORDER BY i.invoice_date DESC, i.id DESC
        LIMIT #{limit} OFFSET #{offset}
        """)
    List<InvoiceRow> findAllWithDetails(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT i.id             AS "invoiceId",
               i.invoice_number,
               i.invoice_date,
               i.due_date,
               i.amount,
               i.payment_status,
               i.issued_at,
               i.paid_at,
               i.subscription_id,
               p.name           AS "planName",
               p.monthly_fee    AS "monthlyFee",
               u.id             AS "ownerUserId",
               u.name           AS "ownerName",
               u.email          AS "ownerEmail",
               u.line_user_id   AS "lineUserId",
               s.end_date       AS "subscriptionEndDate"
        FROM invoices i
        JOIN subscriptions s ON s.id = i.subscription_id AND s.deleted_at IS NULL
        JOIN plans p ON p.id = s.plan_id
        JOIN users u ON u.id = s.user_id AND u.deleted_at IS NULL
        WHERE i.id = #{id} AND i.deleted_at IS NULL
        """)
    InvoiceRow findByIdWithDetails(@Param("id") Long id);

    /**
     * 支払期限が過ぎており未払い/一部払いのまま、かつオーナーがまだ ACTIVE な請求書一覧。
     * OverdueInvoiceScheduler が毎日実行し、通知送信 + アカウント停止を行う。
     */
    @Select("""
        SELECT i.id             AS "invoiceId",
               i.invoice_number,
               i.invoice_date,
               i.due_date,
               i.amount,
               i.payment_status,
               i.issued_at,
               i.paid_at,
               i.subscription_id,
               p.name           AS "planName",
               p.monthly_fee    AS "monthlyFee",
               u.id             AS "ownerUserId",
               u.name           AS "ownerName",
               u.email          AS "ownerEmail",
               u.line_user_id   AS "lineUserId",
               s.end_date       AS "subscriptionEndDate"
        FROM invoices i
        JOIN subscriptions s ON s.id = i.subscription_id AND s.deleted_at IS NULL
        JOIN plans p ON p.id = s.plan_id
        JOIN users u ON u.id = s.user_id AND u.deleted_at IS NULL AND u.status = 'ACTIVE'
        WHERE i.deleted_at IS NULL
          AND i.due_date < CURRENT_DATE
          AND i.payment_status IN ('UNPAID', 'PARTIAL')
        ORDER BY i.due_date
        """)
    List<InvoiceRow> findOverdueInvoicesWithActiveUsers();
}
