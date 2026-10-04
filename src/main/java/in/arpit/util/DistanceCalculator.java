package in.arpit.util;

/**
 * Pure geographic math. Stateless, no Spring dependencies, so it is trivially unit-testable
 * and is the ONLY place the Haversine formula lives.
 */
public final class DistanceCalculator {

    /** Mean Earth radius in kilometres. */
    public static final double EARTH_RADIUS_KM = 6371.0088;

    /** Approximate length of one degree of latitude in km. */
    public static final double KM_PER_DEGREE_LAT = 111.32;

    private DistanceCalculator() {
    }

    /** Great-circle distance in km between two WGS84 points (Haversine formula). */
    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dPhi = Math.toRadians(lat2 - lat1);
        double dLambda = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dPhi / 2) * Math.sin(dPhi / 2)
                + Math.cos(phi1) * Math.cos(phi2) * Math.sin(dLambda / 2) * Math.sin(dLambda / 2);
        // clamp guards against floating-point drift pushing 'a' marginally above 1
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(Math.max(0.0, 1 - a)));
        return EARTH_RADIUS_KM * c;
    }

    /**
     * A rectangle guaranteed to contain every point within radiusKm of the centre. Used only as
     * a cheap database pre-filter; exact distance is still decided by haversineKm.
     * If the box would wrap the antimeridian or reach a pole, longitude is left unbounded.
     */
    public static BoundingBox boundingBox(double lat, double lon, double radiusKm) {
        double dLat = radiusKm / KM_PER_DEGREE_LAT;
        double minLat = Math.max(-90.0, lat - dLat);
        double maxLat = Math.min(90.0, lat + dLat);

        double minLon = -180.0;
        double maxLon = 180.0;
        double cos = Math.cos(Math.toRadians(lat));
        if (maxLat < 90.0 && minLat > -90.0 && cos > 1e-6) {
            double dLon = radiusKm / (KM_PER_DEGREE_LAT * cos);
            if (lon - dLon >= -180.0 && lon + dLon <= 180.0) {
                minLon = lon - dLon;
                maxLon = lon + dLon;
            }
        }
        return new BoundingBox(minLat, maxLat, minLon, maxLon);
    }

    public record BoundingBox(double minLat, double maxLat, double minLon, double maxLon) {
    }
}
