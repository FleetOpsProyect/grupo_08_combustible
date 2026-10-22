package co.edu.univalle.fleetops.fuel;

import co.edu.univalle.fleetops.fuel.domain.FuelRecord;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

/** Shared test data with realistic values of the fuel domain. */
public final class FuelTestData {

    public static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    public static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-10-22T15:00:00Z"), BOGOTA);
    public static final OffsetDateTime NOW = OffsetDateTime.now(FIXED_CLOCK);

    public static final UUID FUEL_RECORD_ID = UUID.fromString("f0000000-0000-4000-8000-000000000001");
    public static final UUID ASSIGNMENT_ID = UUID.fromString("a1111111-1111-4111-8111-111111111111");
    public static final UUID VEHICLE_ID = UUID.fromString("b1111111-1111-4111-8111-111111111111");
    public static final UUID DRIVER_ID = UUID.fromString("c1111111-1111-4111-8111-111111111111");
    public static final UUID ROUTE_ID = UUID.fromString("d1111111-1111-4111-8111-111111111111");

    private FuelTestData() {
    }

    public static BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }

    /** A full-tank record of 12.5 gallons at 45210.0 km with the given efficiency and anomaly flag. */
    public static FuelRecord fuelRecord(BigDecimal efficiency, boolean anomalous) {
        return new FuelRecord(FUEL_RECORD_ID, ASSIGNMENT_ID, VEHICLE_ID, DRIVER_ID, ROUTE_ID,
                decimal("12.500"), decimal("187500.00"), decimal("45210.0"), true, efficiency, anomalous, NOW);
    }
}
