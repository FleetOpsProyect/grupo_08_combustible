package co.edu.univalle.fleetops.fuel.crosscutting;

import co.edu.univalle.fleetops.fuel.application.exception.AssignmentNotFoundException;
import co.edu.univalle.fleetops.fuel.application.exception.AssignmentNotInProgressException;
import co.edu.univalle.fleetops.fuel.application.exception.DependencyUnavailableException;
import co.edu.univalle.fleetops.fuel.application.exception.FuelRecordNotFoundException;
import co.edu.univalle.fleetops.fuel.application.exception.RouteNotInProgressException;
import co.edu.univalle.fleetops.fuel.domain.exception.InvalidFuelRecordException;
import co.edu.univalle.fleetops.fuel.domain.exception.OdometerNotIncreasingException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Translates every exception to the common error format and HTTP code of the conventions:
 * 400 invalid data, 404 not found, 409 state does not allow it, 422 business rule, 503 dependency down.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String VALIDATION_ERROR = "VALIDATION_ERROR";

    private final Clock clock;

    /**
     * Creates the handler.
     *
     * @param clock clock in the Colombia time zone, for the timestamp
     */
    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> invalidBody(MethodArgumentNotValidException error, HttpServletRequest request) {
        List<FieldViolation> violations = error.getBindingResult().getFieldErrors().stream()
                .map(field -> new FieldViolation(toSnakeCase(field.getField()), toSnakeCase(field.getCode())
                        .toUpperCase(Locale.ROOT)))
                .toList();
        return build(HttpStatus.BAD_REQUEST, VALIDATION_ERROR, "One or more fields are invalid", request, violations);
    }

    @ExceptionHandler(RequestValidationException.class)
    ResponseEntity<ErrorResponse> invalidParameters(RequestValidationException error, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, VALIDATION_ERROR, error.getMessage(), request, error.violations());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorResponse> wrongType(MethodArgumentTypeMismatchException error, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, VALIDATION_ERROR, "A parameter has an invalid format", request,
                List.of(new FieldViolation(error.getName(), "INVALID_FORMAT")));
    }

    @ExceptionHandler(InvalidFuelRecordException.class)
    ResponseEntity<ErrorResponse> invalidRecord(InvalidFuelRecordException error, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, VALIDATION_ERROR, error.getMessage(), request,
                List.of(new FieldViolation(error.field(), "INVALID_VALUE")));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> unreadable(HttpMessageNotReadableException error, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "The request body is not valid JSON", request,
                List.of());
    }

    @ExceptionHandler(AssignmentNotFoundException.class)
    ResponseEntity<ErrorResponse> assignmentNotFound(AssignmentNotFoundException error, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "ASSIGNMENT_NOT_FOUND", error.getMessage(), request, List.of());
    }

    @ExceptionHandler(FuelRecordNotFoundException.class)
    ResponseEntity<ErrorResponse> recordNotFound(FuelRecordNotFoundException error, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "FUEL_RECORD_NOT_FOUND", error.getMessage(), request, List.of());
    }

    @ExceptionHandler(AssignmentNotInProgressException.class)
    ResponseEntity<ErrorResponse> assignmentNotInProgress(AssignmentNotInProgressException error,
                                                          HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "ASSIGNMENT_NOT_IN_PROGRESS", error.getMessage(), request, List.of());
    }

    @ExceptionHandler(RouteNotInProgressException.class)
    ResponseEntity<ErrorResponse> routeNotInProgress(RouteNotInProgressException error, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "ROUTE_NOT_IN_PROGRESS", error.getMessage(), request, List.of());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ErrorResponse> concurrentUpdate(OptimisticLockingFailureException error,
                                                   HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "CONCURRENT_UPDATE", "The vehicle was updated at the same time, retry",
                request, List.of());
    }

    @ExceptionHandler(OdometerNotIncreasingException.class)
    ResponseEntity<ErrorResponse> odometer(OdometerNotIncreasingException error, HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "ODOMETER_NOT_INCREASING", error.getMessage(), request,
                List.of());
    }

    @ExceptionHandler(DependencyUnavailableException.class)
    ResponseEntity<ErrorResponse> dependencyDown(DependencyUnavailableException error, HttpServletRequest request) {
        LOG.warn("Dependency unavailable: {}", error.getMessage());
        return build(HttpStatus.SERVICE_UNAVAILABLE, "DEPENDENCY_UNAVAILABLE", error.getMessage(), request,
                List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorResponse> noRoute(NoResourceFoundException error, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", "The requested path does not exist", request, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorResponse> wrongMethod(HttpRequestMethodNotSupportedException error,
                                              HttpServletRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", error.getMessage(), request, List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception error, HttpServletRequest request) {
        LOG.error("Unexpected error", error);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected error", request, List.of());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message,
                                                HttpServletRequest request, List<FieldViolation> violations) {
        ErrorResponse body = new ErrorResponse(code, message, OffsetDateTime.now(clock), request.getRequestURI(),
                violations);
        return ResponseEntity.status(status).body(body);
    }

    static String toSnakeCase(String value) {
        return value == null ? "" : value.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }
}
