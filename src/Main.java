import models.EarthquakeAnalyzer;
import parser.CSVReader;
import database.DatabaseManager;
import database.SQLQueries;
import visualization.TextChartGenerator;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Проект анализа данных о землетрясениях ===\n");

        try {
            // 1. Парсинг CSV файла
            System.out.println("1. Чтение данных из CSV файла...");
            CSVReader csvReader = new CSVReader();
            List<models.Earthquake> earthquakes = csvReader.readCSV("Землетрясения.csv");

            EarthquakeAnalyzer analyzer = new EarthquakeAnalyzer();
            earthquakes.forEach(analyzer::addEarthquake);

            // 2. Вывод статистики
            System.out.println("\n2. Статистика данных:");
            Map<String, Object> stats = analyzer.getStatistics();
            TextChartGenerator.printStatisticsTable(stats, "Общая статистика");

            // 3. Создание и подключение к БД
            System.out.println("\n3. Создание базы данных...");
            DatabaseManager dbManager = new DatabaseManager("earthquakes.db");
            dbManager.createTables();

            // 4. Сохранение данных в БД
            System.out.println("\n4. Сохранение данных в базу данных...");
            dbManager.saveEarthquakes(earthquakes);

            // 5. Выполнение SQL запросов
            System.out.println("\n5. Выполнение SQL запросов...");
            SQLQueries queries = new SQLQueries(dbManager);

            queries.getStrongEarthquakes();
            queries.getEarthquakesByState();
            queries.getDeepestEarthquakes(10);
            queries.getEarthquakesByYear();
            queries.getAverageMagnitudeByType();

            // 6. Текстовая визуализация данных
            System.out.println("\n6. Текстовая визуализация данных...");

            // Используем методы из EarthquakeAnalyzer
            TextChartGenerator.printBarChart(
                    "Количество землетрясений по штатам (топ-15)",
                    convertToNumberMap(analyzer.getEarthquakeCountByState()),
                    "Штат",
                    "Количество землетрясений");

            TextChartGenerator.printPieChart(
                    "Распределение землетрясений по магнитудам",
                    convertToNumberMap(analyzer.getMagnitudeDistribution()));

            TextChartGenerator.printBarChart(
                    "Распределение землетрясений по глубине",
                    convertToNumberMap(analyzer.getDepthDistribution()),
                    "Глубина",
                    "Количество");

            TextChartGenerator.printBarChart(
                    "Количество землетрясений по годам",
                    convertToNumberMap(analyzer.getYearDistribution()),
                    "Год",
                    "Количество");

            System.out.println("\n7. Завершение работы...");
            dbManager.close();


            System.out.println("КОМПИЛЯЦИЯ УСПЕШНО ЗАВЕРШЕН!");



        } catch (Exception e) {
            System.err.println("Ошибка выполнения программы: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Вспомогательный метод для преобразования Map<String, Long> в Map<String, Number>
    private static Map<String, Number> convertToNumberMap(Map<String, Long> longMap) {
        Map<String, Number> numberMap = new HashMap<>();
        for (Map.Entry<String, Long> entry : longMap.entrySet()) {
            numberMap.put(entry.getKey(), entry.getValue());
        }
        return numberMap;
    }
}