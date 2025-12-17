package database;

import models.Earthquake;
import java.sql.*;
import java.util.List;

public class DatabaseManager {
    private Connection connection;

    public DatabaseManager(String dbName) throws Exception {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbName);
        } catch (ClassNotFoundException e) {
            throw e;
        }
    }

    public void createTables() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS earthquakes");
            stmt.execute("DROP TABLE IF EXISTS regions");

            stmt.execute("CREATE TABLE regions (" +
                    "region_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT UNIQUE NOT NULL)");

            stmt.execute("CREATE TABLE earthquakes (" +
                    "earthquake_id TEXT PRIMARY KEY," +
                    "region_id INTEGER," +
                    "magnitude REAL NOT NULL," +
                    "depth REAL," +
                    "time TIMESTAMP)");

        } catch (SQLException e) {
            // Тихий провал
        }
    }

    public void saveEarthquakes(List<Earthquake> earthquakes) {
        try {
            connection.setAutoCommit(false);

            // Вставляем регионы
            String regionSql = "INSERT OR IGNORE INTO regions (name) VALUES (?)";
            try (PreparedStatement pstmt = connection.prepareStatement(regionSql)) {
                for (Earthquake eq : earthquakes) {
                    if (eq.getState() != null && !eq.getState().isEmpty()) {
                        pstmt.setString(1, eq.getState());
                        pstmt.addBatch();
                    }
                }
                pstmt.executeBatch();
            }

            // Вставляем землетрясения
            String eqSql = "INSERT OR REPLACE INTO earthquakes VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(eqSql)) {
                for (Earthquake eq : earthquakes) {
                    int regionId = getRegionId(eq.getState());

                    pstmt.setString(1, eq.getId());
                    pstmt.setInt(2, regionId);
                    pstmt.setDouble(3, eq.getMagnitude());
                    pstmt.setDouble(4, eq.getDepth());

                    if (eq.getTime() != null) {
                        pstmt.setTimestamp(5, Timestamp.valueOf(eq.getTime()));
                    } else {
                        pstmt.setNull(5, Types.TIMESTAMP);
                    }

                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }

            connection.commit();
            connection.setAutoCommit(true);

        } catch (SQLException e) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException ex) {
                // Тихий провал
            }
        }
    }

    private int getRegionId(String regionName) throws SQLException {
        if (regionName == null || regionName.trim().isEmpty()) {
            return 0;
        }

        String sql = "SELECT region_id FROM regions WHERE name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, regionName.trim());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("region_id");
            }
        }
        return 0;
    }

    public ResultSet executeQuery(String sql) throws SQLException {
        Statement stmt = connection.createStatement();
        return stmt.executeQuery(sql);
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            // Тихий провал
        }
    }
}