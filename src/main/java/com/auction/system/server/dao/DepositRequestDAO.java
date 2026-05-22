package com.auction.system.server.dao;

import com.auction.system.model.user.DepositRequest;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class DepositRequestDAO extends BaseDAO {
    // DAO tach rieng cho yeu cau nap tien de giu dung mo hinh MVC/DAO:
    // Controller nhan payload, Manager xu ly nghiep vu, DAO chi lam viec voi database.
    public boolean insert(DepositRequest request) {
        String sql = """
                INSERT INTO deposit_requests (id, bidder_id, amount, status)
                VALUES (?, ?, ?, ?)
                """;
        try (Connection conn = getConnection();
             PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setString(1, request.getId());
            pstm.setString(2, request.getBidderId());
            pstm.setBigDecimal(3, BigDecimal.valueOf(request.getAmount()));
            pstm.setString(4, request.getStatus());
            return pstm.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Loi tao yeu cau nap tien: " + e.getMessage());
        }
        return false;
    }

    public List<DepositRequest> findPending() {
        String sql = """
                SELECT dr.*, u.full_name AS bidder_name
                FROM deposit_requests dr
                JOIN users u ON u.id = dr.bidder_id
                WHERE dr.status = 'PENDING'
                ORDER BY dr.created_at ASC
                """;
        List<DepositRequest> requests = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement pstm = conn.prepareStatement(sql);
             ResultSet rs = pstm.executeQuery()) {
            while (rs.next()) {
                requests.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("Loi lay danh sach yeu cau nap tien: " + e.getMessage());
        }
        return requests;
    }

    public DepositRequest findByIdForUpdate(Connection conn, String id) throws SQLException {
        // FOR UPDATE khoa dong request trong transaction duyet tien.
        // Muc dich: tranh truong hop hai admin cung bam duyet lam cong tien hai lan.
        String sql = """
                SELECT dr.*, u.full_name AS bidder_name
                FROM deposit_requests dr
                JOIN users u ON u.id = dr.bidder_id
                WHERE dr.id = ?
                FOR UPDATE
                """;
        try (PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setString(1, id);
            try (ResultSet rs = pstm.executeQuery()) {
                return rs.next() ? mapResultSet(rs) : null;
            }
        }
    }

    public boolean markApproved(Connection conn, String id, String adminId) throws SQLException {
        // Chi cap nhat request dang PENDING. Neu request da duyet roi thi executeUpdate = 0.
        String sql = """
                UPDATE deposit_requests
                SET status = 'APPROVED', reviewed_at = CURRENT_TIMESTAMP, reviewed_by = ?
                WHERE id = ? AND status = 'PENDING'
                """;
        try (PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setString(1, adminId);
            pstm.setString(2, id);
            return pstm.executeUpdate() > 0;
        }
    }

    private DepositRequest mapResultSet(ResultSet rs) throws SQLException {
        BigDecimal amount = rs.getBigDecimal("amount");
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp reviewedAt = rs.getTimestamp("reviewed_at");
        return new DepositRequest(
                rs.getString("id"),
                rs.getString("bidder_id"),
                rs.getString("bidder_name"),
                amount != null ? amount.doubleValue() : 0,
                rs.getString("status"),
                createdAt != null ? createdAt.toLocalDateTime() : null,
                reviewedAt != null ? reviewedAt.toLocalDateTime() : null,
                rs.getString("reviewed_by")
        );
    }
}
