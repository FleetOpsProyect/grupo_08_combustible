package co.edu.univalle.fleetops.fuel.crosscutting;

import static co.edu.univalle.fleetops.fuel.FuelTestData.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RequestValidatorTest {

    private final RequestValidator validator = new RequestValidator();

    @Test
    void should_accept_valid_page_and_range() {
        assertThatCode(() -> validator.validateSearch(0, 100, NOW.minusDays(1), NOW)).doesNotThrowAnyException();
        assertThatCode(() -> validator.validateSearch(2, 1, null, NOW)).doesNotThrowAnyException();
        assertThatCode(() -> validator.validateSearch(2, 1, NOW, null)).doesNotThrowAnyException();
    }

    @Test
    void should_report_every_invalid_parameter_at_once() {
        // Given / When / Then
        assertThatThrownBy(() -> validator.validateSearch(-1, 0, NOW, NOW.minusDays(1)))
                .isInstanceOfSatisfying(RequestValidationException.class, error ->
                        assertThat(error.violations()).containsExactly(
                                new FieldViolation("page", "MIN_VALUE"),
                                new FieldViolation("size", "MIN_VALUE"),
                                new FieldViolation("date_from", "AFTER_DATE_TO")));
    }

    @Test
    void should_convert_camel_case_to_snake_case() {
        assertThat(GlobalExceptionHandler.toSnakeCase("gallonsPurchased")).isEqualTo("gallons_purchased");
        assertThat(GlobalExceptionHandler.toSnakeCase("DecimalMin")).isEqualTo("decimal_min");
        assertThat(GlobalExceptionHandler.toSnakeCase(null)).isEmpty();
    }
}
