package co.edu.univalle.fleetops.fuel.adapter.in.rest;

import co.edu.univalle.fleetops.fuel.adapter.in.rest.dto.FuelRecordPageResponse;
import co.edu.univalle.fleetops.fuel.adapter.in.rest.dto.FuelRecordResponse;
import co.edu.univalle.fleetops.fuel.adapter.in.rest.dto.RegisterFuelRecordRequest;
import co.edu.univalle.fleetops.fuel.application.port.in.RegisterFuelRecordCommand;
import co.edu.univalle.fleetops.fuel.application.query.FuelRecordPage;
import co.edu.univalle.fleetops.fuel.application.query.FuelRecordSummary;
import co.edu.univalle.fleetops.fuel.domain.FuelRecord;

/** Mapper between the REST DTOs and the application model. Keeps the controller free of conversions. */
final class FuelRecordRestMapper {

    private FuelRecordRestMapper() {
    }

    static RegisterFuelRecordCommand toCommand(RegisterFuelRecordRequest request) {
        return new RegisterFuelRecordCommand(request.assignmentId(), request.gallonsPurchased(),
                request.totalCost(), request.odometerKm(), request.fullTank());
    }

    static FuelRecordResponse toResponse(FuelRecord fuelRecord) {
        return new FuelRecordResponse(fuelRecord.id(), fuelRecord.assignmentId(), fuelRecord.vehicleId(),
                fuelRecord.driverId(), fuelRecord.routeId(), fuelRecord.gallonsPurchased(), fuelRecord.totalCost(),
                fuelRecord.odometerKm(), fuelRecord.fullTank(), fuelRecord.efficiencyKmPerGallon(),
                fuelRecord.anomalousConsumption(), fuelRecord.recordedAt());
    }

    static FuelRecordPageResponse toResponse(FuelRecordPage page) {
        FuelRecordSummary summary = page.summary();
        return new FuelRecordPageResponse(
                page.content().stream().map(FuelRecordRestMapper::toResponse).toList(),
                page.page(),
                page.size(),
                page.total(),
                new FuelRecordPageResponse.Summary(summary.totalCost(), summary.totalGallons(),
                        summary.anomalousRecords(), summary.totalRecords()));
    }
}
