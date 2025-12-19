package visualization;

import java.util.Map;

public class TextChartGenerator {

    public static void printBarChart(String title, Map<String, Number> data,
                                     String categoryLabel, String valueLabel) {
        System.out.println("\n" + title);
        System.out.println("=".repeat(60));
        System.out.printf("%-30s | %-20s | %s%n", categoryLabel, valueLabel, "График");
        System.out.println("-".repeat(60));

        double maxValue = data.values().stream()
                .mapToDouble(Number::doubleValue)
                .max()
                .orElse(1.0);

        int maxBarLength = 50;

        for (Map.Entry<String, Number> entry : data.entrySet()) {
            String category = entry.getKey();
            double value = entry.getValue().doubleValue();
            int barLength = (int) ((value / maxValue) * maxBarLength);

            String bar = "█".repeat(Math.max(0, barLength));
            System.out.printf("%-30s | %-20.2f | %s%n",
                    truncate(category, 30), value, bar);
        }
    }

    public static void printPieChart(String title, Map<String, Number> data) {
        System.out.println("\n" + title);
        System.out.println("=".repeat(60));

        double total = data.values().stream()
                .mapToDouble(Number::doubleValue)
                .sum();

        if (total == 0) {
            System.out.println("Нет данных для отображения");
            return;
        }

        System.out.printf("%-30s | %-10s | %-10s | %s%n",
                "Категория", "Значение", "Процент", "Доля");
        System.out.println("-".repeat(60));

        for (Map.Entry<String, Number> entry : data.entrySet()) {
            String category = entry.getKey();
            double value = entry.getValue().doubleValue();
            double percentage = (value / total) * 100;

            int dots = Math.max(1, (int) (percentage / 2));
            String share = "•".repeat(dots);

            System.out.printf("%-30s | %-10.2f | %-10.1f%% | %s%n",
                    truncate(category, 30), value, percentage, share);
        }
    }

    public static void printStatisticsTable(Map<String, Object> data, String title) {
        System.out.println("\n" + title);
        System.out.println("=".repeat(60));
        System.out.printf("%-40s | %-15s%n", "Параметр", "Значение");
        System.out.println("-".repeat(60));

        data.forEach((key, value) -> {
            if (value instanceof Number) {
                System.out.printf("%-40s | %-15.2f%n", key, ((Number) value).doubleValue());
            } else {
                System.out.printf("%-40s | %-15s%n", key, value != null ? value.toString() : "null");
            }
        });
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, Math.min(text.length(), maxLength - 3)) + "...";
    }
}