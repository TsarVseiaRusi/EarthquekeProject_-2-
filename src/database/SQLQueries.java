package database;

import java.sql.*;

public class SQLQueries {
    private DatabaseManager dbManager;

    public SQLQueries(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    // 1. Землетрясения с магнитудой больше 4.0
    public void getStrongEarthquakes() {
        System.out.println("\n=== Таблица 1: Сильные землетрясения (магнитуда > 4.0) ===");
        String sql = "SELECT earthquake_id, magnitude, depth, time " +
                "FROM earthquakes " +
                "WHERE magnitude > 4.0 " +
                "ORDER BY magnitude DESC " +
                "LIMIT 10";

        executeAndPrintQuery(sql, new String[]{"ID", "Магнитуда", "Глубина", "Время"});
    }

    // 2. Самые глубокие землетрясения
    public void getDeepestEarthquakes(int limit) {
        System.out.println("\n=== Таблица 2: Самые глубокие землетрясения ===");
        String sql = String.format(
                "SELECT earthquake_id, depth, magnitude, time " +
                        "FROM earthquakes " +
                        "WHERE depth > 0 " +
                        "ORDER BY depth DESC " +
                        "LIMIT %d", limit);

        executeAndPrintQuery(sql, new String[]{"ID", "Глубина (м)", "Магнитуда", "Время"});
    }

    // 3. Землетрясения по годам
    public void getEarthquakesByYear() {
        System.out.println("\n=== Таблица 3: Землетрясения по годам ===");
        try {
            // Сначала проверяем, есть ли данные с временем
            String checkSql = "SELECT COUNT(*) as cnt FROM earthquakes WHERE time IS NOT NULL";
            ResultSet checkRs = dbManager.executeQuery(checkSql);
            if (checkRs.next() && checkRs.getInt("cnt") == 0) {
                System.out.println("В данных нет информации о времени землетрясений");
                return;
            }

            // Упрощенный запрос - берем год из строки времени
            String sql = "SELECT " +
                    "SUBSTR(time, 1, 4) as year, " +
                    "COUNT(*) as count, " +
                    "AVG(magnitude) as avg_magnitude, " +
                    "MAX(magnitude) as max_magnitude " +
                    "FROM earthquakes " +
                    "WHERE time IS NOT NULL AND time != '' " +
                    "GROUP BY year " +
                    "HAVING year IS NOT NULL AND year != '' " +
                    "ORDER BY year";

            ResultSet rs = dbManager.executeQuery(sql);

            // Вывод заголовков
            System.out.printf("%-10s | %-10s | %-15s | %-15s\n",
                    "Год", "Количество", "Средняя маг.", "Макс. маг.");
            System.out.println("-".repeat(55));

            // Вывод данных
            int rowCount = 0;
            while (rs.next()) {
                String year = rs.getString("year");
                String count = rs.getString("count");
                String avgMag = rs.getString("avg_magnitude");
                String maxMag = rs.getString("max_magnitude");

                // Форматируем числа
                if (avgMag != null) {
                    avgMag = String.format("%.2f", Double.parseDouble(avgMag));
                }

                System.out.printf("%-10s | %-10s | %-15s | %-15s\n",
                        year != null ? year : "N/A",
                        count != null ? count : "0",
                        avgMag != null ? avgMag : "0.00",
                        maxMag != null ? maxMag : "0.00");
                rowCount++;
            }

            if (rowCount == 0) {
                System.out.println("Данные по годам не найдены");
            }

        } catch (SQLException e) {
            System.out.println("Не удалось получить данные по годам");
        } catch (NumberFormatException e) {
            System.out.println("Ошибка форматирования данных");
        }
    }

    // 4. Статистика по магнитудам
    public void getAverageMagnitudeByType() {
        System.out.println("\n=== Таблица 4: Общая статистика ===");
        try {
            String sql = "SELECT " +
                    "COUNT(*) as total_count, " +
                    "AVG(magnitude) as avg_magnitude, " +
                    "MAX(magnitude) as max_magnitude, " +
                    "MIN(magnitude) as min_magnitude, " +
                    "AVG(depth) as avg_depth " +
                    "FROM earthquakes";

            ResultSet rs = dbManager.executeQuery(sql);

            if (rs.next()) {
                System.out.printf("%-25s | %-15s\n", "Параметр", "Значение");
                System.out.println("-".repeat(45));

                System.out.printf("%-25s | %-15d\n", "Всего землетрясений", rs.getInt("total_count"));
                System.out.printf("%-25s | %-15.2f\n", "Средняя магнитуда", rs.getDouble("avg_magnitude"));
                System.out.printf("%-25s | %-15.2f\n", "Максимальная магнитуда", rs.getDouble("max_magnitude"));
                System.out.printf("%-25s | %-15.2f\n", "Минимальная магнитуда", rs.getDouble("min_magnitude"));
                System.out.printf("%-25s | %-15.0f\n", "Средняя глубина (м)", rs.getDouble("avg_depth"));
            }

        } catch (SQLException e) {
            System.out.println("Не удалось получить статистику");
        }
    }

    // 5. Топ землетрясений
    public void getTopEarthquakes() {
        System.out.println("\n=== Таблица 5: Топ-10 землетрясений по магнитуде ===");
        try {
            String sql = "SELECT earthquake_id, magnitude, time " +
                    "FROM earthquakes " +
                    "ORDER BY magnitude DESC " +
                    "LIMIT 10";

            ResultSet rs = dbManager.executeQuery(sql);

            System.out.printf("%-20s | %-15s | %-20s\n", "ID", "Магнитуда", "Время");
            System.out.println("-".repeat(60));

            int rowCount = 0;
            while (rs.next()) {
                String id = rs.getString("earthquake_id");
                String magnitude = String.format("%.2f", rs.getDouble("magnitude"));
                String time = rs.getString("time");

                System.out.printf("%-20s | %-15s | %-20s\n",
                        id != null && id.length() > 20 ? id.substring(0, 20) + "..." : id,
                        magnitude,
                        time != null && time.length() > 20 ? time.substring(0, 20) + "..." : time);
                rowCount++;
            }

        } catch (SQLException e) {
            System.out.println("Не удалось получить топ землетрясений");
        }
    }

    // Общий метод для выполнения запросов
    private void executeAndPrintQuery(String sql, String[] headers) {
        try {
            ResultSet rs = dbManager.executeQuery(sql);

            // Вывод заголовков
            for (String header : headers) {
                System.out.printf("%-20s", header);
            }
            System.out.println();
            System.out.println("-".repeat(headers.length * 20));

            // Вывод данных
            int rowCount = 0;
            while (rs.next()) {
                for (int i = 1; i <= headers.length; i++) {
                    String value = rs.getString(i);
                    // Форматируем числа
                    if (value != null && value.matches("-?\\d+(\\.\\d+)?")) {
                        try {
                            double num = Double.parseDouble(value);
                            if (headers[i-1].contains("магнитуд") || headers[i-1].contains("Магнитуда")) {
                                value = String.format("%.2f", num);
                            } else if (headers[i-1].contains("Глубина")) {
                                value = String.format("%.0f", num);
                            }
                        } catch (NumberFormatException e) {
                            // Оставляем как есть
                        }
                    }
                    System.out.printf("%-20s",
                            value != null && value.length() > 20 ? value.substring(0, 20) + "..." : value);
                }
                System.out.println();
                rowCount++;
            }

            if (rowCount == 0) {
                System.out.println("Нет данных для отображения");
            }

        } catch (SQLException e) {
            System.out.println("Ошибка выполнения запроса");
        }
    }
}