package com.auction.system.server.dao;

import com.auction.system.model.auction.AutoBid;
import com.auction.system.model.user.User;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AutoBidDAO extends BaseDAO {
    private final UserDAO userDAO = new UserDAO();

    public boolean upsertAutoBid(AutoBid autoBid) {
        String sql = """
                INSERT INTO auto_bids (
                    id, item_id, bidder_id, max_bid, increment_amount, active, created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    id = VALUES(id),
                    max_bid = VALUES(max_bid),
                    increment_amount = VALUES(increment_amount),
                    active = VALUES(active),
                    created_at = VALUES(created_at)
                """;

        try (Connection conn = getConnection();
             PreparedStatement pstm = conn.prepareStatement(sql)) {

            pstm.setString(1, autoBid.getId());
            pstm.setString(2, autoBid.getItemId());
            pstm.setString(3, autoBid.getBidderId());
            pstm.setBigDecimal(4, BigDecimal.valueOf(autoBid.getMaxBid()));
            pstm.setBigDecimal(5, BigDecimal.valueOf(autoBid.getIncrementAmount()));
            pstm.setBoolean(6, autoBid.isActive());
            pstm.setTimestamp(7, toTimestamp(autoBid.getCreatedAt()));

            return pstm.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Loi upsert auto-bid: " + e.getMessage());
        }

        return false;
    }

    //lấy tất cả autobid đang bật của 1 item
    public List<AutoBid> findActiveByItemId(String itemId) {
        String sql = """
                SELECT *
                FROM auto_bids
                WHERE item_id = ?
                  AND active = TRUE
                ORDER BY max_bid DESC, created_at ASC
                """;

        List<AutoBid> autoBids = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement pstm = conn.prepareStatement(sql)) {

            pstm.setString(1, itemId);

            try (ResultSet rs = pstm.executeQuery()) {
                while (rs.next()) {
                    autoBids.add(mapResultSetToAutoBid(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Loi lay auto-bid theo item: " + e.getMessage());
        }

        return autoBids;
    }

    //tắt autobid của 1 bidder cho 1 item (khi bidder đặt trực tiếp hoặc khi item hết hạn)
    //dùng trong trường hợp hủy đấu giá
    public boolean deactivate(String itemId, String bidderId) {
        String sql = """
                UPDATE auto_bids
                SET active = FALSE
                WHERE item_id = ?
                  AND bidder_id = ?
                """;

        try (Connection conn = getConnection();
             PreparedStatement pstm = conn.prepareStatement(sql)) {

            pstm.setString(1, itemId);
            pstm.setString(2, bidderId);

            return pstm.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Loi tat auto-bid: " + e.getMessage());
        }

        return false;
    }

    private AutoBid mapResultSetToAutoBid(ResultSet rs) throws SQLException {
        AutoBid autoBid = new AutoBid();

        autoBid.setId(rs.getString("id"));
        autoBid.setItemId(rs.getString("item_id"));
        autoBid.setBidderId(rs.getString("bidder_id"));
        autoBid.setMaxBid(rs.getBigDecimal("max_bid").doubleValue());
        autoBid.setIncrementAmount(rs.getBigDecimal("increment_amount").doubleValue());
        autoBid.setActive(rs.getBoolean("active"));
        autoBid.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));

        User bidder = userDAO.findById(autoBid.getBidderId());
        if (bidder != null) {
            autoBid.setBidderUsername(bidder.getUserName());
        }

        return autoBid;
    }
}