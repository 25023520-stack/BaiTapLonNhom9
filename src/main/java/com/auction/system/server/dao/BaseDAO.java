package com.auction.system.server.dao;

import com.auction.system.server.database.Database;

import java.sql.Connection;
import  java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public abstract class BaseDAO {
    protected Connection getConnection() throws SQLException {
        return Database.getInstance().getConnection();
    }

    protected Timestamp toTimestamp(LocalDateTime value) { //trả về null nếu không có thời gian thực
        return value == null ? null : Timestamp.valueOf(value);
    }

    protected LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }
}
