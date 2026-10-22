package co.edu.univalle.fleetops.fuel.adapter.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository of outbox_events. Used only by {@link OutboxJpaAdapter}. */
interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, UUID> {

    List<OutboxEventEntity> findByPublishedAtIsNullOrderByOccurredAtAsc(Pageable pageable);
}
