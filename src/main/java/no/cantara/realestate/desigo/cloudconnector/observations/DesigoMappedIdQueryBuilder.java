package no.cantara.realestate.desigo.cloudconnector.observations;

import no.cantara.realestate.mappingtable.desigo.DesigoSensorId;
import no.cantara.realestate.mappingtable.repository.MappedIdQueryBuilder;

public class DesigoMappedIdQueryBuilder extends MappedIdQueryBuilder {
    public DesigoMappedIdQueryBuilder() {
        super();
        sensorIdClass(DesigoSensorId.class);
    }
}
