package co.edu.univalle.fleetops.fuel.utilities;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API documentation of the service: springdoc generates the OpenAPI document (/v3/api-docs) from the
 * controllers and its Scalar module shows it as an interactive reference (/scalar).
 */
@Configuration
public class OpenApiDocs {

    @Bean
    OpenAPI fuelOpenApi() {
        return new OpenAPI().info(new Info()
                .title("FleetOps - Fuel service (G08)")
                .version("1.0.0")
                .description("Fuel records, efficiency in km per gallon and anomalous consumption. "
                        + "Contract in English and snake_case, dates in GMT-5."));
    }
}
