package co.edu.univalle.fleetops.fuel.adapter.in.rest;

import co.edu.univalle.fleetops.fuel.adapter.in.rest.dto.FuelRecordPageResponse;
import co.edu.univalle.fleetops.fuel.adapter.in.rest.dto.FuelRecordResponse;
import co.edu.univalle.fleetops.fuel.adapter.in.rest.dto.RegisterFuelRecordRequest;
import co.edu.univalle.fleetops.fuel.application.port.in.QueryFuelRecordsUseCase;
import co.edu.univalle.fleetops.fuel.application.port.in.RegisterFuelRecordUseCase;
import co.edu.univalle.fleetops.fuel.application.query.FuelRecordFilter;
import co.edu.univalle.fleetops.fuel.application.query.PageQuery;
import co.edu.univalle.fleetops.fuel.crosscutting.RequestValidator;
import co.edu.univalle.fleetops.fuel.domain.FuelRecord;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inbound adapter (REST): translates HTTP into calls to the inbound ports. No business rules here.
 */
@RestController
@RequestMapping(FuelRecordController.BASE_PATH)
@Tag(name = "Fuel records", description = "Refuelings, efficiency and anomalous consumption")
public class FuelRecordController {

    static final String BASE_PATH = "/fuel_records";

    private final RegisterFuelRecordUseCase registerFuelRecord;
    private final QueryFuelRecordsUseCase queryFuelRecords;
    private final RequestValidator requestValidator;

    /**
     * Creates the controller.
     *
     * @param registerFuelRecord inbound port to register
     * @param queryFuelRecords   inbound port to read
     * @param requestValidator   validator of the list parameters
     */
    public FuelRecordController(RegisterFuelRecordUseCase registerFuelRecord,
                                QueryFuelRecordsUseCase queryFuelRecords,
                                RequestValidator requestValidator) {
        this.registerFuelRecord = registerFuelRecord;
        this.queryFuelRecords = queryFuelRecords;
        this.requestValidator = requestValidator;
    }

    @PostMapping
    @Operation(summary = "Register a refueling of a route in progress (Driver)")
    ResponseEntity<FuelRecordResponse> register(@Valid @RequestBody RegisterFuelRecordRequest request) {
        FuelRecord created = registerFuelRecord.register(FuelRecordRestMapper.toCommand(request));
        return ResponseEntity.created(URI.create(BASE_PATH + "/" + created.id()))
                .body(FuelRecordRestMapper.toResponse(created));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one fuel record")
    FuelRecordResponse findById(@PathVariable("id") UUID id) {
        return FuelRecordRestMapper.toResponse(queryFuelRecords.findById(id));
    }

    @GetMapping
    @Operation(summary = "List fuel records with filters and the summary block (Fleet Administrator, Reports)")
    FuelRecordPageResponse search(
            @RequestParam(name = "vehicle_id", required = false) UUID vehicleId,
            @RequestParam(name = "assignment_id", required = false) UUID assignmentId,
            @RequestParam(name = "route_id", required = false) UUID routeId,
            @RequestParam(name = "anomalous_consumption", required = false) Boolean anomalousConsumption,
            @RequestParam(name = "date_from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateFrom,
            @RequestParam(name = "date_to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateTo,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        requestValidator.validateSearch(page, size, dateFrom, dateTo);
        FuelRecordFilter filter = new FuelRecordFilter(vehicleId, assignmentId, routeId, anomalousConsumption,
                dateFrom, dateTo);
        return FuelRecordRestMapper.toResponse(queryFuelRecords.search(filter, new PageQuery(page, size)));
    }
}
