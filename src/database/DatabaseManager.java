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
            System.out.println("Подключение к базе данных установлено: " + dbName);
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC драйвер не найден!");
            throw e;
        } catch (SQLException e) {
            System.err.println("Ошибка подключения к базе данных: " + e.getMessage());
            throw e;
        }
    }

    public void createTables() {
        try (Statement stmt = connection.createStatement()) {
            System.out.println("Создание таблиц в базе данных...");

            stmt.execute("DROP TABLE IF EXISTS earthquakes");
            stmt.execute("DROP TABLE IF EXISTS regions");

            // Создаем таблицу регионов
            stmt.execute("CREATE TABLE IF NOT EXISTS regions (" +
                    "region_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT UNIQUE NOT NULL," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // Создаем таблицу землетрясений
            stmt.execute("CREATE TABLE IF NOT EXISTS earthquakes (" +
                    "earthquake_id TEXT PRIMARY KEY," +
                    "region_id INTEGER," +
                    "magnitude REAL NOT NULL," +
                    "depth REAL DEFAULT 0.0," +
                    "magnitude_type TEXT," +
                    "time TIMESTAMP," +
                    "state TEXT," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (region_id) REFERENCES regions(region_id))");

            // Создаем индексы для ускорения запросов
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_time ON earthquakes(time)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_magnitude ON earthquakes(magnitude)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_depth ON earthquakes(depth)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_region ON earthquakes(region_id)");

            System.out.println("Таблицы успешно созданы");

        } catch (SQLException e) {
            System.err.println("Ошибка создания таблиц: " + e.getMessage());
        }
    }

    public void saveEarthquakes(List<Earthquake> earthquakes) {
        if (earthquakes == null || earthquakes.isEmpty()) {
            System.out.println("Нет данных для сохранения");
            return;
        }

        try {
            System.out.println("Сохранение " + earthquakes.size() + " землетрясений в базу данных...");
            connection.setAutoCommit(false);

            int regionCount = 0;
            int earthquakeCount = 0;
            int timeCount = 0;

            // Вставляем регионы
            String regionSql = "INSERT OR IGNORE INTO regions (name) VALUES (?)";
            try (PreparedStatement pstmt = connection.prepareStatement(regionSql)) {
                for (Earthquake eq : earthquakes) {
                    if (eq.getState() != null && !eq.getState().isEmpty()) {
                        pstmt.setString(1, eq.getState());
                        pstmt.addBatch();
                        regionCount++;
                    }
                }
                if (regionCount > 0) {
                    pstmt.executeBatch();
                    System.out.println("Добавлено регионов: " + regionCount);
                }
            }

            // Вставляем землетрясения
            String eqSql = "INSERT OR REPLACE INTO earthquakes " +
                    "(earthquake_id, region_id, magnitude, depth, magnitude_type, time, state) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement pstmt = connection.prepareStatement(eqSql)) {
                for (Earthquake eq : earthquakes) {
                    int regionId = getRegionId(eq.getState());

                    pstmt.setString(1, eq.getId());
                    pstmt.setInt(2, regionId);
                    pstmt.setDouble(3, eq.getMagnitude());
                    pstmt.setDouble(4, eq.getDepth());
                    pstmt.setString(5, eq.getMagnitudeType());

                    if (eq.getTime() != null) {
                        pstmt.setTimestamp(6, Timestamp.valueOf(eq.getTime()));
                        timeCount++;
                    } else {
                        pstmt.setNull(6, Types.TIMESTAMP);
                    }

                    pstmt.setString(7, eq.getState());
                    pstmt.addBatch();
                    earthquakeCount++;
                }

                pstmt.executeBatch();
            }

            connection.commit();
            connection.setAutoCommit(true);

            System.out.println("Сохранение завершено:");
            System.out.println("  Землетрясений: " + earthquakeCount);
            System.out.println("  С временем: " + timeCount);
            System.out.println("  Без времени: " + (earthquakeCount - timeCount));

        } catch (SQLException e) {
            System.err.println("Ошибка сохранения данных: " + e.getMessage());
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException ex) {
                System.err.println("Ошибка отката транзакции: " + ex.getMessage());
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

    public Connection getConnection() {
        return connection;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Соединение с базой данных закрыто");
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при закрытии соединения: " + e.getMessage());
        }
    }
}