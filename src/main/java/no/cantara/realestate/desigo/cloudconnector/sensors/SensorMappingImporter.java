package no.cantara.realestate.desigo.cloudconnector.sensors;

import no.cantara.config.ApplicationProperties;
import no.cantara.realestate.mappingtable.repository.MappedIdRepository;

public interface SensorMappingImporter {

    long importSensorMappings(ApplicationProperties config, MappedIdRepository mappedIdRepository);
}
