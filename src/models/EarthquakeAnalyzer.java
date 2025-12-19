package models;

import java.util.*;
import java.util.stream.Collectors;

public class EarthquakeAnalyzer {
    private List<Earthquake> earthquakes;

    public EarthquakeAnalyzer() {
        this.earthquakes = new ArrayList<>();
    }

    public void addEarthquake(Earthquake earthquake) {
        earthquakes.add(earthquake);
    }

    public List<Earthquake> getEarthquakes() {
        return earthquakes;
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();

        stats.put("Всего землетрясений", earthquakes.size());

        if (!earthquakes.isEmpty()) {
            // Статистика по магнитудам
            DoubleSummaryStatistics magnitudeStats = earthquakes.stream()
                    .mapToDouble(Earthquake::getMagnitude)
                    .summaryStatistics();

            // Статистика по глубине
            DoubleSummaryStatistics depthStats = earthquakes.stream()
                    .filter(eq -> eq.getDepth() > 0)
                    .mapToDouble(Earthquake::getDepth)
                    .summaryStatistics();

            // Статистика по времени
            long withTime = earthquakes.stream()
                    .filter(eq -> eq.getTime() != null)
                    .count();

            // Самые частые штаты (с нормализацией регистра)
            Map<String, Long> stateCounts = earthquakes.stream()
                    .filter(eq -> eq.getState() != null && !eq.getState().isEmpty())
                    .collect(Collectors.groupingBy(
                            eq -> {
                                String state = eq.getState();
                                String[] parts = state.split(",");
                                // Нормализуем регистр: первая буква заглавная, остальные строчные
                                String normalized = normalizeStateName(parts[0].trim());
                                return normalized;
                            },
                            Collectors.counting()));

            String topState = stateCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("Нет данных");

            // Заполняем статистику
            stats.put("Средняя магнитуда", magnitudeStats.getAverage());
            stats.put("Максимальная магнитуда", magnitudeStats.getMax());
            stats.put("Минимальная магнитуда", magnitudeStats.getMin());
            stats.put("Средняя глубина (м)", depthStats.getCount() > 0 ? depthStats.getAverage() : 0);
            stats.put("Максимальная глубина (м)", depthStats.getCount() > 0 ? depthStats.getMax() : 0);
            stats.put("С временем", withTime);
            stats.put("Без времени", earthquakes.size() - withTime);
            stats.put("Процент с временем", earthquakes.size() > 0 ?
                    String.format("%.1f%%", (withTime * 100.0 / earthquakes.size())) : "0%");
            stats.put("Уникальных штатов", stateCounts.size());
            stats.put("Самый частый штат", topState);

            // Диапазон годов, если есть время
            if (withTime > 0) {
                Optional<Earthquake> oldest = earthquakes.stream()
                        .filter(eq -> eq.getTime() != null)
                        .min(Comparator.comparing(Earthquake::getTime));
                Optional<Earthquake> newest = earthquakes.stream()
                        .filter(eq -> eq.getTime() != null)
                        .max(Comparator.comparing(Earthquake::getTime));

                if (oldest.isPresent() && newest.isPresent()) {
                    stats.put("Период данных",
                            oldest.get().getTime().getYear() + " - " +
                                    newest.get().getTime().getYear());
                }
            }
        }

        return stats;
    }

