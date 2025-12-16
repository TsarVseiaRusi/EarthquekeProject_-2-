package parser;

import models.Earthquake;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CSVReader {
    public List<Earthquake> readCSV(String filename) {
        List<Earthquake> earthquakes = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(filename), StandardCharsets.UTF_8))) {

            String line = br.readLine(); // Пропускаем заголовок

            while ((line = br.readLine()) != null) {
                try {
                    Earthquake eq = parseLine(line);
                    if (eq != null) {
                        earthquakes.add(eq);
                    }
                } catch (Exception e) {
                    // Пропускаем некорректные строки
                }
            }

            System.out.println("Успешно прочитано " + earthquakes.size() + " землетрясений");

        } catch (IOException e) {
            System.err.println("Ошибка чтения файла: " + e.getMessage());
        }

        return earthquakes;
    }

    private Earthquake parseLine(String line) {
        try {
            // Простой парсинг - разделяем по запятой
            String[] parts = parseCSVLine(line);
            if (parts.length < 6) return null;

            String id = parts[0].trim();
            double depth = parseDouble(parts[1]);
            String magnitudeType = parts[2].trim();
            double magnitude = parseDouble(parts[3]);
            String state = parts[4].trim().replace("\"", "");
            LocalDateTime time = parseDateTime(parts[5]);

            return new Earthquake(id, depth, magnitudeType, magnitude, state, time);

        } catch (Exception e) {
            return null;
        }
    }

    private String[] parseCSVLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());

        return result.toArray(new String[0]);
    }

    private double parseDouble(String value) {
        if (value == null || value.trim().isEmpty()) return 0.0;
        try {
            return Double.parseDouble(value.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.trim().isEmpty()) return null;

        value = value.trim().replace("\"", "");

        // Пробуем разные форматы
        DateTimeFormatter[] formatters = {
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd")
        };

        for (DateTimeFormatter formatter : formatters) {
            try {
                if (formatter.toString().contains("HH")) {
                    return LocalDateTime.parse(value, formatter);
                } else {
                    // Для форматов без времени добавляем 00:00:00
                    return LocalDateTime.parse(value + "T00:00:00",
                            DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                }
            } catch (Exception e) {
                continue;
            }
        }

        return null;
    }
}