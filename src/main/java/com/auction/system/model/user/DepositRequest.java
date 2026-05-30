package com.auction.system.model.user;

import com.auction.system.common.money.Money;

import java.io.Serializable;
import java.time.LocalDateTime;

public class DepositRequest implements Serializable {
    // Model dai dien cho mot yeu cau nap tien cua bidder.
    // Luong nghiep vu: bidder tao request PENDING, admin duyet thi request thanh APPROVED va tien duoc cong vao balance.
    private String id;
    private String bidderId;
    private String bidderName;
    private double amount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
    private String reviewedBy;

    public DepositRequest() {
    }

    public DepositRequest(String id, String bidderId, String bidderName, double amount, String status,
                          LocalDateTime createdAt, LocalDateTime reviewedAt, String reviewedBy) {
        this.id = id;
        this.bidderId = bidderId;
        this.bidderName = bidderName;
        this.amount = Money.normalize(amount);
        this.status = status;
        this.createdAt = createdAt;
        this.reviewedAt = reviewedAt;
        this.reviewedBy = reviewedBy;
    }

    public String getId() {
        return id;
    }

    public String getBidderId() {
        return bidderId;
    }

    public String getBidderName() {
        return bidderName;
    }

    public double getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }
}
