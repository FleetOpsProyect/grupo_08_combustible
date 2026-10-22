-- Running average of the non anomalous efficiencies of each vehicle (sum and count)
CREATE TABLE vehicle_fuel_averages (
    vehicle_id      UUID           PRIMARY KEY,
    efficiency_sum  NUMERIC(14, 2) NOT NULL,
    sample_count    INTEGER        NOT NULL CHECK (sample_count >= 0),
    version         BIGINT         NOT NULL DEFAULT 0
);
