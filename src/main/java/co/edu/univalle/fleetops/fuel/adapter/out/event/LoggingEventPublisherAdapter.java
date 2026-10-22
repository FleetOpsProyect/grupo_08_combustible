package co.edu.univalle.fleetops.fuel.adapter.out.event;

import co.edu.univalle.fleetops.fuel.application.port.out.EventPublisherPort;
import co.edu.univalle.fleetops.fuel.application.port.out.OutboxMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * [Archetype Convention Addition] Temporary event publisher: writes the event to the log while the groups
 * choose the broker (RabbitMQ, Kafka or SNS with SQS). When it is chosen, a new adapter implements
 * {@link EventPublisherPort} with that library and this class is removed; the core does not change
 * (Ports and Adapters, Cockburn).
 */
@Component
public class LoggingEventPublisherAdapter implements EventPublisherPort {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingEventPublisherAdapter.class);

    @Override
    public void publish(OutboxMessage message) {
        LOG.info("Event published to log (broker pending): event_type={} event_id={} payload={}",
                message.eventType(), message.eventId(), message.payload());
    }
}
