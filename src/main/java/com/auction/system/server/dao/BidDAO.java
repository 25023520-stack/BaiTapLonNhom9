package com.auction.system.server.dao;

import com.auction.system.common.money.Money;
import com.auction.system.model.auction.Bid;
import com.auction.system.model.user.Bidder;
import com.auction.system.model.user.User;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class BidDAO extends BaseDAO {
    private final UserDAO userDAO = new UserDAO();

    public boolean insertBid(
            Connection conn,
            String id,
            String auctionId,
            String itemId,
            String bidderId,
            double amount,
            LocalDateTime bidTime
    ) throws SQLException {
        String sql = """
                INSERT INTO bids (id, auction_id, item_id, bidder_id, amount, bid_time)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setString(1, id);
            pstm.setString(2, auctionId);
            pstm.setString(3, itemId);
            pstm.setString(4, bidderId);
            pstm.setBigDecimal(5, Money.toDatabaseAmount(amount));
            pstm.setTimestamp(6, toTimestamp(bidTime));

            return pstm.executeUpdate() > 0;
        }
    }

    public List<Bid> findByItemId(String itemId) {
        String sql = """
                SELECT * FROM bids
                WHERE item_id = ?
                ORDER BY bid_time ASC
                """;

        List<Bid> bids = new ArrayList<>();

        try(Connection conn = getConnection();
            PreparedStatement pstm = conn.prepareStatement(sql)) {

            pstm.setString(1, itemId);

            try (ResultSet rs = pstm.executeQuery()) {
                while (rs.next()) {
                    bids.add(mapResultSetToBid(rs));
                }
            }
        }catch(SQLException e) {
            System.err.println("Loi lay bid theo item: " + e.getMessage());
        }
        return bids;
    }

    public List<Bid> findByAuctionId(String auctionId) {
        String sql = """
                SELECT * FROM bids
                WHERE auction_id = ?
                ORDER BY bid_time ASC
                """;

        List<Bid> bids = new ArrayList<>();

        try(Connection conn = getConnection();
            PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setString(1, auctionId);

            try (ResultSet rs = pstm.executeQuery()) {
                while (rs.next()) {
                    bids.add(mapResultSetToBid(rs));
                }
            }
        }catch(SQLException e) {
            System.err.println("Loi lay bid theo auction: " + e.getMessage());
        }
        return bids;
    }

    public Bid findHighestBidByItemId(String itemId) {
        String sql = """
                SELECT * FROM bids 
                WHERE item_id = ?
                ORDER BY amount DESC, bid_time ASC
                LIMIT 1
                """;

        try (Connection conn = getConnection();
             PreparedStatement pstm = conn.prepareStatement(sql) ) {

            pstm.setString(1, itemId);

            try(ResultSet rs = pstm.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToBid(rs);
                }
            }
        }catch (SQLException e) {
            System.err.println("Loi lay bid cao nhat theo item: " + e.getMessage());
        }

        return null;
    }

    private Bid mapResultSetToBid(ResultSet rs) throws SQLException {
        String bidId = rs.getString("id");
        String bidderId = rs.getString("bidder_id");
        BigDecimal amountValue = rs.getBigDecimal("amount");
        LocalDateTime bidTime = toLocalDateTime(rs.getTimestamp("bid_time"));

        User user = userDAO.findById(bidderId);

        if(!(user instanceof Bidder bidder)) {
            throw new SQLException("User khong phai Bidder hoac khong ton tai: " + bidderId);
        }

        double amount = amountValue != null ? amountValue.doubleValue() : 0;

        return new Bid(bidId, bidder, amount, bidTime);
    }

}
