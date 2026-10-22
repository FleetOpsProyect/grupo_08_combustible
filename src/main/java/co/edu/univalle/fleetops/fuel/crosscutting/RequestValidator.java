package co.edu.univalle.fleetops.fuel.crosscutting;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Validator pattern (Usability, protection against user errors): checks the list parameters before the use
 * case. The body of POST is validated with Bean Validation annotations on its DTO.
 */
@Component
public class RequestValidator {

    static final int MAX_PAGE_SIZE = 100;

    /**
     * Validates the paging and the date range of the list.
     *
     * @param page     page number, from 0
     * @param size     page size, from 1 to 100
     * @param dateFrom optional start of the range
     * @param dateTo   optional end of the range
     */
    public void validateSearch(int page, int size, OffsetDateTime dateFrom, OffsetDateTime dateTo) {
        List<FieldViolation> violations = new ArrayList<>();
        if (page < 0) {
            violations.add(new FieldViolation("page", "MIN_VALUE"));
        }
        if (size < 1) {
            violations.add(new FieldViolation("size", "MIN_VALUE"));
        }
        if (size > MAX_PAGE_SIZE) {
            violations.add(new FieldViolation("size", "MAX_VALUE"));
        }
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            violations.add(new FieldViolation("date_from", "AFTER_DATE_TO"));
        }
        if (!violations.isEmpty()) {
            throw new RequestValidationException(violations);
        }
    }
}
