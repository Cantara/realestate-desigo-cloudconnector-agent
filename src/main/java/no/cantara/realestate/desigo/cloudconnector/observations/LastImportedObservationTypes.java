package no.cantara.realestate.desigo.cloudconnector.observations;

import java.time.Instant;
import java.util.Map;

public interface LastImportedObservationTypes {
    int loadLastUpdatedStatus();

    Map<String, Instant> getLastImportedObservationTypes();

    void updateLastImported(String observationType, Instant lastUpdatedDateTime);

    void persistLastUpdatedStatus();
}
