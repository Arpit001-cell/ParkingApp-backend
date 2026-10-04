package in.arpit.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DistanceCalculatorTest {

    @Test
    void samePointIsZero() {
        assertEquals(0.0, DistanceCalculator.haversineKm(23.2599, 77.4126, 23.2599, 77.4126), 1e-9);
    }

    @Test
    void oneDegreeOfLongitudeAtEquator() {
        assertEquals(111.19, DistanceCalculator.haversineKm(0, 0, 0, 1), 0.05);
    }

    @Test
    void bhopalToDelhi() {
        assertEquals(595.7, DistanceCalculator.haversineKm(23.2599, 77.4126, 28.6139, 77.2090), 1.0);
    }

    @Test
    void antipodes() {
        assertEquals(20015.1, DistanceCalculator.haversineKm(0, 0, 0, 180), 1.0);
    }

    @Test
    void boundingBoxContainsRadiusAndIsUnboundedAcrossAntimeridian() {
        var box = DistanceCalculator.boundingBox(23.2599, 77.4126, 5);
        assertTrue(box.minLat() < 23.2599 && box.maxLat() > 23.2599);
        assertTrue(box.minLon() > -180 && box.maxLon() < 180);

        var wrap = DistanceCalculator.boundingBox(0, 179.99, 5);
        assertEquals(-180.0, wrap.minLon());
        assertEquals(180.0, wrap.maxLon());
    }
}
