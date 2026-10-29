
package com.onlinemeeting.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        String host = System.getenv().getOrDefault(
                "OM_DB_HOST", "localhost");
        String port = System.getenv().getOrDefault(
                "OM_DB_PORT", "1433");
        String database = System.getenv().getOrDefault(
                "OM_DB_NAME", "OnlineMeeting");
        String username = System.getenv("OM_DB_USER");
        String password = System.getenv("OM_DB_PASSWORD");

        if (username == null || password == null) {
            throw new IllegalStateException(
                    "Chua cau hinh OM_DB_USER va OM_DB_PASSWORD");
        }

        String url = "jdbc:sqlserver://" + host + ":" + port
                + ";databaseName=" + database
                + ";encrypt=true"
                + ";trustServerCertificate=true"
                + ";loginTimeout=5";

        return DriverManager.getConnection(url, username, password);
    }

    public static boolean testConnection() {
        try (Connection connection = getConnection()) {
            return connection.isValid(5);
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Database connection failed: "
                    + e.getMessage());
            return false;
        }
    }
}
