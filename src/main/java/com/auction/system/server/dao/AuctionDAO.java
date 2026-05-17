package com.auction.system.server.dao;

import com.auction.system.model.auction.Auction;
import com.auction.system.model.auction.AuctionStatus;
import com.auction.system.model.item.Item;

import java.math.BigDecimal;
import java.sql.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionDAO extends BaseDAO {
    private ItemDAO itemDAO = new ItemDAO();

    public boolean insertAuction(
            Connection conn,
            String auctionId,
            String itemId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            AuctionStatus status
    ) throws SQLException {
        String sql = """
                INSERT INTO auctions (id, item_id, start_time, end_time, status, final_price)
                VALUES (?, ?, ?, ?, ? ,? )
                """;

        try (PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setString(1, auctionId);
            pstm.setString(2, itemId);
            pstm.setTimestamp(3, Timestamp.valueOf(startTime));
            pstm.setTimestamp(4, Timestamp.valueOf(endTime));
            pstm.setString(5, status.name());
            pstm.setBigDecimal(6, BigDecimal.ZERO);

            return pstm.executeUpdate() > 0;

        }
    }

    public boolean existsByItemId(
            Connection conn,
            String itemId
    ) throws SQLException {
        String sql = """
                SELECT COUNT(*) FROM auctions WHERE item_id = ?
                """;
        try (PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setString(1, itemId);

            try (ResultSet rs = pstm.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }

    }

    public boolean updateAuctionTimeAndStatus(
            Connection conn,
            String itemId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            AuctionStatus status
    ) throws SQLException {
        String sql = """
                UPDATE auctions 
                SET start_time = ?, end_time = ?,
                status = ? WHERE item_id = ?
                """;

        try(PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setTimestamp(1, Timestamp.valueOf(startTime));
            pstm.setTimestamp(2, Timestamp.valueOf(endTime));

            pstm.setString(3, status.name());
            pstm.setString(4, itemId);

            return pstm.executeUpdate() > 0;
        }
    }

    public boolean updateStatus(
            Connection conn,
            String itemId,
            AuctionStatus status
    ) throws SQLException {
        String sql = """
                UPDATE auctions 
                SET status = ? WHERE item_id = ?
                """;

        try(PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setString(1, status.name());
            pstm.setString(2,itemId);

            return pstm.executeUpdate() > 0;
        }
    }

    public boolean finishAuction(
            Connection conn,
            String itemId,
            String winnerId,
            Double finalPrice
    ) throws SQLException {
        String sql = """
                UPDATE auctions SET status = ?, winner_id = ?, final_price = ? 
                WHERE item_id = ?
                """;

        try (PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setString(1, AuctionStatus.FINISHED.name());
            pstm.setString(2, winnerId);
            pstm.setBigDecimal(3, BigDecimal.valueOf(finalPrice));
            pstm.setString(4, itemId);

            return pstm.executeUpdate() > 0;
        }
    }

    public String findAuctionByItemId(
            Connection conn,
            String itemId
    ) throws SQLException {
        String sql = """
                SELECT id FROM auctions 
                WHERE item_id = ?
                LIMIT 1
                """;

        try (PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setString(1, itemId);

            try (ResultSet rs = pstm.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("id");
                }
            }
        }

        return null;

    }



}
