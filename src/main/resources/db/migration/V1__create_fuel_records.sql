-- Fuel records: one row per refueling made during a route in progress
CREATE TABLE fuel_records (
    id                        UUID           PRIMARY KEY,
    assignment_id             UUID           NOT NULL,
    vehicle_id                UUID           NOT NULL,
    driver_id                 UUID           NOT NULL,
    route_id                  UUID           NOT NULL,
    gallons_purchased         NUMERIC(7, 3)  NOT NULL CHECK (gallons_purchased > 0),
    total_cost                NUMERIC(12, 2) NOT NULL CHECK (total_cost > 0),
    odometer_km               NUMERIC(10, 1) NOT NULL CHECK (odometer_km > 0),
    full_tank                 BOOLEAN        NOT NULL,
    efficiency_km_per_gallon  NUMERIC(8, 2),
    anomalous_consumption     BOOLEAN        NOT NULL DEFAULT FALSE,
    recorded_at               TIMESTAMPTZ    NOT NULL
);

CREATE INDEX idx_fuel_records_vehicle_odometer ON fuel_records (vehicle_id, odometer_km);
CREATE INDEX idx_fuel_records_assignment ON fuel_records (assignment_id);
CREATE INDEX idx_fuel_records_recorded_at ON fuel_records (recorded_at);
