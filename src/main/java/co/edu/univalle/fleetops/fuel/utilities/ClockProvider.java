package co.edu.univalle.fleetops.fuel.utilities;

import java.time.Clock;
import java.time.ZoneId;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Clock in the Colombia time zone (GMT-5, America/Bogota, shared convention) and id generator.
 * The core receives them by constructor, so the domain never reads the system time by itself
 * and the tests use a fixed clock.
 */
@Configuration
public class ClockProvider {

    @Bean
    Clock clock(AppConfig config) {
        return Clock.system(ZoneId.of(config.timeZone()));
    }

    @Bean
    Supplier<UUID> idGenerator() {
        return UUID::randomUUID;
    }
}
