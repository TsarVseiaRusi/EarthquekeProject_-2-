package parser;

import models.Earthquake;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CSVReaderTest {

    public static void main(String[] args) {
        System.out.println("=== Запуск тестов CSVReader ===");

        try {
            testReadCSV();
            testParseDouble();
            System.out.println("✓ Все тесты CSVReader пройдены успешно!");
        } catch (Exception e) {
            System.err.println("✗ Тест провален: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testReadCSV() throws IOException {
        // Создаем временный CSV файл
        Path tempFile = Files.createTempFile("test-csv", ".csv");
        String csvContent = """
            id,depth,magnitude_type,magnitude,state,time
            us7000l5h5,10.5,Mw,4.7,California,2023-12-15T14:30:00
            us7000l6h6,5.2,Mb,3.2,New York,2023-12-14T08:15:00
            """;

        Files.writeString(tempFile, csvContent, StandardCharsets.UTF_8);

        CSVReader csvReader = new CSVReader();
        List<Earthquake> earthquakes = csvReader.readCSV(tempFile.toString());

        assert earthquakes.size() == 2 : "Должно быть 2 землетрясения, а найдено: " + earthquakes.size();

        Earthquake first = earthquakes.get(0);
        assert first.getId().equals("us7000l5h5") : "ID не совпадает";
        assert first.getDepth() == 10.5 : "Глубина не совпадает";
        assert first.getMagnitude() == 4.7 : "Магнитуда не совпадает";
        assert first.getState().equals("California") : "Штат не совпадает";

        Files.delete(tempFile);
        System.out.println("  ✓ testReadCSV пройден");
    }

    private static void testParseDouble() {
        CSVReader csvReader = new CSVReader();

        try {
            // Используем рефлексию для тестирования приватного метода
            java.lang.reflect.Method method = CSVReader.class.getDeclaredMethod("parseDouble", String.class);
            method.setAccessible(true);

            double result1 = (double) method.invoke(csvReader, "10.5");
            assert result1 == 10.5 : "parseDouble(10.5) должен вернуть 10.5";

            double result2 = (double) method.invoke(csvReader, "15,3");
            assert result2 == 15.3 : "parseDouble(15,3) должен вернуть 15.3";

            double result3 = (double) method.invoke(csvReader, "invalid");
            assert result3 == 0.0 : "parseDouble(invalid) должен вернуть 0.0";

            System.out.println("  ✓ testParseDouble пройден");
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при тестировании parseDouble", e);
        }
    }
}