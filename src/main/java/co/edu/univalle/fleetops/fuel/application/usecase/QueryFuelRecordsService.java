package co.edu.univalle.fleetops.fuel.application.usecase;

import co.edu.univalle.fleetops.fuel.application.exception.FuelRecordNotFoundException;
import co.edu.univalle.fleetops.fuel.application.port.in.QueryFuelRecordsUseCase;
import co.edu.univalle.fleetops.fuel.application.port.out.FuelRecordRepositoryPort;
import co.edu.univalle.fleetops.fuel.application.query.FuelRecordFilter;
import co.edu.univalle.fleetops.fuel.application.query.FuelRecordPage;
import co.edu.univalle.fleetops.fuel.application.query.PageQuery;
import co.edu.univalle.fleetops.fuel.domain.FuelRecord;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/** Use case: read fuel records (by id, and the filtered list with its summary). */
public class QueryFuelRecordsService implements QueryFuelRecordsUseCase {

    private final FuelRecordRepositoryPort fuelRecordRepository;

    /**
     * Creates the use case.
     *
     * @param fuelRecordRepository repository port
     */
    public QueryFuelRecordsService(FuelRecordRepositoryPort fuelRecordRepository) {
        this.fuelRecordRepository = fuelRecordRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public FuelRecord findById(UUID fuelRecordId) {
        return fuelRecordRepository.findById(fuelRecordId)
                .orElseThrow(() -> new FuelRecordNotFoundException(fuelRecordId));
    }

    @Override
    @Transactional(readOnly = true)
    public FuelRecordPage search(FuelRecordFilter filter, PageQuery page) {
        return fuelRecordRepository.search(filter, page);
    }
}
