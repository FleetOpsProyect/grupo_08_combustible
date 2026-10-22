package co.edu.univalle.fleetops.fuel.application.usecase;

import static co.edu.univalle.fleetops.fuel.FuelTestData.FIXED_CLOCK;
import static co.edu.univalle.fleetops.fuel.FuelTestData.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.univalle.fleetops.fuel.application.exception.EventPublicationException;
import co.edu.univalle.fleetops.fuel.application.port.out.EventPublisherPort;
import co.edu.univalle.fleetops.fuel.application.port.out.OutboxMessage;
import co.edu.univalle.fleetops.fuel.application.port.out.OutboxPort;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RelayOutboxServiceTest {

    private static final String EVENT_TYPE = "fuel_anomaly_detected_event";

    @Mock
    private OutboxPort outboxPort;
    @Mock
    private EventPublisherPort eventPublisher;

    private final OutboxMessage first = new OutboxMessage(UUID.randomUUID(), EVENT_TYPE, "{\"n\":1}");
    private final OutboxMessage second = new OutboxMessage(UUID.randomUUID(), EVENT_TYPE, "{\"n\":2}");

    @Test
    void should_publish_and_mark_every_pending_event() {
        // Given
        when(outboxPort.findPending(10)).thenReturn(List.of(first, second));
        RelayOutboxService service = new RelayOutboxService(outboxPort, eventPublisher, FIXED_CLOCK, 10);

        // When
        int published = service.relayPending();

        // Then
        assertThat(published).isEqualTo(2);
        verify(outboxPort).markPublished(first.eventId(), NOW);
        verify(outboxPort).markPublished(second.eventId(), NOW);
    }

    @Test
    void should_stop_and_keep_events_pending_when_broker_fails() {
        // Given
        when(outboxPort.findPending(10)).thenReturn(List.of(first, second));
        doThrow(new EventPublicationException("broker down", null)).when(eventPublisher).publish(first);
        RelayOutboxService service = new RelayOutboxService(outboxPort, eventPublisher, FIXED_CLOCK, 10);

        // When
        int published = service.relayPending();

        // Then
        assertThat(published).isZero();
        verify(outboxPort, never()).markPublished(any(), any());
        verify(eventPublisher, never()).publish(second);
    }

    @Test
    void should_publish_nothing_when_outbox_is_empty() {
        // Given
        when(outboxPort.findPending(10)).thenReturn(List.of());
        RelayOutboxService service = new RelayOutboxService(outboxPort, eventPublisher, FIXED_CLOCK, 10);

        // When / Then
        assertThat(service.relayPending()).isZero();
    }

    @Test
    void should_reject_batch_size_below_one() {
        // Given / When / Then
        assertThatThrownBy(() -> new RelayOutboxService(outboxPort, eventPublisher, FIXED_CLOCK, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
