package parser;

import models.Earthquake;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class CSVReader {
    public List<Earthquake> readCSV(String filename) {
        List<Earthquake> earthquakes = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(filename), StandardCharsets.UTF_8))) {

            System.out.println("=== Начало чтения CSV файла ===");

            // Читаем заголовок для отладки
            String header = br.readLine();
            if (header != null) {
                System.out.println("Заголовок файла: " + header);
                System.out.println("Колонки: " + Arrays.toString(header.split(",")));
            }

            int lineNum = 1;
            int successCount = 0;
            int errorCount = 0;

            String line; // Объявляем переменную здесь
            while ((line = br.readLine()) != null) {
                lineNum++;
                try {
                    Earthquake eq = parseLine(line);
                    if (eq != null) {
                        earthquakes.add(eq);
                        successCount++;



                    } else {
                        errorCount++;
                        if (errorCount <= 3) {
                            System.err.println("Не удалось распарсить строку " + lineNum + ": " +
                                    (line.length() > 100 ? line.substring(0, 100) + "..." : line));
                        }
                    }
                } catch (Exception e) {
                    errorCount++;
                    if (errorCount <= 3) {
                        System.err.println("Ошибка в строке " + lineNum + ": " + e.getMessage());
                    }
                }
            }

            System.out.println("\n=== Результаты чтения ===");
            System.out.println("Всего строк в файле: " + lineNum);
            System.out.println("Успешно прочитано: " + successCount);
            System.out.println("Ошибок чтения: " + errorCount);
            System.out.println("Процент успеха: " +
                    (lineNum > 1 ? String.format("%.1f%%", (successCount * 100.0 / (lineNum - 1))) : "0%"));

        } catch (IOException e) {
            System.err.println("Критическая ошибка чтения файла: " + e.getMessage());
            e.printStackTrace();
        }

        return earthquakes;
    }

    public void debugCSV(String filename) {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(filename), StandardCharsets.UTF_8))) {

            System.out.println("=== Отладка CSV файла ===");
            String line; // Объявляем переменную здесь
            int lineNum = 0;

            while ((line = br.readLine()) != null && lineNum < 10) {
                lineNum++;
                System.out.println("Строка " + lineNum + ": " + line);

                if (lineNum == 1) {
                    System.out.println("Заголовок (разделенный): " + Arrays.toString(line.split(",")));
                } else if (lineNum == 2) {
                    System.out.println("Первая строка данных (разделенная): " + Arrays.toString(parseCSVLine(line)));
                }
            }

            System.out.println("=========================");

        } catch (IOException e) {
            System.err.println("Ошибка чтения файла для отладки: " + e.getMessage());
        }
    }

    private Earthquake parseLine(String line) {
        try {
            // Парсинг CSV
            String[] parts = parseCSVLine(line);

            // Вывод отладки для первой строки
            if (parts.length < 6) {
                System.err.println("Недостаточно колонок в строке: " + parts.length +
                        " (ожидается минимум 6). Строка: " +
                        (line.length() > 50 ? line.substring(0, 50) + "..." : line));
                return null;
            }

            // Парсим каждое поле с проверкой
            String id = parts[0].trim();
            if (id.isEmpty()) {
                id = "UNKNOWN-" + UUID.randomUUID().toString().substring(0, 8);
            }

            double depth = parseDouble(parts[1]);
            String magnitudeType = parts[2].trim();
            double magnitude = parseDouble(parts[3]);
            String state = parts[4].trim().replace("\"", "");

            String timeStr = parts[5].trim();
            LocalDateTime time = parseDateTime(timeStr);

            // Отладка для проблемных строк
            if (time == null && timeStr != null && !timeStr.isEmpty()) {
                System.err.println("ВНИМАНИЕ: Не удалось распарсить время: '" + timeStr + "'");
                System.err.println("Строка целиком: " + line);
            }

            // Создаем объект землетрясения
            Earthquake eq = new Earthquake(id, depth, magnitudeType, magnitude, state, time);

            // Дополнительная проверка
            if (magnitude == 0.0 && depth == 0.0) {
                System.err.println("ПРЕДУПРЕЖДЕНИЕ: Нулевые значения магнитуды и глубины для ID: " + id);
            }

            return eq;

        } catch (Exception e) {
            System.err.println("Критическая ошибка парсинга строки: " + e.getMessage());
            System.err.println("Строка: " + (line.length() > 100 ? line.substring(0, 100) + "..." : line));
            return null;
        }
    }

    private String[] parseCSVLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean lastCharWasQuote = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // Двойная кавычка внутри кавычек - экранирование
                    current.append('"');
                    i++; // Пропускаем следующую кавычку
                } else {
                    inQuotes = !inQuotes;
                    lastCharWasQuote = true;
                }
            } else if (c == ',' && !inQuotes) {
                String value = current.toString();
                // Убираем внешние кавычки, если есть
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                result.add(value);
                current = new StringBuilder();
                lastCharWasQuote = false;
            } else {
                current.append(c);
                lastCharWasQuote = false;
            }
        }

        // Добавляем последнее поле
        String lastValue = current.toString();
        if (lastValue.startsWith("\"") && lastValue.endsWith("\"")) {
            lastValue = lastValue.substring(1, lastValue.length() - 1);
        }
        result.add(lastValue);

        return result.toArray(new String[0]);
    }

    private double parseDouble(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0.0;
        }

        String cleaned = value.trim()
                .replace(',', '.')
                .replace(" ", "")
                .replace("\"", "");

        // Убираем лишние символы
        cleaned = cleaned.replaceAll("[^\\d.-]", "");

        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            System.err.println("Не удалось преобразовать в число: '" + value + "' -> '" + cleaned + "'");
            return 0.0;
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String cleaned = value.trim()
                .replace("\"", "")
                .replace("'", "")
                .replace("  ", " ")
                .trim();

        // Убираем временную зону если она есть
        if (cleaned.contains("+")) {
            cleaned = cleaned.substring(0, cleaned.indexOf("+"));
        }
        if (cleaned.contains("-")) {
            int lastDash = cleaned.lastIndexOf("-");
            if (lastDash > 10) { // Если это не часть даты
                String potentialTimeZone = cleaned.substring(lastDash + 1);
                if (potentialTimeZone.length() <= 5 && potentialTimeZone.matches("\\d{2}:?\\d{2}")) {
                    cleaned = cleaned.substring(0, lastDash);
                }
            }
        }

        // Удаляем лишние пробелы вокруг 'T' и ':'
        cleaned = cleaned.replace(" T", "T")
                .replace("T ", "T")
                .replace(" :", ":")
                .replace(": ", ":");

        // Расширенный список форматов
        List<DateTimeFormatter> formatters = Arrays.asList(
                // Стандартные ISO форматы
                DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),

                // Форматы с пробелом вместо T
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),

                // Альтернативные разделители дат
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"),
                DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"),

                // Без секунд
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),

                // Только дата
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd"),
                DateTimeFormatter.ofPattern("dd.MM.yyyy"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy"),

                // Специфические форматы из реальных данных
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX"),
                DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy")
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                if (formatter.toString().contains("HH:mm")) {
                    // Форматы с временем
                    return LocalDateTime.parse(cleaned, formatter);
                } else {
                    // Форматы только с датой
                    try {
                        return LocalDateTime.parse(cleaned + "T00:00:00",
                                DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    } catch (DateTimeParseException e) {
                        // Пробуем с пробелом
                        return LocalDateTime.parse(cleaned + " 00:00:00",
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    }
                }
            } catch (DateTimeParseException e) {
                continue;
            }
        }

        // Обработать вручную сложные случаи
        try {
            // Пытаемся найти дату и время регулярными выражениями
            String dateTimePattern = "(\\d{4})[-/.](\\d{1,2})[-/.](\\d{1,2})[T ](\\d{1,2}):(\\d{1,2}):(\\d{1,2})";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(dateTimePattern);
            java.util.regex.Matcher matcher = pattern.matcher(cleaned);

            if (matcher.find()) {
                int year = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                int day = Integer.parseInt(matcher.group(3));
                int hour = Integer.parseInt(matcher.group(4));
                int minute = Integer.parseInt(matcher.group(5));
                int second = Integer.parseInt(matcher.group(6));

                return LocalDateTime.of(year, month, day, hour, minute, second);
            }

            // Только дата
            String datePattern = "(\\d{4})[-/.](\\d{1,2})[-/.](\\d{1,2})";
            pattern = java.util.regex.Pattern.compile(datePattern);
            matcher = pattern.matcher(cleaned);

            if (matcher.find()) {
                int year = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                int day = Integer.parseInt(matcher.group(3));

                return LocalDateTime.of(year, month, day, 0, 0, 0);
            }

        } catch (Exception e) {
            // Игнорируем, перейдем к следующему шагу
        }

        System.err.println("ВНИМАНИЕ: Не удалось распарсить время: '" + value + "' (очищено: '" + cleaned + "')");
        return null;
    }
}