package com.example.petlife.mapper;

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
}
