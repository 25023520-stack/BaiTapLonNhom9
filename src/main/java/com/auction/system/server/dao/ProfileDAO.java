package com.auction.system.server.dao;

import com.auction.system.common.payload.UserProfilePayload;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ProfileDAO extends BaseDAO {
    private static final int HISTORY_LIMIT = 100;

    public UserProfilePayload.BidderStats findBidderStats(String bidderId) {
        UserProfilePayload.BidderStats stats = new UserProfilePayload.BidderStats();
        try (Connection conn = getConnection()) {
            fillBidderBidCounts(conn, bidderId, stats);
            fillBidderLeadingCount(conn, bidderId, stats);
            fillBidderWonCounts(conn, bidderId, stats);
        } catch (SQLException exception) {
            throw new IllegalStateException("Cannot load bidder profile statistics", exception);
        }
        return stats;
    }

    public List<UserProfilePayload.BidHistoryEntry> findBidHistory(String bidderId) {
        String sql = """
                SELECT b.id AS bid_id,
                       b.item_id,
                       i.name AS item_name,
                       i.status AS item_status,
                       i.highest_bidder_id,
                       b.amount,
                       b.bid_time
                FROM bids b
                JOIN items i ON i.id = b.item_id
                WHERE b.bidder_id = ?
                ORDER BY b.bid_time DESC
                LIMIT ?
                """;

        List<UserProfilePayload.BidHistoryEntry> entries = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, bidderId);
            statement.setInt(2, HISTORY_LIMIT);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    UserProfilePayload.BidHistoryEntry entry = new UserProfilePayload.BidHistoryEntry();
                    entry.setBidId(rs.getString("bid_id"));
                    entry.setItemId(rs.getString("item_id"));
                    entry.setItemName(rs.getString("item_name"));
                    entry.setItemStatus(rs.getString("item_status"));
                    entry.setAmount(toDouble(rs.getBigDecimal("amount")));
                    entry.setBidTime(toLocalDateTime(rs.getTimestamp("bid_time")));
                    entry.setHighestBid(bidderId.equals(rs.getString("highest_bidder_id")));
                    entries.add(entry);
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Cannot load bidder bid history", exception);
        }
        return entries;
    }

    public List<UserProfilePayload.WonItemEntry> findWonItems(String bidderId) {
        String sql = """
                SELECT i.id AS item_id,
                       i.name AS item_name,
                       seller.username AS seller_username,
                       a.status,
                       a.final_price,
                       a.end_time
                FROM auctions a
                JOIN items i ON i.id = a.item_id
                LEFT JOIN users seller ON seller.id = i.seller_id
                WHERE a.winner_id = ?
                  AND a.status = 'PAID'
                ORDER BY a.end_time DESC
                LIMIT ?
                """;

        List<UserProfilePayload.WonItemEntry> entries = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, bidderId);
            statement.setInt(2, HISTORY_LIMIT);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    UserProfilePayload.WonItemEntry entry = new UserProfilePayload.WonItemEntry();
                    entry.setItemId(rs.getString("item_id"));
                    entry.setItemName(rs.getString("item_name"));
                    entry.setSellerUsername(rs.getString("seller_username"));
                    entry.setStatus(rs.getString("status"));
                    entry.setFinalPrice(toDouble(rs.getBigDecimal("final_price")));
                    entry.setEndTime(toLocalDateTime(rs.getTimestamp("end_time")));
                    entries.add(entry);
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Cannot load bidder won items", exception);
        }
        return entries;
    }

    public UserProfilePayload.SellerStats findSellerStats(String sellerId) {
        UserProfilePayload.SellerStats stats = new UserProfilePayload.SellerStats();
        String itemSql = """
                SELECT
                    SUM(CASE WHEN status = 'RUNNING' THEN 1 ELSE 0 END) AS running_items,
                    SUM(CASE WHEN status = 'FINISHED' THEN 1 ELSE 0 END) AS finished_unpaid_items,
                    SUM(CASE WHEN status = 'PAID' THEN 1 ELSE 0 END) AS sold_items,
                    SUM(CASE WHEN status = 'CANCELED' THEN 1 ELSE 0 END) AS canceled_items
                FROM items
                WHERE seller_id = ?
                """;
        String revenueSql = """
                SELECT COALESCE(SUM(a.final_price), 0) AS revenue
                FROM auctions a
                JOIN items i ON i.id = a.item_id
                WHERE i.seller_id = ?
                  AND a.status = 'PAID'
                """;

        try (Connection conn = getConnection()) {
            try (PreparedStatement statement = conn.prepareStatement(itemSql)) {
                statement.setString(1, sellerId);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        stats.setRunningItems(rs.getInt("running_items"));
                        stats.setFinishedUnpaidItems(rs.getInt("finished_unpaid_items"));
                        stats.setSoldItems(rs.getInt("sold_items"));
                        stats.setCanceledItems(rs.getInt("canceled_items"));
                    }
                }
            }
            try (PreparedStatement statement = conn.prepareStatement(revenueSql)) {
                statement.setString(1, sellerId);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        stats.setRevenue(toDouble(rs.getBigDecimal("revenue")));
                    }
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Cannot load seller profile statistics", exception);
        }

        int resolvedItems = stats.getSoldItems() + stats.getFinishedUnpaidItems() + stats.getCanceledItems();
        stats.setPaymentSuccessRate(resolvedItems == 0 ? 0 : (double) stats.getSoldItems() / resolvedItems);
        return stats;
    }

    public List<UserProfilePayload.SellerResultEntry> findSellerResults(String sellerId) {
        String sql = """
                SELECT i.id AS item_id,
                       i.name AS item_name,
                       i.status,
                       i.current_price,
                       i.end_time AS item_end_time,
                       a.final_price,
                       a.end_time AS auction_end_time,
                       winner.username AS winner_username
                FROM items i
                LEFT JOIN auctions a ON a.item_id = i.id
                LEFT JOIN users winner ON winner.id = a.winner_id
                WHERE i.seller_id = ?
                  AND i.status = 'PAID'
                ORDER BY COALESCE(i.end_time, a.end_time) DESC, i.name ASC
                LIMIT ?
                """;

        List<UserProfilePayload.SellerResultEntry> entries = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, sellerId);
            statement.setInt(2, HISTORY_LIMIT);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String status = rs.getString("status");
                    double finalPrice = toDouble(rs.getBigDecimal("final_price"));
                    if (finalPrice <= 0 && ("FINISHED".equals(status) || "PAID".equals(status))) {
                        finalPrice = toDouble(rs.getBigDecimal("current_price"));
                    }

                    UserProfilePayload.SellerResultEntry entry = new UserProfilePayload.SellerResultEntry();
                    entry.setItemId(rs.getString("item_id"));
                    entry.setItemName(rs.getString("item_name"));
                    entry.setWinnerUsername(rs.getString("winner_username"));
                    entry.setStatus(status);
                    entry.setFinalPrice(finalPrice);
                    Timestamp endTime = rs.getTimestamp("item_end_time");
                    if (endTime == null) {
                        endTime = rs.getTimestamp("auction_end_time");
                    }
                    entry.setEndTime(toLocalDateTime(endTime));
                    entries.add(entry);
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Cannot load seller result history", exception);
        }
        return entries;
    }

    private void fillBidderBidCounts(Connection conn, String bidderId, UserProfilePayload.BidderStats stats)
            throws SQLException {
        String sql = """
                SELECT COUNT(*) AS total_bids,
                       COUNT(DISTINCT item_id) AS participated_items
                FROM bids
                WHERE bidder_id = ?
                """;
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, bidderId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    stats.setTotalBids(rs.getInt("total_bids"));
                    stats.setParticipatedItems(rs.getInt("participated_items"));
                }
            }
        }
    }

    private void fillBidderLeadingCount(Connection conn, String bidderId, UserProfilePayload.BidderStats stats)
            throws SQLException {
        String sql = """
                SELECT COUNT(*) AS leading_items
                FROM items
                WHERE highest_bidder_id = ?
                  AND status = 'RUNNING'
                """;
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, bidderId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    stats.setLeadingItems(rs.getInt("leading_items"));
                }
            }
        }
    }

    private void fillBidderWonCounts(Connection conn, String bidderId, UserProfilePayload.BidderStats stats)
            throws SQLException {
        String sql = """
                SELECT
                    SUM(CASE WHEN status = 'FINISHED' THEN 1 ELSE 0 END) AS pending_payment_items,
                    SUM(CASE WHEN status = 'PAID' THEN 1 ELSE 0 END) AS paid_items,
                    COALESCE(SUM(CASE WHEN status = 'PAID' THEN final_price ELSE 0 END), 0) AS total_paid
                FROM auctions
                WHERE winner_id = ?
                """;
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, bidderId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    stats.setPendingPaymentItems(rs.getInt("pending_payment_items"));
                    stats.setPaidItems(rs.getInt("paid_items"));
                    stats.setTotalPaid(toDouble(rs.getBigDecimal("total_paid")));
                }
            }
        }
    }

    private double toDouble(BigDecimal value) {
        return value == null ? 0 : value.doubleValue();
    }
}
