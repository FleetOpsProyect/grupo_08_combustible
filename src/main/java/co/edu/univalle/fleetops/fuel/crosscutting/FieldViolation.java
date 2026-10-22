package co.edu.univalle.fleetops.fuel.crosscutting;

/**
 * One invalid field of a 400 response.
 *
 * @param field contract name of the field (snake_case)
 * @param code  machine readable reason, for example NOT_NULL or MAX_VALUE
 */
public record FieldViolation(String field, String code) {
}
