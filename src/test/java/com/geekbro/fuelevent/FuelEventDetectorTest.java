package com.geekbro.fuelevent;

import com.geekbro.fuelevent.config.FuelEventDetectionProperties;
import com.geekbro.fuelevent.detector.FuelEventDetector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FuelEventDetectorTest {

    private FuelEventDetector detector;

    @BeforeEach
    void setUp() {
        FuelEventDetectionProperties props = new FuelEventDetectionProperties();
        detector = new FuelEventDetector(props);
    }

    @Test
    void ignoresSingleSampleGlitch() {
        List<Double> readings = toList(82, 82, 82, 30, 82, 82, 82);

        assertEquals(List.of(), detector.detectEvents(readings, 10));
    }

    @Test
    void detectsSimpleTheft() {
        List<Double> readings = toList(12, 12, 12, 12, 12, 6, 6, 6, 6);

        assertEquals(List.of(-6.0), detector.detectEvents(readings, 5));
    }

    @Test
    void detectsSimpleRefuel() {
        List<Double> readings = toList(5, 5, 5, 5, 5, 16, 16, 16, 16);

        assertEquals(List.of(11.0), detector.detectEvents(readings, 5));
    }

    @Test
    void detectsGradualRampAsSingleEvent() {
        List<Double> readings = toList(13, 14, 15, 15, 23, 34, 46, 46, 47, 49, 49);

        List<Double> events = detector.detectEvents(readings, 5);
        assertEquals(1, events.size());
        assertEquals(36.0, events.get(0), 2.0);
    }

    @Test
    void detectsMultipleMixedEventsInOrder() {
        List<Double> readings = toList(
                100, 100, 100, 100,
                70, 70, 70, 70,
                120, 120, 120, 120,
                180, 180, 180, 180
        );

        assertEquals(List.of(-30.0, 50.0, 60.0), detector.detectEvents(readings, 5));
    }

    @Test
    void returnsEmptyListWhenNoEvents() {
        List<Double> readings = toList(50, 50, 51, 50, 49, 50, 50, 51);

        assertEquals(List.of(), detector.detectEvents(readings, 5));
    }

    @Test
    void handlesNullAndTooShortInput() {
        assertEquals(List.of(), detector.detectEvents(null, 5));
        assertEquals(List.of(), detector.detectEvents(toList(42), 5));
    }

    private static List<Double> toList(double... values) {
        return Arrays.stream(values).boxed().toList();
    }
}
