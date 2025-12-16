
package database;

import java.sql.*;

public class SQLQueries {
    private DatabaseManager dbManager;

    public SQLQueries(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    // 1. Землетрясения с магнитудой больше 4.0
    public void getStrongEarthquakes() {
        String sql = "SELECT e.earthquake_id, r.name as region, e.magnitude, e.time " +
                "FROM earthquakes e " +
                "JOIN regions r ON e.region_id = r.region_id " +
                "WHERE e.magnitude > 4.0 " +
                "ORDER BY e.magnitude DESC";

        executeAndPrintQuery(sql, "Сильные землетрясения (магнитуда > 4.0):");
    }

    // 2. Количество землетрясений по штатам
    public void getEarthquakesByState() {
        String sql = "SELECT r.name as state, COUNT(*) as count, AVG(e.magnitude) as avg_magnitude " +
                "FROM earthquakes e " +
                "JOIN regions r ON e.region_id = r.region_id " +
                "GROUP BY r.name " +
                "HAVING COUNT(*) > 5 " +
                "ORDER BY count DESC";

        executeAndPrintQuery(sql, "Количество землетрясений по штатам:");
    }

    // 3. Самые глубокие землетрясения
    public void getDeepestEarthquakes(int limit) {
        String sql = String.format(
                "SELECT e.earthquake_id, r.name as region, e.depth, e.magnitude " +
                        "FROM earthquakes e " +
                        "JOIN regions r ON e.region_id = r.region_id " +
                        "WHERE e.depth > 0 " +
                        "ORDER BY e.depth DESC " +
                        "LIMIT %d", limit);

        executeAndPrintQuery(sql, "Самые глубокие землетрясения:");
    }

    // 4. Землетрясения по годам
    public void getEarthquakesByYear() {
        String sql = "SELECT strftime('%Y', time) as year, " +
                "COUNT(*) as count, " +
                "AVG(magnitude) as avg_magnitude, " +
                "MAX(magnitude) as max_magnitude " +
                "FROM earthquakes " +
                "WHERE time IS NOT NULL " +
                "GROUP BY year " +
                "ORDER BY year";

        executeAndPrintQuery(sql, "Статистика землетрясений по годам:");
    }

    // 5. Средняя магнитуда по типам
    public void getAverageMagnitudeByType() {
        String sql = "SELECT mt.name as type, " +
                "COUNT(*) as count, " +
                "AVG(e.magnitude) as avg_magnitude, " +
                "MIN(e.magnitude) as min_magnitude, " +
                "MAX(e.magnitude) as max_magnitude " +
                "FROM earthquakes e " +
                "JOIN magnitude_types mt ON e.magnitude_type_id = mt.type_id " +
                "GROUP BY mt.name " +
                "ORDER BY avg_magnitude DESC";

        executeAndPrintQuery(sql, "Статистика по типам магнитуд:");
    }

    private void executeAndPrintQuery(String sql, String title) {
        System.out.println("\n" + title);
        System.out.println("=".repeat(50));

        try (ResultSet rs = dbManager.executeQuery(sql)) {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            // Вывод заголовков
            for (int i = 1; i <= columnCount; i++) {
                System.out.printf("%-20s", metaData.getColumnName(i));
            }
            System.out.println();
            System.out.println("-".repeat(columnCount * 20));

            // Вывод данных
            int rowCount = 0;
            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    System.out.printf("%-20s", rs.getString(i));
                }
                System.out.println();
                rowCount++;
            }

            System.out.println("\nНайдено записей: " + rowCount);

        } catch (SQLException e) {
            System.err.println("Error executing query: " + e.getMessage());
        }
    }
}