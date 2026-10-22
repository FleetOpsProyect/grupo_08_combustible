package co.edu.univalle.fleetops.fuel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point of the Fuel microservice (G08) of FleetOps. */
@SpringBootApplication
public class FuelApplication {

    /**
     * Starts the service.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(FuelApplication.class, args);
    }
}
