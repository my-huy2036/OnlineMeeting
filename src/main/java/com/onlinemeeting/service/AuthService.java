
package com.onlinemeeting.service;

import com.onlinemeeting.database.DatabaseConnection;
import com.onlinemeeting.model.User;
import com.onlinemeeting.protocol.Response;
import org.mindrot.jbcrypt.BCrypt;

import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuthService {

    public Response login(String username, String password,
                          InetAddress remoteAddress) {
        if (username == null || username.isBlank()
                || password == null || password.isEmpty()) {
            return Response.error("Vui long nhap tai khoan va mat khau");
        }

        String sql = """
                SELECT UserId, Username, PasswordHash, DisplayName,
                       Email, UserRole, AccountStatus
                FROM dbo.Users
                WHERE Username = ?
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {
                User user;

                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, username);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            connection.rollback();
                            return Response.error(
                                    "Tai khoan hoac mat khau khong dung");
                        }

                        String hash = rs.getString("PasswordHash");
                        String status = rs.getString("AccountStatus");

                        boolean passwordMatches = false;

                        if (hash != null && hash.startsWith("$2")
                                && hash.length() == 60) {
                            try {
                                passwordMatches =
                                        BCrypt.checkpw(password, hash);
                            } catch (IllegalArgumentException ignored) {
                                passwordMatches = false;
                            }
                        }

                        if (!passwordMatches) {
                            connection.rollback();
                            return Response.error(
                                    "Tai khoan hoac mat khau khong dung");
                        }

                        if (!"active".equals(status)) {
                            connection.rollback();
                            return Response.error(
                                    "Tai khoan khong hoat dong");
                        }

                        user = new User(
                                rs.getInt("UserId"),
                                rs.getString("Username"),
                                rs.getString("DisplayName"),
                                rs.getString("Email"),
                                rs.getString("UserRole"),
                                status
                        );
                    }
                }

                try (PreparedStatement ps = connection.prepareStatement(
                        "UPDATE dbo.Users SET LastLoginAt = SYSDATETIME() "
                                + "WHERE UserId = ?")) {
                    ps.setInt(1, user.userId);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = connection.prepareStatement(
                        """
                        INSERT INTO dbo.UserSessions
                            (UserId, LocalIP, Protocol, SessionStatus)
                        VALUES (?, ?, N'TCP', N'active')
                        """)) {
                    ps.setInt(1, user.userId);
                    ps.setString(2, remoteAddress == null
                            ? null : remoteAddress.getHostAddress());
                    ps.executeUpdate();
                }

                connection.commit();
                return Response.ok("Dang nhap thanh cong", user);

            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }

        } catch (SQLException | IllegalStateException e) {
            System.err.println("Login error: " + e.getMessage());
            return Response.error("Loi he thong khi dang nhap");
        }
    }
    
    //register


public Response register(String username, String password, String displayName, String email) {
    if (username == null || !username.trim().matches("[A-Za-z0-9_.-]{3,50}")) {
        return Response.error("Tai khoan phai co 3-50 ky tu, chi gom chu, so, _, ., -");
    }
    if (password == null || password.length() < 8 || password.length() > 72) {
        return Response.error("Mat khau phai co tu 8 den 72 ky tu");
    }
    if (displayName == null || displayName.isBlank() || displayName.trim().length() > 100) {
        return Response.error("Vui long nhap ten hien thi hop le");
    }
    String normalizedEmail = email == null || email.isBlank() ? null : email.trim();
    if (normalizedEmail != null && (normalizedEmail.length() > 254 || !normalizedEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) {
        return Response.error("Email khong hop le");
    }
    String sql = """
            INSERT INTO dbo.Users (Username, PasswordHash, DisplayName, Email, UserRole, AccountStatus)
            VALUES (?, ?, ?, ?, N'user', N'active')
            """;
    try (Connection connection = DatabaseConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {
        ps.setString(1, username.trim());
        ps.setString(2, BCrypt.hashpw(password, BCrypt.gensalt(12)));
        ps.setString(3, displayName.trim());
        ps.setString(4, normalizedEmail);
        ps.executeUpdate();
        return Response.ok("Dang ky tai khoan thanh cong", null);
    } catch (SQLException e) {
        if (e.getErrorCode() == 2601 || e.getErrorCode() == 2627) {
            return Response.error("Tai khoan hoac email da ton tai");
        }
        System.err.println("Register error: " + e.getMessage());
        return Response.error("Loi he thong khi dang ky");
    } catch (IllegalStateException e) {
        System.err.println("Register configuration error: " + e.getMessage());
        return Response.error("Server chua cau hinh ket noi database");
    }
}



}
