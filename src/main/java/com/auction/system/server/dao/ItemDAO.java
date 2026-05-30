package com.auction.system.server.dao;

import com.auction.system.factory.ItemFactory;
import com.auction.system.common.money.Money;
import com.auction.system.model.auction.AuctionStatus;
import com.auction.system.model.item.Item;
import com.auction.system.model.user.User;

import java.math.BigDecimal;

import java.sql.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO extends BaseDAO {
    private final BidDAO bidDAO = new BidDAO();
    private final UserDAO userDAO = new UserDAO();

    public boolean insertItem(Item item) {
        String sql = """
                INSERT INTO items (
                     id, name, description, category, image_path,
                     start_price, current_price, status, auction_approved,
                     seller_id, highest_bidder_id,
                     start_time, end_time
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = getConnection();
             PreparedStatement pstm = conn.prepareStatement(sql)) {

            double currentPrice = item.getCurrentPrice() > 0
                    ? item.getCurrentPrice()
                    : item.getStartPrice();

            AuctionStatus status = item.getStatus() != null
                    ? item.getStatus()
                    : AuctionStatus.OPEN;

            pstm.setString(1, item.getId());
            pstm.setString(2, item.getName());
            pstm.setString(3, item.getDescription());
            pstm.setString(4, item.getCategory());
            pstm.setString(5, item.getImagePath());

            pstm.setBigDecimal(6, Money.toDatabaseAmount(item.getStartPrice()));
            pstm.setBigDecimal(7, Money.toDatabaseAmount(currentPrice));

            pstm.setString(8, status.name());
            pstm.setBoolean(9, item.isAuctionApproved());
            pstm.setString(10, item.getSellerId());
            pstm.setString(11, item.getHighestBidderId());

            pstm.setTimestamp(12, toTimestamp(item.getStartTime()));
            pstm.setTimestamp(13, toTimestamp(item.getEndTime()));

            return pstm.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Loi them item: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    public Item findById(String id) {
        String sql = "SELECT * FROM items WHERE id = ?";

        try(Connection conn = getConnection();
            PreparedStatement pstm = conn.prepareStatement(sql)) {

            pstm.setString(1, id);

            try(ResultSet rs = pstm.executeQuery()){
                if(rs.next()) {
                    return mapResultSetToItem(rs);
                }

            }
        }catch (SQLException e) {
            System.err.println("loi tim id: "  + e.getMessage());
            e.printStackTrace();
        }

        return null;
     }

    public List<Item> findAll() {
        String sql = "SELECT * FROM items";
        List<Item> items = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                items.add(mapResultSetToItem(rs));
            }

        } catch (SQLException e) {
            System.err.println("Loi lay danh sach item: " + e.getMessage());
        }

        return items;
    }


    private Item mapResultSetToItem(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String name = rs.getString("name");
        String description = rs.getString("description");

        BigDecimal startPriceValue = rs.getBigDecimal("start_price");
        BigDecimal currentPriceValue = rs.getBigDecimal("current_price");

        double startPrice = startPriceValue != null ? startPriceValue.doubleValue() : 0;
        double currentPrice = currentPriceValue != null ? currentPriceValue.doubleValue() : startPrice;

        String statusText = rs.getString("status");
        AuctionStatus status = statusText != null
                ? AuctionStatus.valueOf(statusText.trim().toUpperCase())
                : AuctionStatus.OPEN;

        Item item = ItemFactory.createItem(
                rs.getString("category"),
                id,
                name,
                description,
                startPrice,
                rs.getString("seller_id")
        );
        item.setCurrentPrice(currentPrice);
        item.setStatus(status);
        item.setSellerId(rs.getString("seller_id"));
        item.setImagePath(rs.getString("image_path"));
        item.setHighestBidderId(rs.getString("highest_bidder_id"));
        item.setSellerUsername(findUsernameById(item.getSellerId()));
        item.setHighestBidderUsername(findUsernameById(item.getHighestBidderId()));
        item.setAuctionApproved(rs.getBoolean("auction_approved"));
        item.setStartTime(toLocalDateTime(rs.getTimestamp("start_time")));
        item.setEndTime(toLocalDateTime(rs.getTimestamp("end_time")));
        for (com.auction.system.model.auction.Bid bid : bidDAO.findByItemId(id)) {
            item.addBid(bid);
        }

     return item;
    }

    public boolean updateItem(Item item) {
        String sql = """
            UPDATE items
            SET name = ?, description = ?, image_path = ?, category = ?, start_price = ?, current_price = ?
            WHERE id = ?
            """;

        try (Connection conn = getConnection();
             PreparedStatement pstm = conn.prepareStatement(sql)) {

            // Nếu currentPrice <= 0 thì gán bằng startPrice
            double currentPrice = item.getCurrentPrice() > 0
                    ? item.getCurrentPrice()
                    : item.getStartPrice();

            // Gán các giá trị vào PreparedStatement
            pstm.setString(1, item.getName());
            pstm.setString(2, item.getDescription());
            pstm.setString(3, item.getImagePath());
            pstm.setString(4, item.getCategory());
            pstm.setBigDecimal(5, Money.toDatabaseAmount(item.getStartPrice()));
            pstm.setBigDecimal(6, Money.toDatabaseAmount(currentPrice));
            pstm.setString(7, item.getId());  // Sử dụng item ID để tìm item cần cập nhật

            // Thực thi câu SQL UPDATE
            return pstm.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Loi cap nhat item: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    public boolean deleteItem(String itemId) {
        if (itemId == null || itemId.trim().isEmpty()) return false;

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement s = conn.prepareStatement("DELETE FROM bids WHERE item_id = ?")) {
                    s.setString(1, itemId);
                    s.executeUpdate();
                }
                try (PreparedStatement s = conn.prepareStatement("DELETE FROM auctions WHERE item_id = ?")) {
                    s.setString(1, itemId);
                    s.executeUpdate();
                }
                try (PreparedStatement s = conn.prepareStatement("DELETE FROM items WHERE id = ?")) {
                    s.setString(1, itemId);
                    int rows = s.executeUpdate();
                    conn.commit();
                    return rows > 0;
                }
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Loi xoa item: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Loi xoa item: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
// cap nhap khi bat dau auction
    public boolean updateAuctionInfo(
            Connection conn,
            String itemId,
            AuctionStatus status,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) throws SQLException {
        String sql = """
                UPDATE items
                SET status = ?, start_time = ?, end_time = ?, auction_approved = TRUE
                WHERE id = ?
                """;

        try (PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setString(1, status.name());
            pstm.setTimestamp(2, toTimestamp(startTime));
            pstm.setTimestamp(3, toTimestamp(endTime));
            pstm.setString(4, itemId);

            return pstm.executeUpdate() > 0;
        }
    }

    public boolean requestAuctionApproval(
            Connection conn,
            String itemId,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) throws SQLException {
        String sql = """
                UPDATE items
                SET status = ?, start_time = ?, end_time = ?, auction_approved = FALSE
                WHERE id = ?
                """;

        try (PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setString(1, AuctionStatus.OPEN.name());
            pstm.setTimestamp(2, toTimestamp(startTime));
            pstm.setTimestamp(3, toTimestamp(endTime));
            pstm.setString(4, itemId);
            return pstm.executeUpdate() > 0;
        }
    }

    public boolean clearAuctionRequest(String itemId) {
        String sql = """
        UPDATE items
        SET status = ?,
            current_price = start_price,
            highest_bidder_id = NULL,
            start_time = NULL,
            end_time = NULL,
            auction_approved = FALSE
        WHERE id = ?
        """;

        try (Connection conn = getConnection();
             PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setString(1, AuctionStatus.OPEN.name());
            pstm.setString(2, itemId);
            return pstm.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Loi xoa yeu cau duyet phien dau gia: " + e.getMessage());
        }

        return false;
    }

    public boolean resetUnsoldAuction(Connection conn, String itemId) throws SQLException {
        // Ghi chu: phien khong co bid nao thi khong tinh la da ban.
        // Reset item ve OPEN/chua gui duyet de seller co the cap nhat va gui admin duyet lai.
        String sql = """
                UPDATE items
                SET status = ?,
                    current_price = start_price,
                    highest_bidder_id = NULL,
                    start_time = NULL,
                    end_time = NULL,
                    auction_approved = FALSE
                WHERE id = ?
                """;

        try (PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setString(1, AuctionStatus.OPEN.name());
            pstm.setString(2, itemId);
            return pstm.executeUpdate() > 0;
        }
    }

    public boolean updateAfterBid(
            Connection conn,
            String itemId,
            BigDecimal newPrice,
            String highestBidderId
    ) throws SQLException {
        String sql = """
            UPDATE items SET 
            current_price = ?, highest_bidder_id = ?
            WHERE id = ?
            AND current_price < ?
            AND status = 'RUNNING'
            AND end_time > NOW()
            """;

        try (PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setBigDecimal(1, newPrice);
            pstm.setString(2, highestBidderId);
            pstm.setString(3, itemId);
            pstm.setBigDecimal(4, newPrice);

            return pstm.executeUpdate() > 0;
        }
    }

    public boolean updateEndTime(Connection conn, String itemId, LocalDateTime endTime) throws SQLException {
        // Ghi chu: dung cho anti-sniping, chi keo dai thoi gian khi phien van dang RUNNING.
        String sql = """
                UPDATE items
                SET end_time = ?
                WHERE id = ?
                  AND status = 'RUNNING'
                """;

        try (PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setTimestamp(1, toTimestamp(endTime));
            pstm.setString(2, itemId);
            return pstm.executeUpdate() > 0;
        }
    }

    public boolean updateStatus(
            Connection conn,
            String itemId,
            AuctionStatus status
    ) throws SQLException {
        String sql = """
                UPDATE items
                SET status = ? 
                WHERE id = ?
                """;

        try(PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setString(1, status.name());
            pstm.setString(2, itemId);

            return pstm.executeUpdate() > 0;
        }
    }

    private String findUsernameById(String userId) {
        if(userId == null || userId.isBlank()) {
            return null;
        }

        User user = userDAO.findById(userId);
        return user == null ? null : user.getUserName();
    }




}
