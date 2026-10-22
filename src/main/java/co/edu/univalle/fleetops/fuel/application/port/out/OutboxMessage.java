package co.edu.univalle.fleetops.fuel.application.port.out;

import java.util.UUID;

/**
 * Event waiting in the outbox. The payload is the full contract message, already serialized by the adapter.
 *
 * @param eventId   unique id, used by consumers to deduplicate
 * @param eventType contract name of the event
 * @param payload   JSON message (standard envelope)
 */
public record OutboxMessage(UUID eventId, String eventType, String payload) {
}
