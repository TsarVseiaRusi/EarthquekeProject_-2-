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
            System.out.println("✓ База данных подключена: " + dbName);
        } catch (ClassNotFoundException e) {
            System.err.println("✗ Драйвер SQLite не найден!");
            throw e;
        }
    }

    public void createTables() {
        try (Statement stmt = connection.createStatement()) {
            System.out.println("Создание таблиц...");

            // Сначала удаляем старые таблицы, если есть
            stmt.execute("DROP TABLE IF EXISTS earthquakes");
            stmt.execute("DROP TABLE IF EXISTS regions");
            stmt.execute("DROP TABLE IF EXISTS magnitude_types");

            // Создаем таблицу регионов
            String regionsTable = "CREATE TABLE regions (" +
                    "region_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT UNIQUE NOT NULL)";
            stmt.execute(regionsTable);
            System.out.println("✓ Таблица 'regions' создана");

            // Создаем таблицу типов магнитуд
            String typesTable = "CREATE TABLE magnitude_types (" +
                    "type_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT UNIQUE NOT NULL)";
            stmt.execute(typesTable);
            System.out.println("✓ Таблица 'magnitude_types' создана");

            // Создаем основную таблицу землетрясений
            String earthquakesTable = "CREATE TABLE earthquakes (" +
                    "earthquake_id TEXT PRIMARY KEY," +
                    "region_id INTEGER," +
                    "magnitude_type_id INTEGER," +
                    "magnitude REAL NOT NULL," +
                    "depth REAL," +
                    "time TIMESTAMP," +
                    "FOREIGN KEY (region_id) REFERENCES regions(region_id)," +
                    "FOREIGN KEY (magnitude_type_id) REFERENCES magnitude_types(type_id))";
            stmt.execute(earthquakesTable);
            System.out.println("✓ Таблица 'earthquakes' создана");

        } catch (SQLException e) {
            System.err.println("✗ Ошибка создания таблиц: " + e.getMessage());
        }
    }

    public void saveEarthquakes(List<Earthquake> earthquakes) {
        System.out.println("Сохранение данных в БД...");

        try {
            // Начинаем транзакцию
            connection.setAutoCommit(false);

            // Вставляем уникальные регионы
            insertRegions(earthquakes);

            // Вставляем уникальные типы магнитуд
            insertMagnitudeTypes(earthquakes);

            // Вставляем землетрясения
            insertEarthquakes(earthquakes);

            // Фиксируем транзакцию
            connection.commit();
            connection.setAutoCommit(true);

            System.out.println("✓ Сохранено " + earthquakes.size() + " землетрясений в БД");

        } catch (SQLException e) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException ex) {
                System.err.println("Ошибка отката: " + ex.getMessage());
            }
            System.err.println("✗ Ошибка сохранения: " + e.getMessage());
        }
    }

    private void insertRegions(List<Earthquake> earthquakes) throws SQLException {
        // Собираем уникальные регионы
        java.util.Set<String> uniqueRegions = new java.util.HashSet<>();
        for (Earthquake eq : earthquakes) {
            if (eq.getState() != null && !eq.getState().trim().isEmpty()) {
                uniqueRegions.add(eq.getState().trim());
            }
        }

        // Вставляем регионы
        String sql = "INSERT OR IGNORE INTO regions (name) VALUES (?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (String region : uniqueRegions) {
                pstmt.setString(1, region);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
        System.out.println("✓ Добавлено " + uniqueRegions.size() + " уникальных регионов");
    }

    private void insertMagnitudeTypes(List<Earthquake> earthquakes) throws SQLException {
        // Собираем уникальные типы магнитуд
        java.util.Set<String> uniqueTypes = new java.util.HashSet<>();
        for (Earthquake eq : earthquakes) {
            if (eq.getMagnitudeType() != null && !eq.getMagnitudeType().trim().isEmpty()) {
                uniqueTypes.add(eq.getMagnitudeType().trim());
            }
        }

        // Вставляем типы
        String sql = "INSERT OR IGNORE INTO magnitude_types (name) VALUES (?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (String type : uniqueTypes) {
                pstmt.setString(1, type);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
        System.out.println("✓ Добавлено " + uniqueTypes.size() + " уникальных типов магнитуд");
    }

    private void insertEarthquakes(List<Earthquake> earthquakes) throws SQLException {
        String sql = "INSERT OR REPLACE INTO earthquakes " +
                "(earthquake_id, region_id, magnitude_type_id, magnitude, depth, time) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            int batchSize = 0;

            for (Earthquake eq : earthquakes) {
                // Получаем ID региона
                int regionId = getRegionId(eq.getState());

                // Получаем ID типа магнитуды
                int typeId = getMagnitudeTypeId(eq.getMagnitudeType());

                pstmt.setString(1, eq.getId());
                pstmt.setInt(2, regionId);
                pstmt.setInt(3, typeId);
                pstmt.setDouble(4, eq.getMagnitude());
                pstmt.setDouble(5, eq.getDepth());

                if (eq.getTime() != null) {
                    pstmt.setTimestamp(6, Timestamp.valueOf(eq.getTime()));
                } else {
                    pstmt.setNull(6, Types.TIMESTAMP);
                }

                pstmt.addBatch();
                batchSize++;

                if (batchSize % 100 == 0) {
                    pstmt.executeBatch();
                }
            }

            pstmt.executeBatch();
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

    private int getMagnitudeTypeId(String typeName) throws SQLException {
        if (typeName == null || typeName.trim().isEmpty()) {
            return 0;
        }

        String sql = "SELECT type_id FROM magnitude_types WHERE name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, typeName.trim());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("type_id");
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
                System.out.println("✓ Соединение с БД закрыто");
            }
        } catch (SQLException e) {
            System.err.println("✗ Ошибка закрытия соединения: " + e.getMessage());
        }
    }
}