    public Map<String, Long> getEarthquakeCountByState() {
        return earthquakes.stream()
                .filter(eq -> eq.getState() != null && !eq.getState().isEmpty())
                .collect(Collectors.groupingBy(
                        eq -> {
                            String state = eq.getState();
                            String[] parts = state.split(",");
                            String cleanState = parts[0].trim();
                            // Нормализуем имя штата: правильный регистр
                            cleanState = normalizeStateName(cleanState);
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
    }

    // Нормализация имени штата
    private String normalizeStateName(String stateName) {
        if (stateName == null || stateName.isEmpty()) {
            return "";
        }

        String lower = stateName.toLowerCase().trim();

        // Специальная обработка для составных названий штатов
        if (lower.contains("new ")) {
            // Для штатов с "New" - первая буква каждого слова заглавная
            return Arrays.stream(lower.split("\\s+"))
                    .map(word -> {
                        if (word.length() > 0) {
                            return Character.toUpperCase(word.charAt(0)) + word.substring(1);
                        }
                        return word;
                    })
                    .collect(Collectors.joining(" "));
        } else if (lower.contains("north ") || lower.contains("south ") ||
                lower.contains("west ") || lower.contains("east ")) {
            // Для направлений (North, South, etc.)
            return Arrays.stream(lower.split("\\s+"))
                    .map(word -> {
                        if (word.length() > 0) {
                            return Character.toUpperCase(word.charAt(0)) + word.substring(1);
                        }
                        return word;
                    })
                    .collect(Collectors.joining(" "));
        } else {
            // Для обычных штатов - первая буква заглавная, остальные строчные
            if (lower.length() > 0) {
                return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
            }
            return lower;
        }
    }

    public Map<String, Long> getMagnitudeDistribution() {
        Map<String, Long> distribution = earthquakes.stream()
                .collect(Collectors.groupingBy(
                        eq -> {
                            double mag = eq.getMagnitude();
                            if (mag < 2.0) return "< 2.0";
                            else if (mag < 3.0) return "2.0 - 2.9";
                            else if (mag < 4.0) return "3.0 - 3.9";
                            else if (mag < 5.0) return "4.0 - 4.9";
                            else if (mag < 6.0) return "5.0 - 5.9";
                            else return ">= 6.0";
                        },
                        Collectors.counting()));

        // Сортируем по ключам
        return distribution.entrySet().stream()
                .sorted((a, b) -> {
                    String[] order = {"< 2.0", "2.0 - 2.9", "3.0 - 3.9", "4.0 - 4.9", "5.0 - 5.9", ">= 6.0"};
                    int indexA = Arrays.asList(order).indexOf(a.getKey());
                    int indexB = Arrays.asList(order).indexOf(b.getKey());
                    return Integer.compare(indexA, indexB);
                })
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (v1, v2) -> v1,
                        LinkedHashMap::new));
    }

    public Map<String, Long> getDepthDistribution() {
        return earthquakes.stream()
                .filter(eq -> eq.getDepth() > 0)
                .collect(Collectors.groupingBy(
                        eq -> {
                            double depth = eq.getDepth();
                            if (depth < 5000) return "Мелкие (< 5 км)";
                            else if (depth < 10000) return "Средние (5-10 км)";
                            else if (depth < 20000) return "Глубокие (10-20 км)";
                            else if (depth < 50000) return "Очень глубокие (20-50 км)";
                            else return "Экстремальные (> 50 км)";
                        },
                        Collectors.counting()))
                .entrySet().stream()
                .sorted((a, b) -> {
                    String[] order = {
                            "Мелкие (< 5 км)", "Средние (5-10 км)", "Глубокие (10-20 км)",
                            "Очень глубокие (20-50 км)", "Экстремальные (> 50 км)"
                    };
                    int indexA = Arrays.asList(order).indexOf(a.getKey());
                    int indexB = Arrays.asList(order).indexOf(b.getKey());
                    return Integer.compare(indexA, indexB);
                })
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (v1, v2) -> v1,
                        LinkedHashMap::new));
    }

    public Map<String, Long> getYearDistribution() {
        return earthquakes.stream()
                .filter(eq -> eq.getTime() != null)
                .collect(Collectors.groupingBy(
                        eq -> String.valueOf(eq.getTime().getYear()),
                        Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (v1, v2) -> v1,
                        LinkedHashMap::new));
    }

    public Map<String, Long> getMonthDistribution(int year) {
        return earthquakes.stream()
                .filter(eq -> eq.getTime() != null && eq.getTime().getYear() == year)
                .collect(Collectors.groupingBy(
                        eq -> String.format("%02d", eq.getTime().getMonthValue()),
                        Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (v1, v2) -> v1,
                        LinkedHashMap::new));
    }

    public List<Earthquake> getTopByMagnitude(int limit) {
        return earthquakes.stream()
                .sorted((a, b) -> Double.compare(b.getMagnitude(), a.getMagnitude()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<Earthquake> getTopByDepth(int limit) {
        return earthquakes.stream()
                .filter(eq -> eq.getDepth() > 0)
                .sorted((a, b) -> Double.compare(b.getDepth(), a.getDepth()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    // Получить статистику по штатам с нормализацией
    public Map<String, Long> getStateStatistics() {
        return earthquakes.stream()
                .filter(eq -> eq.getState() != null && !eq.getState().isEmpty())
                .collect(Collectors.groupingBy(
                        eq -> {
                            String state = eq.getState();
                            String[] parts = state.split(",");
                            String cleanState = parts[0].trim();
                            // Нормализуем имя штата
                            return normalizeStateName(cleanState);
                        },
                        Collectors.counting()))
                .entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (v1, v2) -> v1,
                        LinkedHashMap::new));
    }
}