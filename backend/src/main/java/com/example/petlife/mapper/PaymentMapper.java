package com.example.petlife.mapper;

import com.example.petlife.entity.PaymentEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PaymentMapper {

    @Select("""
        SELECT id, invoice_id, paid_amount, paid_at, payment_method,
               transaction_ref, status, created_at
        FROM payments
        WHERE invoice_id = #{invoiceId}
        ORDER BY created_at DESC
        """)
    List<PaymentEntity> findByInvoiceId(@Param("invoiceId") Long invoiceId);

    @Select("""
        SELECT id, invoice_id, paid_amount, paid_at, payment_method,
               transaction_ref, status, created_at
        FROM payments WHERE id = #{id}
        """)
    PaymentEntity findById(@Param("id") Long id);

    @Select("""
        INSERT INTO payments(invoice_id, paid_amount, paid_at, payment_method,
            transaction_ref, status, created_at)
        VALUES(#{invoiceId}, #{paidAmount}, #{paidAt}, #{paymentMethod},
            #{transactionRef}, #{status}, CURRENT_TIMESTAMP)
        RETURNING id
        """)
    Long insertReturningId(PaymentEntity payment);

    @Update("""
        UPDATE payments
        SET status = #{status}, updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id}
        """)
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
