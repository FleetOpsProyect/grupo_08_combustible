package co.edu.univalle.fleetops.fuel.domain;

import java.math.BigDecimal;

/**
 * Strategy (GoF): decides whether an efficiency is an anomalous consumption for a vehicle.
 */
public interface AnomalyRule {

    /**
     * Evaluates an efficiency against the vehicle history.
     *
     * @param efficiencyKmPerGallon efficiency of the current full tank
     * @param average               average of the previous non anomalous efficiencies
     * @return true when the consumption is anomalous
     */
    boolean isAnomalous(BigDecimal efficiencyKmPerGallon, VehicleFuelAverage average);
}
