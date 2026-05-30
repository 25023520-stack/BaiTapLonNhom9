package com.auction.system.common.payload;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserProfilePayload implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String userId;
    private String fullName;
    private String username;
    private String email;
    private String role;
    private boolean approved;
    private double balance;
    private BidderStats bidderStats;
    private SellerStats sellerStats;
    private List<BidHistoryEntry> bidHistory = new ArrayList<>();
    private List<WonItemEntry> wonItems = new ArrayList<>();
    private List<SellerResultEntry> sellerResults = new ArrayList<>();

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public BidderStats getBidderStats() {
        return bidderStats;
    }

    public void setBidderStats(BidderStats bidderStats) {
        this.bidderStats = bidderStats;
    }

    public SellerStats getSellerStats() {
        return sellerStats;
    }

    public void setSellerStats(SellerStats sellerStats) {
        this.sellerStats = sellerStats;
    }

    public List<BidHistoryEntry> getBidHistory() {
        return bidHistory;
    }

    public void setBidHistory(List<BidHistoryEntry> bidHistory) {
        this.bidHistory = bidHistory == null ? new ArrayList<>() : bidHistory;
    }

    public List<WonItemEntry> getWonItems() {
        return wonItems;
    }

    public void setWonItems(List<WonItemEntry> wonItems) {
        this.wonItems = wonItems == null ? new ArrayList<>() : wonItems;
    }

    public List<SellerResultEntry> getSellerResults() {
        return sellerResults;
    }

    public void setSellerResults(List<SellerResultEntry> sellerResults) {
        this.sellerResults = sellerResults == null ? new ArrayList<>() : sellerResults;
    }

    public static class BidderStats implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private int totalBids;
        private int participatedItems;
        private int leadingItems;
        private int pendingPaymentItems;
        private int paidItems;
        private double totalPaid;

        public int getTotalBids() {
            return totalBids;
        }

        public void setTotalBids(int totalBids) {
            this.totalBids = totalBids;
        }

        public int getParticipatedItems() {
            return participatedItems;
        }

        public void setParticipatedItems(int participatedItems) {
            this.participatedItems = participatedItems;
        }

        public int getLeadingItems() {
            return leadingItems;
        }

        public void setLeadingItems(int leadingItems) {
            this.leadingItems = leadingItems;
        }

        public int getPendingPaymentItems() {
            return pendingPaymentItems;
        }

        public void setPendingPaymentItems(int pendingPaymentItems) {
            this.pendingPaymentItems = pendingPaymentItems;
        }

        public int getPaidItems() {
            return paidItems;
        }

        public void setPaidItems(int paidItems) {
            this.paidItems = paidItems;
        }

        public double getTotalPaid() {
            return totalPaid;
        }

        public void setTotalPaid(double totalPaid) {
            this.totalPaid = totalPaid;
        }
    }

    public static class SellerStats implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private int runningItems;
        private int finishedUnpaidItems;
        private int soldItems;
        private int canceledItems;
        private double revenue;
        private double paymentSuccessRate;

        public int getRunningItems() {
            return runningItems;
        }

        public void setRunningItems(int runningItems) {
            this.runningItems = runningItems;
        }

        public int getFinishedUnpaidItems() {
            return finishedUnpaidItems;
        }

        public void setFinishedUnpaidItems(int finishedUnpaidItems) {
            this.finishedUnpaidItems = finishedUnpaidItems;
        }

        public int getSoldItems() {
            return soldItems;
        }

        public void setSoldItems(int soldItems) {
            this.soldItems = soldItems;
        }

        public int getCanceledItems() {
            return canceledItems;
        }

        public void setCanceledItems(int canceledItems) {
            this.canceledItems = canceledItems;
        }

        public double getRevenue() {
            return revenue;
        }

        public void setRevenue(double revenue) {
            this.revenue = revenue;
        }

        public double getPaymentSuccessRate() {
            return paymentSuccessRate;
        }

        public void setPaymentSuccessRate(double paymentSuccessRate) {
            this.paymentSuccessRate = paymentSuccessRate;
        }
    }

    public static class BidHistoryEntry implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String bidId;
        private String itemId;
        private String itemName;
        private String itemStatus;
        private double amount;
        private LocalDateTime bidTime;
        private boolean highestBid;

        public String getBidId() {
            return bidId;
        }

        public void setBidId(String bidId) {
            this.bidId = bidId;
        }

        public String getItemId() {
            return itemId;
        }

        public void setItemId(String itemId) {
            this.itemId = itemId;
        }

        public String getItemName() {
            return itemName;
        }

        public void setItemName(String itemName) {
            this.itemName = itemName;
        }

        public String getItemStatus() {
            return itemStatus;
        }

        public void setItemStatus(String itemStatus) {
            this.itemStatus = itemStatus;
        }

        public double getAmount() {
            return amount;
        }

        public void setAmount(double amount) {
            this.amount = amount;
        }

        public LocalDateTime getBidTime() {
            return bidTime;
        }

        public void setBidTime(LocalDateTime bidTime) {
            this.bidTime = bidTime;
        }

        public boolean isHighestBid() {
            return highestBid;
        }

        public void setHighestBid(boolean highestBid) {
            this.highestBid = highestBid;
        }
    }

    public static class WonItemEntry implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String itemId;
        private String itemName;
        private String sellerUsername;
        private String status;
        private double finalPrice;
        private LocalDateTime endTime;

        public String getItemId() {
            return itemId;
        }

        public void setItemId(String itemId) {
            this.itemId = itemId;
        }

        public String getItemName() {
            return itemName;
        }

        public void setItemName(String itemName) {
            this.itemName = itemName;
        }

        public String getSellerUsername() {
            return sellerUsername;
        }

        public void setSellerUsername(String sellerUsername) {
            this.sellerUsername = sellerUsername;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public double getFinalPrice() {
            return finalPrice;
        }

        public void setFinalPrice(double finalPrice) {
            this.finalPrice = finalPrice;
        }

        public LocalDateTime getEndTime() {
            return endTime;
        }

        public void setEndTime(LocalDateTime endTime) {
            this.endTime = endTime;
        }
    }

    public static class SellerResultEntry implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String itemId;
        private String itemName;
        private String winnerUsername;
        private String status;
        private double finalPrice;
        private LocalDateTime endTime;

        public String getItemId() {
            return itemId;
        }

        public void setItemId(String itemId) {
            this.itemId = itemId;
        }

        public String getItemName() {
            return itemName;
        }

        public void setItemName(String itemName) {
            this.itemName = itemName;
        }

        public String getWinnerUsername() {
            return winnerUsername;
        }

        public void setWinnerUsername(String winnerUsername) {
            this.winnerUsername = winnerUsername;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public double getFinalPrice() {
            return finalPrice;
        }

        public void setFinalPrice(double finalPrice) {
            this.finalPrice = finalPrice;
        }

        public LocalDateTime getEndTime() {
            return endTime;
        }

        public void setEndTime(LocalDateTime endTime) {
            this.endTime = endTime;
        }
    }
}
