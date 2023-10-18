package no.cantara.realestate.desigo.cloudconnector.sensors;

import no.cantara.config.ApplicationProperties;
import no.cantara.realestate.azure.storage.AzureTableClient;
import no.cantara.realestate.mappingtable.MappedSensorId;
import no.cantara.realestate.mappingtable.desigo.DesigoTableSensorImporter;
import no.cantara.realestate.mappingtable.importer.CsvSensorImporter;
import no.cantara.realestate.mappingtable.metasys.MetasysCsvSensorImporter;
import no.cantara.realestate.mappingtable.repository.MappedIdRepository;
import org.slf4j.Logger;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

public class DesigoSensorMappingImporter implements SensorMappingImporter {
    private static final Logger log = getLogger(DesigoSensorMappingImporter.class);

    @Override
    public long importSensorMappings(ApplicationProperties config, MappedIdRepository mappedIdRepository) {
        long totalCount = 0;
        boolean importFromAzureTable = config.asBoolean("sensormappings.azure.enabled", false);
        if (importFromAzureTable) {
            String connectionString = config.get("sensormappings.azure.connectionString");
            String tableName = config.get("sensormappings.azure.tableName");
            long count =  importAzureTableConfig(connectionString, tableName, mappedIdRepository);
            log.info("Imported {} Desigo Sensor configs from Azure Table {}", count, tableName);
            totalCount += count;
        }
        boolean importFromCsv = config.asBoolean("sensormappings.csv.enabled", false);
        if (importFromCsv) {
            String configDirectory = config.get("sensormappings.csv.directory");
            long count = importCsvConfig(configDirectory, mappedIdRepository);
            log.info("Imported {} Desigo Sensor configs from directory {}", count, configDirectory);
            totalCount += count;
        }
        log.info("Imported {} Desigo Sensor configs in total", totalCount);
        return totalCount;

    }

    public long importAzureTableConfig(String connectionString, String tableName, MappedIdRepository mappedIdRepository) {
        AzureTableClient tableClient = new AzureTableClient(connectionString, tableName);
        List<Map<String, String>> rows = tableClient.listRows("1");
        List<MappedSensorId> mappedSensorIds = new DesigoTableSensorImporter(rows).importMappedId("Desigo");
        for (MappedSensorId mappedSensorId : mappedSensorIds) {
            mappedIdRepository.add(mappedSensorId);
        }
        return mappedSensorIds.size();
    }


    public long importCsvConfig(String configDirectory, MappedIdRepository mappedIdRepository) {
        File importDirectory = new File(configDirectory);
        CsvSensorImporter csvImporter = new MetasysCsvSensorImporter(importDirectory);
        List<MappedSensorId> mappedSensorIds = csvImporter.importMappedId("Desigo");
        log.info("Imported {} Metasys Sensor configs from directory {}", mappedSensorIds.size(), importDirectory);
        for (MappedSensorId mappedSensorId : mappedSensorIds) {
            mappedIdRepository.add(mappedSensorId);
        }
        return mappedSensorIds.size();
    }
}
