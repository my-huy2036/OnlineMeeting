
package com.onlinemeeting.service;

import com.onlinemeeting.database.DatabaseConnection;
import com.onlinemeeting.protocol.Response;
import java.security.SecureRandom;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

public class MeetingService {
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public Response createMeeting(int userId, String title, String description, String scheduledStartAt) {
        if (userId <= 0) return Response.error("Tai khoan khong hop le");
        if (title == null || title.isBlank() || title.trim().length() > 150) return Response.error("Ten cuoc hop phai tu 1 den 150 ky tu");
        if (description != null && description.length() > 1000) return Response.error("Mo ta khong duoc vuot qua 1000 ky tu");

        Timestamp scheduled = null;
        if (scheduledStartAt != null && !scheduledStartAt.isBlank()) {
            try {
                String value = scheduledStartAt.trim().replace('T', ' ');
                if (value.length() == 16) value += ":00";
                scheduled = Timestamp.valueOf(value);
            } catch (IllegalArgumentException e) {
                return Response.error("Thoi gian khong hop le. Dung yyyy-MM-dd HH:mm hoac yyyy-MM-dd HH:mm:ss");
            }
        }

        String status = scheduled != null && scheduled.toLocalDateTime().isAfter(LocalDateTime.now()) ? "scheduled" : "ongoing";
        String sql = "INSERT INTO dbo.Meetings (MeetingCode, Title, Description, HostUserId, ScheduledAt, MeetingStatus, StartedAt) VALUES (?, ?, ?, ?, ?, ?, CASE WHEN ? = N'ongoing' THEN SYSDATETIME() ELSE NULL END)";

        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                for (int attempt = 0; attempt < 5; attempt++) {
                    String code = generateCode();
                    try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                        ps.setString(1, code);
                        ps.setString(2, title.trim());
                        ps.setString(3, description == null || description.isBlank() ? null : description.trim());
                        ps.setInt(4, userId);
                        ps.setTimestamp(5, scheduled);
                        ps.setString(6, status);
                        ps.setString(7, status);
                        ps.executeUpdate();

                        int meetingId;
                        try (ResultSet keys = ps.getGeneratedKeys()) {
                            if (!keys.next()) throw new SQLException("Khong lay duoc MeetingId");
                            meetingId = keys.getInt(1);
                        }

                        insertHostParticipant(c, meetingId, userId);
                        c.commit();

                        Map<String, Object> data = new LinkedHashMap<>();
                        data.put("meetingId", meetingId);
                        data.put("meetingCode", code);
                        data.put("title", title.trim());
                        data.put("meetingStatus", status);
                        data.put("scheduledAt", scheduled == null ? null : scheduled.toString());
                        return Response.ok("Tao cuoc hop thanh cong", data);
                    } catch (SQLException e) {
                        if (e.getErrorCode() == 2601 || e.getErrorCode() == 2627) {
                            c.rollback();
                            continue;
                        }
                        throw e;
                    }
                }
                c.rollback();
                return Response.error("Khong tao duoc ma phong, vui long thu lai");
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Create meeting error: " + e.getMessage());
            return Response.error("Loi he thong khi tao cuoc hop");
        }
    }

    public Response joinMeetingByCode(int userId, String meetingCode) {
        if (userId <= 0) return Response.error("Tai khoan khong hop le");
        if (meetingCode == null || !meetingCode.trim().matches("[A-Za-z0-9]{6,30}")) return Response.error("Ma cuoc hop khong hop le");

        String sql = "SELECT MeetingId, MeetingCode, Title, Description, HostUserId, ScheduledAt, MeetingStatus, MaxParticipants FROM dbo.Meetings WHERE MeetingCode = ?";
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, meetingCode.trim().toUpperCase(Locale.ROOT));
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        c.rollback();
                        return Response.error("Khong tim thay cuoc hop");
                    }

                    int meetingId = rs.getInt("MeetingId");
                    int hostUserId = rs.getInt("HostUserId");
                    String status = rs.getString("MeetingStatus");
                    int maxParticipants = rs.getInt("MaxParticipants");
                    String title = rs.getString("Title");
                    String description = rs.getString("Description");
                    String code = rs.getString("MeetingCode");
                    Timestamp scheduled = rs.getTimestamp("ScheduledAt");

                    if (!"scheduled".equalsIgnoreCase(status) && !"ongoing".equalsIgnoreCase(status)) {
                        c.rollback();
                        return Response.error("Cuoc hop da ket thuc hoac da bi huy");
                    }

                    if (!isUserActive(c, userId)) {
                        c.rollback();
                        return Response.error("Tai khoan khong ton tai hoac khong hoat dong");
                    }

                    if (!isAlreadyJoined(c, meetingId, userId) && countJoined(c, meetingId) >= maxParticipants) {
                        c.rollback();
                        return Response.error("Cuoc hop da du so luong thanh vien");
                    }

                    upsertParticipant(c, meetingId, userId, hostUserId);
                    c.commit();

                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("meetingId", meetingId);
                    data.put("meetingCode", code);
                    data.put("title", title);
                    data.put("description", description);
                    data.put("hostUserId", hostUserId);
                    data.put("meetingStatus", status);
                    data.put("scheduledAt", scheduled == null ? null : scheduled.toString());
                    return Response.ok("Tham gia cuoc hop thanh cong", data);
                }
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Join meeting error: " + e.getMessage());
            return Response.error("Loi he thong khi tham gia cuoc hop");
        }
    }

    public Response leaveMeeting(int userId, String meetingCode) {
        if (userId <= 0 || meetingCode == null || meetingCode.isBlank()) return Response.error("Thong tin phong hop khong hop le");

        String sql = "UPDATE p SET p.ParticipantStatus = N'left', p.LeftAt = SYSDATETIME() FROM dbo.MeetingParticipants p JOIN dbo.Meetings m ON m.MeetingId = p.MeetingId WHERE p.UserId = ? AND m.MeetingCode = ? AND p.ParticipantStatus = N'joined'";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, meetingCode.trim().toUpperCase(Locale.ROOT));
            int changed = ps.executeUpdate();
            return changed > 0 ? Response.ok("Da roi khoi cuoc hop", null) : Response.error("Ban khong o trong cuoc hop nay");
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Leave meeting error: " + e.getMessage());
            return Response.error("Loi he thong khi roi cuoc hop");
        }
    }

    public Response listParticipants(String meetingCode) {
        if (meetingCode == null || meetingCode.isBlank()) return Response.error("Ma cuoc hop khong hop le");

        String sql = "SELECT u.UserId, u.Username, u.DisplayName, p.ParticipantRole, p.MicEnabled, p.CameraEnabled, p.JoinedAt FROM dbo.MeetingParticipants p JOIN dbo.Meetings m ON m.MeetingId = p.MeetingId JOIN dbo.Users u ON u.UserId = p.UserId WHERE m.MeetingCode = ? AND p.ParticipantStatus = N'joined' ORDER BY p.JoinedAt";
        List<Map<String, Object>> participants = new ArrayList<>();

        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, meetingCode.trim().toUpperCase(Locale.ROOT));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("userId", rs.getInt("UserId"));
                    item.put("username", rs.getString("Username"));
                    item.put("displayName", rs.getString("DisplayName"));
                    item.put("role", rs.getString("ParticipantRole"));
                    item.put("micEnabled", rs.getBoolean("MicEnabled"));
                    item.put("cameraEnabled", rs.getBoolean("CameraEnabled"));
                    item.put("joinedAt", rs.getTimestamp("JoinedAt").toString());
                    participants.add(item);
                }
            }
            return Response.ok("Lay danh sach thanh vien thanh cong", participants);
        } catch (SQLException | IllegalStateException e) {
            System.err.println("List participants error: " + e.getMessage());
            return Response.error("Loi he thong khi lay danh sach thanh vien");
        }
    }

    private void insertHostParticipant(Connection c, int meetingId, int userId) throws SQLException {
        String sql = "INSERT INTO dbo.MeetingParticipants (MeetingId, UserId, ParticipantRole, ParticipantStatus) VALUES (?, ?, N'host', N'joined')";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, meetingId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    private void upsertParticipant(Connection c, int meetingId, int userId, int hostUserId) throws SQLException {
        String update = "UPDATE dbo.MeetingParticipants SET ParticipantRole = ?, ParticipantStatus = N'joined', JoinedAt = SYSDATETIME(), LeftAt = NULL, IsRemovedByHost = 0 WHERE MeetingId = ? AND UserId = ? AND ParticipantStatus <> N'banned'";
        try (PreparedStatement ps = c.prepareStatement(update)) {
            ps.setString(1, userId == hostUserId ? "host" : "participant");
            ps.setInt(2, meetingId);
            ps.setInt(3, userId);
            if (ps.executeUpdate() > 0) return;
        }

        String insert = "INSERT INTO dbo.MeetingParticipants (MeetingId, UserId, ParticipantRole, ParticipantStatus) VALUES (?, ?, ?, N'joined')";
        try (PreparedStatement ps = c.prepareStatement(insert)) {
            ps.setInt(1, meetingId);
            ps.setInt(2, userId);
            ps.setString(3, userId == hostUserId ? "host" : "participant");
            ps.executeUpdate();
        }
    }

    private boolean isUserActive(Connection c, int userId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM dbo.Users WHERE UserId = ? AND AccountStatus = N'active'")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean isAlreadyJoined(Connection c, int meetingId, int userId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM dbo.MeetingParticipants WHERE MeetingId = ? AND UserId = ? AND ParticipantStatus = N'joined'")) {
            ps.setInt(1, meetingId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private int countJoined(Connection c, int meetingId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM dbo.MeetingParticipants WHERE MeetingId = ? AND ParticipantStatus = N'joined'")) {
            ps.setInt(1, meetingId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private String generateCode() {
        StringBuilder code = new StringBuilder(8);
        for (int i = 0; i < 8; i++) code.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        return code.toString();
    }
}
