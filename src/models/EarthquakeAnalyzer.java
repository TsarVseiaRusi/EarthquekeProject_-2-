// EarthquakeAnalyzer.java
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
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalCount", earthquakes.size());

        if (!earthquakes.isEmpty()) {
            DoubleSummaryStatistics magnitudeStats = earthquakes.stream()
                    .mapToDouble(Earthquake::getMagnitude)
                    .summaryStatistics();

            DoubleSummaryStatistics depthStats = earthquakes.stream()
                    .filter(eq -> eq.getDepth() > 0)
                    .mapToDouble(Earthquake::getDepth)
                    .summaryStatistics();

            stats.put("avgMagnitude", magnitudeStats.getAverage());
            stats.put("maxMagnitude", magnitudeStats.getMax());
            stats.put("minMagnitude", magnitudeStats.getMin());
            stats.put("avgDepth", depthStats.getAverage());

            Set<String> states = earthquakes.stream()
                    .map(Earthquake::getState)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            stats.put("uniqueStates", states.size());
        }

        return stats;
    }

    // Дополнительные методы для получения данных в нужном формате
    public Map<String, Long> getEarthquakeCountByState() {
        return earthquakes.stream()
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
    }

    public Map<String, Long> getMagnitudeDistribution() {
        return earthquakes.stream()
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
                            else return "Очень глубокие (> 20 км)";
                        },
                        Collectors.counting()));
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
}