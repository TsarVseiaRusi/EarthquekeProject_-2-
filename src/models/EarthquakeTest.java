package models;

import java.time.LocalDateTime;

public class EarthquakeTest {

    public static void main(String[] args) {
        System.out.println("=== Запуск тестов Earthquake ===");

        try {
            testConstructorAndGetters();
            testSettersAndGetters();
            testToString();
            System.out.println("✓ Все тесты Earthquake пройдены успешно!");
        } catch (AssertionError e) {
            System.err.println("✗ Тест провален: " + e.getMessage());
        }
    }

    private static void testConstructorAndGetters() {
        LocalDateTime time = LocalDateTime.of(2023, 12, 15, 14, 30, 0);
        Earthquake earthquake = new Earthquake("test123", 10.5, "Mw", 4.7, "California", time);

        assert earthquake.getId().equals("test123") : "ID не совпадает";
        assert earthquake.getDepth() == 10.5 : "Глубина не совпадает";
        assert earthquake.getMagnitudeType().equals("Mw") : "Тип магнитуды не совпадает";
        assert earthquake.getMagnitude() == 4.7 : "Магнитуда не совпадает";
        assert earthquake.getState().equals("California") : "Штат не совпадает";
        assert earthquake.getTime().equals(time) : "Время не совпадает";

        System.out.println("  ✓ testConstructorAndGetters пройден");
    }

    private static void testSettersAndGetters() {
        Earthquake earthquake = new Earthquake();
        LocalDateTime time = LocalDateTime.now();

        earthquake.setId("setterTest");
        earthquake.setDepth(15.3);
        earthquake.setMagnitudeType("Mb");
        earthquake.setMagnitude(3.8);
        earthquake.setState("New York");
        earthquake.setTime(time);

        assert earthquake.getId().equals("setterTest") : "ID сеттера не совпадает";
        assert earthquake.getDepth() == 15.3 : "Глубина сеттера не совпадает";
        assert earthquake.getMagnitudeType().equals("Mb") : "Тип магнитуды сеттера не совпадает";
        assert earthquake.getMagnitude() == 3.8 : "Магнитуда сеттера не совпадает";
        assert earthquake.getState().equals("New York") : "Штат сеттера не совпадает";
        assert earthquake.getTime().equals(time) : "Время сеттера не совпадает";

        System.out.println("  ✓ testSettersAndGetters пройден");
    }

    private static void testToString() {
        LocalDateTime time = LocalDateTime.of(2023, 12, 15, 14, 30, 0);
        Earthquake earthquake = new Earthquake("id123", 10.5, "Mw", 4.7, "California", time);

        String result = earthquake.toString();
        assert result.contains("id123") : "toString не содержит ID";
        assert result.contains("4.7") : "toString не содержит магнитуду";
        assert result.contains("California") : "toString не содержит штат";

        System.out.println("  ✓ testToString пройден");
    }
}