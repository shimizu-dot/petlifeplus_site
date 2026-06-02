package com.example.petlife.service;

import com.example.petlife.dto.billing.InvoiceRow;
import com.example.petlife.dto.billing.PaymentForm;
import com.example.petlife.entity.InvoiceEntity;
import com.example.petlife.entity.PaymentEntity;
import com.example.petlife.exception.BadRequestException;
import com.example.petlife.mapper.InvoiceMapper;
import com.example.petlife.mapper.PaymentMapper;
import com.example.petlife.mapper.SubscriptionMapper;
import com.example.petlife.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);
    private static final DateTimeFormatter NUM_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final InvoiceMapper invoiceMapper;
    private final PaymentMapper paymentMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final UserMapper userMapper;
    private final BillingNotificationService notificationService;

    public BillingService(InvoiceMapper invoiceMapper,
                          PaymentMapper paymentMapper,
                          SubscriptionMapper subscriptionMapper,
                          UserMapper userMapper,
                          BillingNotificationService notificationService) {
        this.invoiceMapper = invoiceMapper;
        this.paymentMapper = paymentMapper;
        this.subscriptionMapper = subscriptionMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
    }

    /**
     * サブスクリプションに対して請求書を発行し、顧客へメール・LINE 通知を送信する。
     * 支払期限は請求日から 14 日後。
     */
    public InvoiceRow createInvoice(Long subscriptionId) {
        BigDecimal fee = subscriptionMapper.findMonthlyFeeBySubscriptionId(subscriptionId);
        if (fee == null) {
            throw new BadRequestException("対象サブスクリプションが見つかりません: " + subscriptionId);
        }

        if (invoiceMapper.countUnpaidBySubscriptionId(subscriptionId) > 0) {
            throw new BadRequestException("未払いまたは一部払いの請求書が既に存在します。お支払い完了後に更新申請してください。");
        }

        LocalDate today = LocalDate.now();
        String invoiceNumber = "INV-" + LocalDateTime.now().format(NUM_FMT) + "-" + subscriptionId;

        InvoiceEntity entity = new InvoiceEntity(
                null, subscriptionId, invoiceNumber,
                today, today.plusDays(14),
                fee, "UNPAID",
                LocalDateTime.now(), null,
                null, null, null
        );
        Long invoiceId = invoiceMapper.insertReturningId(entity);
        if (invoiceId == null) throw new BadRequestException("請求書の作成に失敗しました。再度お試しください。");

        InvoiceRow row = invoiceMapper.findByIdWithDetails(invoiceId);
        notificationService.sendInvoiceIssuedInApp(row);   // 同期（確実に届く）
        notificationService.notifyInvoiceIssued(row);      // @Async（メール・LINE）
        return row;
    }

    /**
     * 管理者が入金を登録する。
     * 累計支払額が請求金額以上になれば PAID、未満なら PARTIAL に更新する。
     */
    public void registerPayment(Long invoiceId, PaymentForm form) {
        InvoiceRow invoice = invoiceMapper.findByIdWithDetails(invoiceId);
        if (invoice == null) {
            throw new BadRequestException("請求書が見つかりません: " + invoiceId);
        }
        if ("PAID".equals(invoice.paymentStatus())) {
            throw new BadRequestException("この請求書はすでに支払い済みです");
        }

        PaymentEntity payment = new PaymentEntity(
                null, invoiceId,
                form.getPaidAmount(),
                form.getPaidAt().atStartOfDay(),
                form.getPaymentMethod(),
                form.getTransactionRef(),
                "SUCCEEDED",
                null, null, null
        );
        Long paymentId = paymentMapper.insertReturningId(payment);
        if (paymentId == null) {
            throw new BadRequestException("入金記録の保存に失敗しました。再度お試しください。");
        }

        // 累計入金額を集計してステータスを更新
        List<PaymentEntity> payments = paymentMapper.findByInvoiceId(invoiceId);
        BigDecimal totalPaid = payments.stream()
                .filter(p -> "SUCCEEDED".equals(p.status()))
                .map(PaymentEntity::paidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String newStatus = totalPaid.compareTo(invoice.amount()) >= 0 ? "PAID" : "PARTIAL";
        LocalDateTime paidAt = "PAID".equals(newStatus) ? LocalDateTime.now() : null;

        InvoiceEntity updated = new InvoiceEntity(
                invoiceId, invoice.subscriptionId(), invoice.invoiceNumber(),
                invoice.invoiceDate(), invoice.dueDate(),
                invoice.amount(), newStatus,
                invoice.issuedAt(), paidAt,
                null, null, null
        );
        int updatedRows = invoiceMapper.updatePaymentStatus(updated);
        if (updatedRows == 0) {
            throw new BadRequestException("請求ステータスの更新に失敗しました。再度お試しください。");
        }

        if ("PAID".equals(newStatus)) {
            // サブスクリプション有効期限を1ヶ月延長
            LocalDate currentEnd = subscriptionMapper.findEndDateById(invoice.subscriptionId());
            LocalDate newEnd = (currentEnd != null && currentEnd.isAfter(LocalDate.now()))
                    ? currentEnd.plusMonths(1)
                    : LocalDate.now().plusMonths(1);
            subscriptionMapper.updateEndDate(invoice.subscriptionId(), newEnd);

            // 延長後の情報を含む最新 InvoiceRow で通知
            InvoiceRow refreshed = invoiceMapper.findByIdWithDetails(invoiceId);
            notificationService.sendPaymentConfirmedInApp(refreshed);  // 同期
            notificationService.notifyPaymentConfirmed(refreshed);     // @Async（メール）
        }
    }

    /** 指定請求書の通知をアプリ内（同期）+ メール・LINE（非同期）で再送する */
    public void resendNotification(Long invoiceId) {
        InvoiceRow row = invoiceMapper.findByIdWithDetails(invoiceId);
        if (row == null) throw new BadRequestException("請求書が見つかりません: " + invoiceId);
        notificationService.sendInvoiceIssuedInApp(row);
        notificationService.notifyInvoiceIssued(row);
    }

    public List<InvoiceRow> findAllWithDetails(int page, int size) {
        return invoiceMapper.findAllWithDetails(size, (page - 1) * size);
    }

    public long countAll() {
        return invoiceMapper.countAll();
    }

    public InvoiceRow findByIdWithDetails(Long invoiceId) {
        InvoiceRow row = invoiceMapper.findByIdWithDetails(invoiceId);
        if (row == null) throw new BadRequestException("請求書が見つかりません: " + invoiceId);
        return row;
    }

    public List<PaymentEntity> findPaymentsByInvoiceId(Long invoiceId) {
        return paymentMapper.findByInvoiceId(invoiceId);
    }

    /**
     * 支払期限超過（UNPAID/PARTIAL）の請求に対して、停止通知を送りアカウントを停止する。
     * 停止対象は users.status='ACTIVE' のみ（InvoiceMapper 側で絞り込み済み）。
     */
    public void processOverdueInvoices() {
        List<InvoiceRow> overdueInvoices = invoiceMapper.findOverdueInvoicesWithActiveUsers();
        if (overdueInvoices.isEmpty()) return;

        // ユーザーごとに1回のみ処理する（複数の未払い請求書がある場合の重複を防ぐ）
        Set<Long> processedUserIds = new HashSet<>();

        for (InvoiceRow invoice : overdueInvoices) {
            if (processedUserIds.contains(invoice.ownerUserId())) continue;

            // アカウント停止を先に実行（通知失敗でも停止を保証）
            try {
                int suspended = userMapper.suspendUser(invoice.ownerUserId());
                if (suspended == 0) continue; // 既に停止済み（findOverdueInvoicesWithActiveUsers が ACTIVE のみ返すため通常ここには来ない）
                processedUserIds.add(invoice.ownerUserId());
                log.info("Suspended overdue user: invoiceId={} userId={}", invoice.invoiceId(), invoice.ownerUserId());
            } catch (Exception e) {
                log.error("Failed to suspend user for overdue invoice invoiceId={} userId={}: {}",
                        invoice.invoiceId(), invoice.ownerUserId(), e.getMessage(), e);
                continue;
            }

            // 停止後に通知（失敗してもアカウント停止は維持）
            try {
                notificationService.sendOverdueInApp(invoice);
                notificationService.notifyOverdueAsync(invoice);
            } catch (Exception e) {
                log.warn("Failed to send overdue notification for invoiceId={} userId={}: {}",
                        invoice.invoiceId(), invoice.ownerUserId(), e.getMessage(), e);
            }
        }
    }
}
