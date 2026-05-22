package com.auction.system.server.dao;

import com.auction.system.model.user.*;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO extends BaseDAO {

    // kiểm tra tài khoản tồn tại chưa
    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?"; // đếm số dòng thỏa mãn
        try (Connection conn = getConnection();
             PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setString(1, username);

            try (ResultSet rs = pstm.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            System.err.println("Lỗi kiểm tra username: " + e.getMessage());
        }

        return false;
    }

    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setString(1, email);

            try (ResultSet rs = pstm.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            System.err.println("Loi kiem tra email: " + e.getMessage());
        }

        return false;
    }

    //thêm user mới vào DB
    public String insertUser(User user) {
        String sql = """
                INSERT INTO users (id, full_name, username, email, password, role, approved, balance)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = getConnection();
             PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setString(1, user.getId());
            pstm.setString(2, user.getFullName());
            pstm.setString(3, user.getUserName());
            pstm.setString(4, user.getEmail());
            pstm.setString(5, user.getPassWord());
            pstm.setString(6, user.getRole());
            pstm.setBoolean(7, user.isApproved());
            pstm.setBigDecimal(8, BigDecimal.valueOf(user.getBalance()));

            int rowsAffected = pstm.executeUpdate();
            return rowsAffected > 0 ? user.getId() : null;
        } catch (SQLException e) {
            System.err.println("Lỗi thêm user: " + e.getMessage());
        }
        return null;

    }

    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setString(1, username);

            try (ResultSet rs = pstm.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("Lỗi tìm users: " + e.getMessage());
        }

        return null;
    }

    public User findById(String id) {
        String sql = "SELECT * FROM users WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setString(1, id);

            try (ResultSet rs = pstm.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("Lỗi tìm users: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    public List<User> findAll() {
        String sql = "SELECT * FROM users";

        List<User> users = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement pstm = conn.prepareStatement(sql);
             ResultSet rs = pstm.executeQuery()) {

             while (rs.next()) {
                User user = mapResultSetToUser(rs);
                users.add(user);
            }
        } catch (SQLException e) {
            System.err.println("Loi lay danh sach users: " + e.getMessage());
        }
        return users;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String username = rs.getString("username");
        String fullname = rs.getString("full_name");
        String email = rs.getString("email");
        String password = rs.getString("password");
        String role = rs.getString("role");
        boolean approved = rs.getBoolean("approved");
        BigDecimal balance = rs.getBigDecimal("balance");

        User user = switch (role.toUpperCase()) {
            case "ADMIN" -> new Admin(id, fullname, username, email, password);
            case "SELLER" -> new Seller(id, fullname, username, email, password);
            case "BIDDER" -> new Bidder(id, fullname, username, email, password);
            default -> throw new SQLException("role khong hop le: " + role);
        };
        user.setApproved(approved);
        user.setBalance(balance != null ? balance.doubleValue() : 0);
        return user;
    }

    public boolean updateUser(User user) {
        String sql = """
                UPDATE users SET full_name= ?, email = ?, password = ? WHERE id = ?
                """;
        try (Connection conn = getConnection();
             PreparedStatement pstm = conn.prepareStatement(sql)) {

            pstm.setString(1, user.getFullName());
            pstm.setString(2, user.getEmail());
            pstm.setString(3, user.getPassWord());
            pstm.setString(4, user.getId());

            int rowsAffected = pstm.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi khi updateUser: " + e.getMessage());
        }
        return false;

    }

    public boolean deleteUser(String id) {
        String sql = "DELETE FROM users WHERE id = ? ";
        try (Connection conn = getConnection();
             PreparedStatement pstm = conn.prepareStatement(sql)) {

             pstm.setString(1, id);

            int rowsAffected = pstm.executeUpdate();
            return rowsAffected > 0;

        } catch(Exception e) {
            System.err.println("Lỗi xóa users: " + e.getMessage());
        }

        return false;
    }

    public boolean updateApproval(String id, boolean approved) {
        String sql = "UPDATE users SET approved = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setBoolean(1, approved);
            pstm.setString(2, id);
            return pstm.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Loi cap nhat trang thai duyet user: " + e.getMessage());
        }
        return false;
    }

    public boolean addBalance(Connection conn, String userId, double amount) throws SQLException {
        String sql = "UPDATE users SET balance = balance + ? WHERE id = ? AND role = 'BIDDER'";
        try (PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setBigDecimal(1, BigDecimal.valueOf(amount));
            pstm.setString(2, userId);
            return pstm.executeUpdate() > 0;
        }
    }
}
