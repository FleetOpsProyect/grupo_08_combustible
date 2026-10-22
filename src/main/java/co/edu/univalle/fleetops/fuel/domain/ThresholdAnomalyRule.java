package co.edu.univalle.fleetops.fuel.domain;

import java.math.BigDecimal;

/**
 * Strategy implementation (G08 decision, 15 Oct 2026): a consumption is anomalous when the efficiency
 * drops more than the threshold against the vehicle average, and only when the vehicle already has the
 * minimum number of previous non anomalous efficiencies. Both values come from configuration.
 */
public class ThresholdAnomalyRule implements AnomalyRule {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final BigDecimal thresholdPercent;
    private final int minimumSamples;

    /**
     * Creates the rule.
     *
     * @param thresholdPercent maximum allowed drop, in percent (for example 30)
     * @param minimumSamples   previous non anomalous efficiencies needed to evaluate (for example 2)
     */
    public ThresholdAnomalyRule(BigDecimal thresholdPercent, int minimumSamples) {
        if (thresholdPercent == null || thresholdPercent.signum() <= 0
                || thresholdPercent.compareTo(ONE_HUNDRED) >= 0) {
            throw new IllegalArgumentException("thresholdPercent must be between 0 and 100");
        }
        if (minimumSamples < 1) {
            throw new IllegalArgumentException("minimumSamples must be at least 1");
        }
        this.thresholdPercent = thresholdPercent;
        this.minimumSamples = minimumSamples;
    }

    @Override
    public boolean isAnomalous(BigDecimal efficiencyKmPerGallon, VehicleFuelAverage average) {
        if (average.sampleCount() < minimumSamples) {
            return false;
        }
        BigDecimal remainingRatio = ONE_HUNDRED.subtract(thresholdPercent).divide(ONE_HUNDRED);
        BigDecimal lowerLimit = average.averageKmPerGallon().multiply(remainingRatio);
        return efficiencyKmPerGallon.compareTo(lowerLimit) < 0;
    }
}
