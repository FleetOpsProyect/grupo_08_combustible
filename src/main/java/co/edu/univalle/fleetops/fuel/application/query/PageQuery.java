package co.edu.univalle.fleetops.fuel.application.query;

/**
 * Requested page.
 *
 * @param page page number, from 0
 * @param size page size, from 1 to 100
 */
public record PageQuery(int page, int size) {
}
