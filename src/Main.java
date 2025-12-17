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

        try {
            // 1. Парсинг CSV файла
            System.out.println("1. Чтение данных из CSV файла...");
            CSVReader csvReader = new CSVReader();
            List<models.Earthquake> earthquakes = csvReader.readCSV("Землетрясения.csv");

            if (earthquakes.isEmpty()) {
                System.out.println("Не удалось прочитать данные из CSV файла");
                return;
            }

            System.out.println("Успешно прочитано: " + earthquakes.size() + " землетрясений");

            EarthquakeAnalyzer analyzer = new EarthquakeAnalyzer();
            earthquakes.forEach(analyzer::addEarthquake);

            // 2. Вывод статистики
            System.out.println("\n=== Статистическая таблица ===");
            Map<String, Object> stats = analyzer.getStatistics();
            TextChartGenerator.printStatisticsTable(stats, "Общая статистика");

            // 3. Текстовая визуализация данных
            System.out.println("\n=== Текстовая диаграмма 1 ===");
            Map<String, Long> earthquakesByState = earthquakes.stream()
                    .filter(eq -> eq.getState() != null && !eq.getState().isEmpty())
                    .collect(Collectors.groupingBy(
                            eq -> {
                                String state = eq.getState();
                                String[] parts = state.split(",");
                                String cleanState = parts[0].trim();
                                if (cleanState.length() > 25) {
                                    return cleanState.substring(0, 25) + "...";
                                }
                                return cleanState;
                            },
                            Collectors.counting()))
                    .entrySet().stream()
                    .filter(entry -> entry.getValue() > 5)
                    .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                    .limit(15)
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (v1, v2) -> v1,
                            LinkedHashMap::new));

            Map<String, Number> stateData = new HashMap<>();
            for (Map.Entry<String, Long> entry : earthquakesByState.entrySet()) {
                stateData.put(entry.getKey(), entry.getValue());
            }

            TextChartGenerator.printBarChart(
                    "Количество землетрясений по штатам (топ-15)",
                    stateData,
                    "Штат",
                    "Количество землетрясений");

            // Распределение по магнитудам
            System.out.println("\n=== Текстовая диаграмма 2 ===");
            Map<String, Long> magnitudeDistribution = earthquakes.stream()
                    .collect(Collectors.groupingBy(
                            eq -> {
                                double mag = eq.getMagnitude();
                                if (mag < 2.0) return "< 2.0";
                                else if (mag < 3.0) return "2.0 - 2.9";
                                else if (mag < 4.0) return "3.0 - 3.9";
                                else if (mag < 5.0) return "4.0 - 4.9";
                                else return ">= 5.0";
                            },
                            Collectors.counting()));

            Map<String, Number> magnitudeData = new HashMap<>();
            for (Map.Entry<String, Long> entry : magnitudeDistribution.entrySet()) {
                magnitudeData.put(entry.getKey(), entry.getValue());
            }

            TextChartGenerator.printPieChart(
                    "Распределение землетрясений по магнитудам",
                    magnitudeData);

            // 4. Работа с базой данных
            System.out.println("\n=== Результаты SQL запросов ===");
            DatabaseManager dbManager = new DatabaseManager("earthquakes.db");
            dbManager.createTables();
            dbManager.saveEarthquakes(earthquakes);

            // 5. Выполнение SQL запросов
            SQLQueries queries = new SQLQueries(dbManager);
            queries.getStrongEarthquakes();
            queries.getDeepestEarthquakes(10);
            queries.getEarthquakesByYear();
            queries.getAverageMagnitudeByType();
            queries.getTopEarthquakes();

            dbManager.close();

        } catch (Exception e) {
            System.err.println("Ошибка выполнения программы: " + e.getMessage());
        }
    }
}