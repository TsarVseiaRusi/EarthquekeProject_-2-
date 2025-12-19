import models.EarthquakeAnalyzer;
import parser.CSVReader;
import database.DatabaseManager;
import database.SQLQueries;
import visualization.TextChartGenerator;
import java.util.*;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Проект анализа данных о землетрясениях ===\n");
        System.out.println("=".repeat(50));

        try {
            // 1. Отладка CSV файла
            CSVReader csvReader = new CSVReader();
            csvReader.debugCSV("Землетрясения.csv");

            // 2. Чтение данных из CSV файла
            System.out.println("\n2. Чтение данных из CSV файла...");
            List<models.Earthquake> earthquakes = csvReader.readCSV("Землетрясения.csv");

            if (earthquakes.isEmpty()) {
                System.out.println("ОШИБКА: Не удалось прочитать данные из CSV файла");
                return;
            }

            // Анализ данных о времени
            int withTime = 0;
            int withoutTime = 0;
            for (models.Earthquake eq : earthquakes) {
                if (eq.getTime() != null) {
                    withTime++;
                } else {
                    withoutTime++;
                }
            }

            System.out.println("\nСтатистика времени:");
            System.out.println("  С временем: " + withTime + " (" +
                    String.format("%.1f%%", earthquakes.size() > 0 ? (withTime * 100.0 / earthquakes.size()) : 0) + ")");
            System.out.println("  Без времени: " + withoutTime + " (" +
                    String.format("%.1f%%", earthquakes.size() > 0 ? (withoutTime * 100.0 / earthquakes.size()) : 0) + ")");

            // 3. Создание анализатора
            System.out.println("\n" + "=".repeat(50));
            System.out.println("3. Создание анализатора...");
            EarthquakeAnalyzer analyzer = new EarthquakeAnalyzer();
            for (models.Earthquake eq : earthquakes) {
                analyzer.addEarthquake(eq);
            }

            // 4. Вывод общей статистики
            System.out.println("\n" + "=".repeat(50));
            System.out.println("4. Общая статистика:");

            Map<String, Object> stats = analyzer.getStatistics();
            TextChartGenerator.printStatisticsTable(stats, "Общая статистика данных");

            // 5. Текстовая визуализация данных
            System.out.println("\n" + "=".repeat(50));
            System.out.println("5. Визуализация данных:");

            // 5.1 Распределение по штатам
            System.out.println("\n=== Распределение по штатам ===");
            Map<String, Long> earthquakesByState = analyzer.getEarthquakeCountByState();
            if (!earthquakesByState.isEmpty()) {
                Map<String, Number> stateData = new HashMap<>();
                for (Map.Entry<String, Long> entry : earthquakesByState.entrySet()) {
                    stateData.put(entry.getKey(), entry.getValue());
                }
                TextChartGenerator.printBarChart(
                        "Количество землетрясений по штатам (топ-15)",
                        stateData,
                        "Штат",
                        "Количество землетрясений");

                // Дополнительная информация о топ-5 штатах
                System.out.println("\nТоп-5 штатов по количеству землетрясений:");
                int count = 0;
                for (Map.Entry<String, Long> entry : earthquakesByState.entrySet()) {
                    if (count >= 5) break;
                    System.out.printf("  %d. %s: %d землетрясений\n",
                            count + 1, entry.getKey(), entry.getValue());
                    count++;
                }
            } else {
                System.out.println("Нет данных по штатам для визуализации");
            }

            // 5.2 Распределение по магнитудам
            System.out.println("\n=== Распределение по магнитудам ===");
            Map<String, Long> magnitudeDistribution = analyzer.getMagnitudeDistribution();
            if (!magnitudeDistribution.isEmpty()) {
                Map<String, Number> magnitudeData = new HashMap<>();
                for (Map.Entry<String, Long> entry : magnitudeDistribution.entrySet()) {
                    magnitudeData.put(entry.getKey(), entry.getValue());
                }
                TextChartGenerator.printPieChart(
                        "Распределение землетрясений по магнитудам",
                        magnitudeData);
            } else {
                System.out.println("Нет данных по магнитудам для визуализации");
            }

            // 5.3 Распределение по глубине
            System.out.println("\n=== Распределение по глубине ===");
            Map<String, Long> depthDistribution = analyzer.getDepthDistribution();
            if (!depthDistribution.isEmpty()) {
                Map<String, Number> depthData = new HashMap<>();
                for (Map.Entry<String, Long> entry : depthDistribution.entrySet()) {
                    depthData.put(entry.getKey(), entry.getValue());
                }
                TextChartGenerator.printBarChart(
                        "Распределение землетрясений по глубине",
                        depthData,
                        "Глубина",
                        "Количество землетрясений");
            } else {
                System.out.println("Нет данных по глубине для визуализации");
            }

            // 5.4 Распределение по годам (если есть данные о времени)
            System.out.println("\n=== Распределение по годам ===");
            Map<String, Long> yearDistribution = analyzer.getYearDistribution();
            if (!yearDistribution.isEmpty()) {
                // Сортируем годы от большего к меньшему
                Map<String, Long> sortedYearDistribution = yearDistribution.entrySet()
                        .stream()
                        .sorted((a, b) -> Integer.compare(
                                Integer.parseInt(b.getKey()),
                                Integer.parseInt(a.getKey())
                        ))
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (v1, v2) -> v1,
                                LinkedHashMap::new
                        ));

                Map<String, Number> yearData = new HashMap<>();
                for (Map.Entry<String, Long> entry : sortedYearDistribution.entrySet()) {
                    yearData.put(entry.getKey(), entry.getValue());
                }
                TextChartGenerator.printBarChart(
                        "Распределение землетрясений по годам",
                        yearData,
                        "Год",
                        "Количество землетрясений");

                // Дополнительная информация по годам
                int totalYears = yearDistribution.size();
                long totalEarthquakes = yearDistribution.values().stream().mapToLong(Long::longValue).sum();
                double averagePerYear = (double) totalEarthquakes / totalYears;

                System.out.println("\nСтатистика по годам:");
                System.out.printf("  Всего лет с данными: %d\n", totalYears);
                System.out.printf("  Среднее количество в год: %.1f\n", averagePerYear);

                // Находим год с максимальным количеством
                Optional<Map.Entry<String, Long>> maxYearEntry = yearDistribution.entrySet()
                        .stream()
                        .max(Map.Entry.comparingByValue());

                if (maxYearEntry.isPresent()) {
                    System.out.printf("  Год с наибольшим количеством: %s (%d землетрясений)\n",
                            maxYearEntry.get().getKey(), maxYearEntry.get().getValue());
                }
            } else {
                System.out.println("Нет данных по годам для визуализации (отсутствует информация о времени)");
            }

            // 6. Работа с базой данных
            System.out.println("\n" + "=".repeat(50));
            System.out.println("6. Работа с базой данных:");

            DatabaseManager dbManager = new DatabaseManager("earthquakes.db");
            dbManager.createTables();
            dbManager.saveEarthquakes(earthquakes);

            // 7. Выполнение SQL запросов
            System.out.println("\n" + "=".repeat(50));
            System.out.println("7. Результаты SQL запросов:");

            SQLQueries queries = new SQLQueries(dbManager);

            // 7.1 Сильные землетрясения
            queries.getStrongEarthquakes();

            // 7.2 Самые глубокие землетрясения
            queries.getDeepestEarthquakes(10);

            // 7.3 Землетрясения по годам
            queries.getEarthquakesByYear();

            // 7.4 Общая статистика
            queries.getAverageMagnitudeByType();

            // 7.5 Топ землетрясений по магнитуде
            queries.getTopEarthquakes();

            // 7.6 Дополнительный анализ по последнему году, если есть данные
            if (!yearDistribution.isEmpty()) {
                List<String> years = new ArrayList<>(yearDistribution.keySet());
                if (!years.isEmpty()) {
                    int latestYear = Integer.parseInt(Collections.max(years));
                    System.out.println("\n=== Анализ по последнему году (" + latestYear + ") ===");
                    queries.getEarthquakesByMonth(latestYear);
                }
            }

            // 8. Дополнительный анализ из анализатора
            System.out.println("\n" + "=".repeat(50));
            System.out.println("8. Дополнительный анализ:");

            // 8.1 Топ-10 землетрясений по магнитуде (из анализатора)
            System.out.println("\nТоп-10 землетрясений по магнитуде (анализ в памяти):");
            List<models.Earthquake> topByMagnitude = analyzer.getTopByMagnitude(10);
            if (!topByMagnitude.isEmpty()) {
                System.out.printf("%-15s | %-10s | %-10s | %-20s | %-25s\n",
                        "ID", "Магнитуда", "Глубина", "Штат", "Время");
                System.out.println("-".repeat(85));

                for (models.Earthquake eq : topByMagnitude) {
                    String timeStr = (eq.getTime() != null) ?
                            eq.getTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) :
                            "Нет данных";

                    System.out.printf("%-15s | %-10.2f | %-10.0f | %-20s | %-25s\n",
                            eq.getId().length() > 15 ? eq.getId().substring(0, 12) + "..." : eq.getId(),
                            eq.getMagnitude(),
                            eq.getDepth(),
                            eq.getState() != null && eq.getState().length() > 20 ?
                                    eq.getState().substring(0, 17) + "..." : eq.getState(),
                            timeStr);
                }
            }

            // 8.2 Топ-5 самых глубоких землетрясений
            System.out.println("\nТоп-5 самых глубоких землетрясений:");
            List<models.Earthquake> topByDepth = analyzer.getTopByDepth(5);
            if (!topByDepth.isEmpty()) {
                System.out.printf("%-15s | %-10s | %-10s | %-20s\n",
                        "ID", "Глубина (м)", "Магнитуда", "Штат");
                System.out.println("-".repeat(60));

                for (models.Earthquake eq : topByDepth) {
                    System.out.printf("%-15s | %-10.0f | %-10.2f | %-20s\n",
                            eq.getId().length() > 15 ? eq.getId().substring(0, 12) + "..." : eq.getId(),
                            eq.getDepth(),
                            eq.getMagnitude(),
                            eq.getState() != null && eq.getState().length() > 20 ?
                                    eq.getState().substring(0, 17) + "..." : eq.getState());
                }
            }

            // 9. Итоговая статистика
            System.out.println("\n" + "=".repeat(50));
            System.out.println("9. Итоговая статистика:");

            System.out.println("Всего обработано записей: " + earthquakes.size());
            System.out.println("Успешно считано из CSV: " + earthquakes.size());
            System.out.println("Записей с временем: " + withTime +
                    String.format(" (%.1f%%)", earthquakes.size() > 0 ? (withTime * 100.0 / earthquakes.size()) : 0));

            // Полная статистика по штатам
            Map<String, Long> fullStateStats = analyzer.getStateStatistics();
            System.out.println("Всего уникальных штатов (после нормализации): " + fullStateStats.size());

            if (!fullStateStats.isEmpty()) {
                System.out.println("\nСтатистика по всем штатам:");
                System.out.printf("%-30s | %-15s\n", "Штат", "Количество");
                System.out.println("-".repeat(50));

                // Выводим ВСЕ штаты (без ограничения в 10)
                for (Map.Entry<String, Long> entry : fullStateStats.entrySet()) {
                    System.out.printf("%-30s | %-15d\n",
                            entry.getKey().length() > 30 ? entry.getKey().substring(0, 27) + "..." : entry.getKey(),
                            entry.getValue());
                }
            }

            // Закрываем соединение с базой данных
            dbManager.close();

            System.out.println("\n" + "=".repeat(50));
            System.out.println("Анализ завершен успешно!");
            System.out.println("=".repeat(50));

        } catch (Exception e) {
            System.err.println("\nОШИБКА выполнения программы: " + e.getMessage());
            e.printStackTrace();

            // Выводим только релевантные части стека вызовов
            System.err.println("\nСтек вызовов (отфильтрованный):");
            boolean foundRelevant = false;
            for (StackTraceElement element : e.getStackTrace()) {
                if (element.getClassName().contains("earthquake") ||
                        element.getClassName().contains("Earthquake") ||
                        element.getClassName().contains("parser") ||
                        element.getClassName().contains("database") ||
                        element.getClassName().contains("models")) {
                    System.err.println("  " + element);
                    foundRelevant = true;
                }
            }

            if (!foundRelevant) {
                // Если не нашли релевантных, показываем первые 5 строк
                System.err.println("Первые 5 строк стека вызовов:");
                for (int i = 0; i < Math.min(5, e.getStackTrace().length); i++) {
                    System.err.println("  " + e.getStackTrace()[i]);
                }
            }
        }
    }
}