package com.auction.system.server.dao;

import com.auction.system.server.database.Database;
import com.auction.system.model.user.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    //lấy kết nối từ Database singleton
    private Connection getConnection() throws SQLException {
        return Database.getInstance().getConnection();
    }

    // kiểm tra tài khoản tồn tại chưa
    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?"; // đếm số dòng thỏa mãn
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, username);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Lỗi kiểm tra username: " + e.getMessage());
        }
        return false;
    }

    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, email);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Lá»—i kiá»ƒm tra email: " + e.getMessage());
        }
        return false;
    }

    //thêm user mới vào DB
    public int insertUser(User user) {
        String sql = """
                INSERT INTO users (full_name, username, email, password, role)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement stmt = getConnection().prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getFullName());
            stmt.setString(2, user.getUserName());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getPassWord());
            stmt.setString(5, user.getRole());

            int rowsAffected = stmt.executeUpdate();
            ResultSet generatedKeys = stmt.getGeneratedKeys();
            if (rowsAffected > 0) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi thêm user: " + e.getMessage());
        }
        return -1;

    }

    public User findByUsername() {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            System.err.println("Lỗi tìm users: " + e.getMessage());
        }
        return null;
    }

    public User findById() {
        String sql = "SELECT * FROM users WHERE id = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            System.err.println("Lỗi tìm users: " + e.getMessage());
        }
        return null;
    }

    public List<User> findAll() {
        String sql = "SELECT * FROM users";

        List<User> users = new ArrayList<>();

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                User user = mapResultSetToUser(rs);
                users.add(user);
            }
        } catch (SQLException e) {
            System.err.println("Lỗi tìm users: " + e.getMessage());
        }
        return null;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String username = rs.getString("username");
        String fullname = rs.getString("full_name");
        String email = rs.getString("email");
        String password = rs.getString("password");
        String role = rs.getString("role");

        return switch (role.toUpperCase()) {
            case "ADMIN" -> new Admin(id, fullname, username, email, password);
            case "SELLER" -> new Seller(id, fullname, username, email, password);
            case "BIDDER" -> new Bidder(id, fullname, username, email, password);
            default -> throw new SQLException("role khong hop le: " + role);
        };
    }

    public boolean updateUser(User user) {
        String sql = """
                UPDATE users SET full_name= ?, email = ?, password = ? WHERE id = ?
                """;
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, user.getFullName());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPassWord());
            stmt.setInt(4, user.getId());

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (Exception e) {
            System.err.println("Lỗi khi updateUser: " + e.getMessage());
        }
        return false;

    }

    public boolean deleteUser(int id) {
        String sql = "DELETE FROM users WHERE id = ? ";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch(Exception e) {
            System.err.println("Lỗi xóa users: " + e.getMessage());
        } return false;
    }
}

