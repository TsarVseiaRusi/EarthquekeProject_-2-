package database;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

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

        executeAndPrintQueryWithTime(sql, new String[]{"ID", "Магнитуда", "Глубина", "Время"});
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

        executeAndPrintQueryWithTime(sql, new String[]{"ID", "Глубина (м)", "Магнитуда", "Время"});
    }

    // 3. Землетрясения по годам
    public void getEarthquakesByYear() {
        System.out.println("\n=== Таблица 3: Землетрясения по годам ===");
        try {
            // Проверяем наличие данных о времени
            String checkSql = "SELECT COUNT(*) as cnt FROM earthquakes WHERE time IS NOT NULL";
            ResultSet checkRs = dbManager.executeQuery(checkSql);

            int timeCount = 0;
            if (checkRs.next()) {
                timeCount = checkRs.getInt("cnt");
            }

            if (timeCount == 0) {
                System.out.println("В данных нет информации о времени землетрясений");
                return;
            }

            System.out.println("Найдено записей со временем: " + timeCount);

            // Запрос для анализа по годам
            String sql = "SELECT " +
                    "strftime('%Y', datetime(time/1000, 'unixepoch')) as year, " +
                    "COUNT(*) as count, " +
                    "AVG(magnitude) as avg_magnitude, " +
                    "MAX(magnitude) as max_magnitude, " +
                    "MIN(magnitude) as min_magnitude " +
                    "FROM earthquakes " +
                    "WHERE time IS NOT NULL AND time != 0 " +
                    "GROUP BY year " +
                    "HAVING year IS NOT NULL AND year != '' " +
                    "ORDER BY year DESC";

            ResultSet rs = dbManager.executeQuery(sql);

            // Вывод заголовков
            System.out.printf("%-10s | %-12s | %-15s | %-15s | %-15s\n",
                    "Год", "Количество", "Средняя маг.", "Макс. маг.", "Мин. маг.");
            System.out.println("-".repeat(75));

            // Вывод данных
            int rowCount = 0;
            double totalAvgMagnitude = 0;
            int totalCount = 0;

            while (rs.next()) {
                String year = rs.getString("year");
                int count = rs.getInt("count");
                double avgMag = rs.getDouble("avg_magnitude");
                double maxMag = rs.getDouble("max_magnitude");
                double minMag = rs.getDouble("min_magnitude");

                if (!rs.wasNull()) {
                    System.out.printf("%-10s | %-12d | %-15.2f | %-15.2f | %-15.2f\n",
                            year != null ? year : "N/A",
                            count,
                            avgMag,
                            maxMag,
                            minMag);

                    totalAvgMagnitude += avgMag * count;
                    totalCount += count;
                    rowCount++;
                }
            }

            if (rowCount == 0) {
                System.out.println("Данные по годам не найдены");
            } else {
                System.out.println("-".repeat(75));
                System.out.printf("%-10s | %-12d | %-15.2f\n",
                        "ИТОГО",
                        totalCount,
                        totalCount > 0 ? totalAvgMagnitude / totalCount : 0);
            }

        } catch (SQLException e) {
            System.out.println("Ошибка при получении данных по годам: " + e.getMessage());
        }
    }

    // 4. Статистика по магнитудам
    public void getAverageMagnitudeByType() {
        System.out.println("\n=== Таблица 4: Общая статистика ===");
        try {
            String sql = "SELECT " +
                    "COUNT(*) as total_count, " +
                    "SUM(CASE WHEN time IS NOT NULL AND time != 0 THEN 1 ELSE 0 END) as with_time_count, " +
                    "AVG(magnitude) as avg_magnitude, " +
                    "MAX(magnitude) as max_magnitude, " +
                    "MIN(magnitude) as min_magnitude, " +
                    "AVG(depth) as avg_depth, " +
                    "MAX(depth) as max_depth " +
                    "FROM earthquakes";

            ResultSet rs = dbManager.executeQuery(sql);

            if (rs.next()) {
                System.out.printf("%-30s | %-15s\n", "Параметр", "Значение");
                System.out.println("-".repeat(50));

                int totalCount = rs.getInt("total_count");
                int withTimeCount = rs.getInt("with_time_count");

                System.out.printf("%-30s | %-15d\n", "Всего землетрясений", totalCount);
                System.out.printf("%-30s | %-15d\n", "С временем", withTimeCount);
                System.out.printf("%-30s | %-15.1f%%\n", "Процент с временем",
                        totalCount > 0 ? (withTimeCount * 100.0 / totalCount) : 0);
                System.out.println("-".repeat(50));
                System.out.printf("%-30s | %-15.2f\n", "Средняя магнитуда", rs.getDouble("avg_magnitude"));
                System.out.printf("%-30s | %-15.2f\n", "Максимальная магнитуда", rs.getDouble("max_magnitude"));
                System.out.printf("%-30s | %-15.2f\n", "Минимальная магнитуда", rs.getDouble("min_magnitude"));
                System.out.printf("%-30s | %-15.0f\n", "Средняя глубина (м)", rs.getDouble("avg_depth"));
                System.out.printf("%-30s | %-15.0f\n", "Максимальная глубина (м)", rs.getDouble("max_depth"));
            }

        } catch (SQLException e) {
            System.out.println("Не удалось получить статистику: " + e.getMessage());
        }
    }

    // 5. Топ землетрясений
    public void getTopEarthquakes() {
        System.out.println("\n=== Таблица 5: Топ-10 землетрясений по магнитуде ===");
        try {
            String sql = "SELECT earthquake_id, magnitude, depth, time " +
                    "FROM earthquakes " +
                    "WHERE magnitude > 0 " +
                    "ORDER BY magnitude DESC " +
                    "LIMIT 10";

            ResultSet rs = dbManager.executeQuery(sql);

            System.out.printf("%-15s | %-10s | %-10s | %-25s\n",
                    "ID", "Магнитуда", "Глубина", "Время");
            System.out.println("-".repeat(65));

            int rowCount = 0;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            while (rs.next()) {
                String id = rs.getString("earthquake_id");
                double magnitude = rs.getDouble("magnitude");
                double depth = rs.getDouble("depth");
                long timestamp = rs.getLong("time");

                String formattedTime = "Нет данных";
                if (!rs.wasNull() && timestamp > 0) {
                    try {
                        // Преобразуем timestamp в LocalDateTime
                        Instant instant = Instant.ofEpochMilli(timestamp);
                        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                        formattedTime = dateTime.format(formatter);
                    } catch (Exception e) {
                        formattedTime = "Ошибка формата";
                    }
                }

                String displayId = (id != null && id.length() > 15) ? id.substring(0, 12) + "..." : id;

                System.out.printf("%-15s | %-10.2f | %-10.0f | %-25s\n",
                        displayId != null ? displayId : "N/A",
                        magnitude,
                        depth,
                        formattedTime);

                rowCount++;
            }

            if (rowCount == 0) {
                System.out.println("Нет данных для отображения");
            }

        } catch (SQLException e) {
            System.out.println("Не удалось получить топ землетрясений: " + e.getMessage());
        }
    }

    // Землетрясения по месяцам
    public void getEarthquakesByMonth(int year) {
        System.out.println("\n=== Таблица 6: Землетрясения по месяцам за " + year + " год ===");
        try {
            String sql = "SELECT " +
                    "strftime('%m', datetime(time/1000, 'unixepoch')) as month, " +
                    "COUNT(*) as count, " +
                    "AVG(magnitude) as avg_magnitude " +
                    "FROM earthquakes " +
                    "WHERE time IS NOT NULL AND time != 0 " +
                    "AND strftime('%Y', datetime(time/1000, 'unixepoch')) = ? " +
                    "GROUP BY month " +
                    "ORDER BY month";

            Connection conn = dbManager.getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, String.valueOf(year));
                ResultSet rs = pstmt.executeQuery();

                System.out.printf("%-10s | %-15s | %-15s\n", "Месяц", "Количество", "Ср. магнитуда");
                System.out.println("-".repeat(45));

                int rowCount = 0;
                while (rs.next()) {
                    String month = rs.getString("month");
                    int count = rs.getInt("count");
                    double avgMag = rs.getDouble("avg_magnitude");

                    String monthName = getMonthName(Integer.parseInt(month));

                    System.out.printf("%-10s | %-15d | %-15.2f\n",
                            monthName,
                            count,
                            avgMag);
                    rowCount++;
                }

                if (rowCount == 0) {
                    System.out.println("Нет данных за " + year + " год");
                }
            }

        } catch (SQLException e) {
            System.out.println("Ошибка при получении данных по месяцам: " + e.getMessage());
        }
    }

    // Общий метод для выполнения запросов с временем (с форматированием)
    private void executeAndPrintQueryWithTime(String sql, String[] headers) {
        try {
            ResultSet rs = dbManager.executeQuery(sql);

            // Вывод заголовков
            for (String header : headers) {
                System.out.printf("%-20s", header);
            }
            System.out.println();
            System.out.println("-".repeat(headers.length * 20));

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            // Вывод данных
            int rowCount = 0;
            while (rs.next()) {
                for (int i = 1; i <= headers.length; i++) {
                    String header = headers[i-1];
                    Object valueObj = null;

                    if (header.equals("Время") || header.equalsIgnoreCase("time")) {
                        // Специальная обработка для времени
                        long timestamp = rs.getLong(i);
                        String value;
                        if (!rs.wasNull() && timestamp > 0) {
                            try {
                                Instant instant = Instant.ofEpochMilli(timestamp);
                                LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                                value = dateTime.format(formatter);
                            } catch (Exception e) {
                                value = String.valueOf(timestamp);
                            }
                        } else {
                            value = "Нет данных";
                        }
                        System.out.printf("%-20s", value);
                    } else {
                        // Обычная обработка других полей
                        String value = rs.getString(i);
                        if (value != null) {
                            // Форматирование числовых значений
                            if (value.matches("-?\\d+(\\.\\d+)?")) {
                                try {
                                    double num = Double.parseDouble(value);
                                    if (header.contains("Магнитуд") || header.contains("магнитуд")) {
                                        value = String.format("%.2f", num);
                                    } else if (header.contains("Глубина")) {
                                        value = String.format("%.0f м", num);
                                    }
                                } catch (NumberFormatException e) {
                                    // Оставляем как есть
                                }
                            }
                        }
                        System.out.printf("%-20s",
                                value != null && value.length() > 20 ?
                                        value.substring(0, 17) + "..." :
                                        (value != null ? value : "N/A"));
                    }
                }
                System.out.println();
                rowCount++;
            }

            if (rowCount == 0) {
                System.out.println("Нет данных для отображения");
            }

        } catch (SQLException e) {
            System.out.println("Ошибка выполнения запроса: " + e.getMessage());
        }
    }

    private void executeAndPrintQuery(String sql, String[] headers) {
        executeAndPrintQueryWithTime(sql, headers);
    }

    private String getMonthName(int month) {
        String[] monthNames = {
                "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
                "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
        };
        return monthNames[month - 1];
    }
}