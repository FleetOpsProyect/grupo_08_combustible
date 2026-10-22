package co.edu.univalle.fleetops.fuel.crosscutting;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Common error format of the shared conventions, in English and snake_case:
 * {"code", "message", "timestamp", "path", "errors": [{"field", "code"}]}. "errors" only appears on a 400.
 */
public record ErrorResponse(
        String code,
        String message,
        OffsetDateTime timestamp,
        String path,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<FieldViolation> errors) {
}
