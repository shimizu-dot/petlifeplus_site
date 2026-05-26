package com.example.petlife.mapper;

import com.example.petlife.entity.PaymentEntity;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PaymentMapper {

    @Select("""
        SELECT id, invoice_id, paid_amount, paid_at, payment_method,
               transaction_ref, status, deleted_at, created_at, updated_at
        FROM payments
        WHERE invoice_id = #{invoiceId}
          AND deleted_at IS NULL
        ORDER BY created_at DESC
        """)
    List<PaymentEntity> findByInvoiceId(@Param("invoiceId") Long invoiceId);

    @Select("""
        SELECT id, invoice_id, paid_amount, paid_at, payment_method,
               transaction_ref, status, deleted_at, created_at, updated_at
        FROM payments
        WHERE id = #{id} AND deleted_at IS NULL
        """)
    PaymentEntity findById(@Param("id") Long id);

    @Select("""
        INSERT INTO payments(invoice_id, paid_amount, paid_at, payment_method,
            transaction_ref, status, created_at, updated_at)
        VALUES(#{invoiceId}, #{paidAmount}, #{paidAt}, #{paymentMethod},
            #{transactionRef}, #{status}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        RETURNING id
        """)
    Long insertReturningId(PaymentEntity payment);

    @Update("""
        UPDATE payments
        SET status = #{status}, updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id} AND deleted_at IS NULL
        """)
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Update("""
        UPDATE payments
        SET deleted_at = #{deletedAt}, updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id} AND deleted_at IS NULL
        """)
    int softDelete(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);
}
