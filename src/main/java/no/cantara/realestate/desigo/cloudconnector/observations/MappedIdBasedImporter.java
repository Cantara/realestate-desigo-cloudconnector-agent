package no.cantara.realestate.desigo.cloudconnector.observations;

import no.cantara.config.ApplicationProperties;
import no.cantara.realestate.desigo.cloudconnector.DesigoCloudConnectorException;
import no.cantara.realestate.desigo.cloudconnector.DesigoCloudconnectorApplicationFactory;
import no.cantara.realestate.desigo.cloudconnector.StatusType;
import no.cantara.realestate.desigo.cloudconnector.automationserver.*;
import no.cantara.realestate.desigo.cloudconnector.distribution.*;
import no.cantara.realestate.desigo.cloudconnector.notifications.NotificationService;
import no.cantara.realestate.desigo.cloudconnector.notifications.SlackNotificationService;
import no.cantara.realestate.desigo.cloudconnector.sensors.DesigoSensorMappingImporter;
import no.cantara.realestate.desigo.cloudconnector.sensors.SensorType;
import no.cantara.realestate.desigo.cloudconnector.status.TemporaryHealthResource;
import no.cantara.realestate.distribution.ObservationDistributionClient;
import no.cantara.realestate.mappingtable.MappedSensorId;
import no.cantara.realestate.mappingtable.desigo.DesigoSensorId;
import no.cantara.realestate.mappingtable.repository.MappedIdQuery;
import no.cantara.realestate.mappingtable.repository.MappedIdRepository;
import no.cantara.realestate.mappingtable.repository.MappedIdRepositoryImpl;
import no.cantara.realestate.observations.ObservationMessage;
import org.slf4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

import static no.cantara.realestate.desigo.cloudconnector.status.TemporaryHealthResource.lastImportedObservationTypes;
import static org.slf4j.LoggerFactory.getLogger;

public class MappedIdBasedImporter implements TrendLogsImporter, PresentValueImporter {
    private static final Logger log = getLogger(MappedIdBasedImporter.class);
    public static final int FIRST_IMPORT_LATEST_SECONDS = 60 * 60 * 24;

    private final MappedIdQuery mappedIdQuery;
    private final SdClient basClient;
    private final ObservationDistributionClient distributionClient;
    private final MetricsDistributionClient metricsClient;
    private final MappedIdRepository mappedIdRepository;
    private List<MappedSensorId> importableTrendIds = new ArrayList<>();
    private final Map<String, Instant> lastSuccessfulImportAt;
    private Timer metricsDistributor;
    private int numberOfSuccessfulImports = 0;

    public MappedIdBasedImporter(MappedIdQuery mappedIdQuery, SdClient basClient, ObservationDistributionClient distributionClient, MetricsDistributionClient metricsClient, MappedIdRepository mappedIdRepository) {
        this.mappedIdQuery = mappedIdQuery;
        this.basClient = basClient;
        this.distributionClient = distributionClient;
        this.metricsClient = metricsClient;
        this.mappedIdRepository = mappedIdRepository;
        lastSuccessfulImportAt = new HashMap<>();
    }

    @Override
    public void startup() {
        try {
            importableTrendIds = mappedIdRepository.find(mappedIdQuery);
            log.debug("Found {} trendIds to import. Query: {}", importableTrendIds.size(), mappedIdQuery);
            if (!basClient.isLoggedIn()) {
                basClient.logon();
            }
            metricsClient.openDb();
            lastImportedObservationTypes.loadLastUpdatedStatus();
            TemporaryHealthResource.lastImportedObservationTypes = lastImportedObservationTypes;
            metricsClient.sendMetrics(new MetasysLogonOk());
            metricsDistributor = new Timer();
            metricsDistributor.schedule(new TimerTask() {
                @Override
                public void run() {
                    int countOfImportsLastSecond = getNumberOfSuccessfulImports();
                    if (countOfImportsLastSecond > 0) {
                        setNumberOfSuccessfulImports(0);
                        metricsClient.sendMetrics(new MetasysTrendsFetchedOk(countOfImportsLastSecond));
                    }
                }
            }, 2000, 1000);
        } catch (SdLogonFailedException e) {
            //FIXME add alerting and health
            TemporaryHealthResource.addRegisteredError("Logon to Metasys Failed");
            TemporaryHealthResource.setUnhealthy();
            metricsClient.sendMetrics(new MetasysLogonFailed());
            String message = "Failed to logon to ApiClient. Reason: " + e.getMessage();
            if (e.getCause() != null) {
                message += ". Cause is: " + e.getCause().getMessage();
            }
            log.warn(message);
            throw new RuntimeException("Failed to logon to ApiClient.", e);
        }
    }

    @Override
    public void flush() {
        if (metricsClient != null) {
            metricsClient.flush();
        }
    }

    public void close() {
        metricsClient.closeDb();
        if (lastImportedObservationTypes != null) {
            lastImportedObservationTypes.persistLastUpdatedStatus();
        }
    }

    @Override
    public void importAll() {
        ApplicationProperties config = ApplicationProperties.getInstance();
        boolean importTrends = config.asBoolean("import.trends", false);
        if (importTrends) {
            Map<String, Instant> lastImportedAtList = lastImportedObservationTypes.getLastImportedObservationTypes();
            String sensorType = mappedIdQuery.getSensorType();
            Instant lastImportedAt = lastImportedAtList.get(sensorType);
            if (lastImportedAt == null) {
                lastImportedAt = Instant.now();
                lastImportedObservationTypes.updateLastImported(sensorType, lastImportedAt);
            }
            Instant importFromDateTime = getImportFromDateTime();
            importAfterDateTime(sensorType, importFromDateTime);
        } else {
            log.info("Import of trends is disabled. Skipping import of trends.");
        }
        boolean importPresentValues = config.asBoolean("import.presentvalues", false);
        if (importPresentValues) {
            importAllPresentValues();
        } else {
            log.info("Import of present values is disabled. Skipping import of present values.");
        }
    }

    protected Instant getImportFromDateTime() {
        Instant importFrom = null;
        int importBeforeInSec = ApplicationProperties.getInstance().asInt("import.start.before.sec", FIRST_IMPORT_LATEST_SECONDS);
        importFrom = Instant. now().minusSeconds(importBeforeInSec);
        log.info("Start import from: {}", importFrom);
        return importFrom;
    }

    @Override
    public void importAllAfterDateTime(Instant fromDateTime) {
        int successfulImport = 0;
        int failedImport = 0;
        for (MappedSensorId mappedSensorId : importableTrendIds) {
            //TODO take and skip need logic
            int take = 200;
            int skip = 0;
            if (mappedSensorId.getSensorId() != null && mappedSensorId.getSensorId() instanceof DesigoSensorId) {

                DesigoSensorId sensorId = (DesigoSensorId) mappedSensorId.getSensorId();
                String trendId = sensorId.getTrendId();
                if (trendId == null) {
                    log.trace("TrendId is null for sensorId: {}. Will not attempt to import trend observations.", sensorId);
                } else {
                    Instant importFrom = lastSuccessfulImportAt.get(trendId);
                    if (importFrom == null) {
                        importFrom = fromDateTime;
                    }


                    log.trace("Try to import trendId: {} from: {}", trendId, importFrom);
                    try {
                        Set<DesigoTrendSample> trendSamples = basClient.findTrendSamplesByDate(trendId, take, skip, importFrom);
                        if (trendSamples != null) {
                            log.trace("Found {} samples for trendId: {}", trendSamples.size(), trendId);
                            if (trendSamples.size() > 0) {
                                lastSuccessfulImportAt.put(trendId, Instant.now());
                            }

                            successfulImport++;
                            for (DesigoTrendSample trendSample : trendSamples) {
                                ObservationMessage observationMessage = new DesigoObservationMessage(trendSample, mappedSensorId);
                                distributionClient.publish(observationMessage);
                            }
                            metricsClient.populate(trendSamples, mappedSensorId);
                            reportSuccessfulImport(trendId);
                        } else {
                            log.trace("Missing TrendSamples for trendId: {}",trendId);
                        }
                    } catch (URISyntaxException e) {
                        DesigoCloudConnectorException se = new DesigoCloudConnectorException("Import of trend: {} is not possible now. Reason: {}", e, StatusType.RETRY_NOT_POSSIBLE);
                        log.warn("Import of trend: {} is not possible now. URI to SD server is misconfigured. Reason: {} ", trendId, e.getMessage());
                        throw se;
                    } catch (SdLogonFailedException e) {
                        DesigoCloudConnectorException se = new DesigoCloudConnectorException("Failed to logon to SD server.", e, StatusType.RETRY_NOT_POSSIBLE);
                        log.warn("Import of trend: {} is not possible now. Reason: {} ", trendId, e.getMessage());
                        throw se;
                    } catch (Exception e) {
                        DesigoCloudConnectorException se = new DesigoCloudConnectorException("Failed to import trendId " + trendId, e, StatusType.RETRY_MAY_FIX_ISSUE);
                        log.trace("Failed to import trendId {} for tfm2rec: {}. Reason: {}", trendId, mappedSensorId, se.getMessage());
                        log.trace("cause:", e);
                        failedImport++;
                    }
                }
            } else {
                log.warn("SensorId is not a MetasysSensorId. Skipping import of sensorId: {}", mappedSensorId.getSensorId());
                continue;
            }
        }
        log.trace("Tried to import {}. Successful {}. Failed {}", importableTrendIds.size(), successfulImport, failedImport);
    }
    @Override
    public void importAllPresentValues() {
        int successfulImport = 0;
        int failedImport = 0;
        for (MappedSensorId mappedSensorId : importableTrendIds) {

            if (mappedSensorId.getSensorId() != null && mappedSensorId.getSensorId() instanceof DesigoSensorId) {

                DesigoSensorId sensorId = (DesigoSensorId) mappedSensorId.getSensorId();
                String objectId = sensorId.getDesigoId();
                String propertyId = sensorId.getDesigoPropertyId();
                if (objectId == null || propertyId == null) {
                    log.warn("objectId or propertyId is null for sensorId: {}", sensorId);
                } else {
                    String objectOrPropertyId = objectId + "." + propertyId;
                    log.trace("Try import of ObjectOrPropertyId: {} ", objectOrPropertyId);
                    try {
                        DesigoPresentValue presentValue = basClient.findPresentValue(sensorId);
                        if (presentValue != null) {
                            log.trace("Found {} sample for objectOrPropertyId: {}",presentValue, objectOrPropertyId);
                                lastSuccessfulImportAt.put(objectId, Instant.now());
                            successfulImport++;
                                ObservationMessage observationMessage = new DesigoObservationMessage(presentValue, mappedSensorId);
                                distributionClient.publish(observationMessage);
                            metricsClient.populate(presentValue, mappedSensorId);
                            reportSuccessfulImport(objectOrPropertyId);
                        } else {
                            log.trace("Missing PresentValue for objectOrPropertyId: {}", objectOrPropertyId);
                        }
                    } catch (URISyntaxException e) {
                        DesigoCloudConnectorException se = new DesigoCloudConnectorException("Import of objectOrPropertyId: {} is not possible now. Reason: {}", e, StatusType.RETRY_NOT_POSSIBLE);
                        log.warn("Import of objectOrPropertyId: {} is not possible now. URI to SD server is misconfigured. Reason: {} ", objectOrPropertyId, e.getMessage());
                        throw se;
                    } catch (SdLogonFailedException e) {
                        DesigoCloudConnectorException se = new DesigoCloudConnectorException("Failed to logon to SD server.", e, StatusType.RETRY_NOT_POSSIBLE);
                        log.warn("Import of objectOrPropertyId: {} is not possible now. Reason: {} ", objectOrPropertyId, e.getMessage());
                        throw se;
                    } catch (Exception e) {
                        DesigoCloudConnectorException se = new DesigoCloudConnectorException("Failed to import objectOrPropertyId " + objectOrPropertyId, e, StatusType.RETRY_MAY_FIX_ISSUE);
                        log.trace("Failed to import objectOrPropertyId {} for MappedSensorId: {}. Reason: {}", objectOrPropertyId, mappedSensorId, se.getMessage());
                        log.trace("cause:", e);
                        failedImport++;
                    }
                }
            } else {
                log.warn("SensorId is not a DesigoSensorId. Skipping import of sensorId: {}", mappedSensorId.getSensorId());
            }
        }
        log.trace("Tried to import {}. Successful {}. Failed {}", importableTrendIds.size(), successfulImport, failedImport);
    }

    @Override
    public void importFromDay0(String observationType) {

    }

    @Override
    public void importAfterDateTime(String observationType, Instant fromDateTime) {
        log.info("Import all after {}", fromDateTime);
        importAllAfterDateTime(fromDateTime);
        lastImportedObservationTypes.updateLastImported(observationType, Instant.now());
    }

    /*
    Helper to ensure Thread safety.
     */
    void reportSuccessfulImport(String trendId) {

        int successful = getNumberOfSuccessfulImports();
        setNumberOfSuccessfulImports(successful + 1);
    }

    public synchronized int getNumberOfSuccessfulImports() {
        return numberOfSuccessfulImports;
    }

    public synchronized void setNumberOfSuccessfulImports(int numberOfSuccessfulImports) {
        this.numberOfSuccessfulImports = numberOfSuccessfulImports;
    }


    public static void main(String[] args) throws URISyntaxException, InterruptedException {
        ApplicationProperties config = new DesigoCloudconnectorApplicationFactory()
                .conventions(ApplicationProperties.builder())
                .build();
        String apiUrl = config.get("sd_api_url");
        URI apiUri = new URI(apiUrl);
        NotificationService notificationService = new SlackNotificationService();
        SdClient sdClient = new DesigoApiClientRest(apiUri, notificationService);

        String measurementName = config.get("MEASUREMENT_NAME");
        ObservationDistributionClient observationClient = new ObservationDistributionServiceStub();//Simulator
        MetricsDistributionClient metricsClient = new MetricsDistributionServiceStub(measurementName);//Simulator

        MappedIdQuery tfm2RecQuery = new DesigoMappedIdQueryBuilder().realEstate("RE1")
                .sensorType(SensorType.co2.name())
                .build();
        String configDirectory = config.get("importdata.directory");
        MappedIdRepository mappedIdRepository = createMappedIdRepository(true, configDirectory);
        MappedIdBasedImporter importer = new MappedIdBasedImporter(tfm2RecQuery, sdClient, observationClient, metricsClient, mappedIdRepository);
        importer.startup();
        log.info("Startup finished.");
        importer.importAllAfterDateTime(Instant.now().minusSeconds(60 * 15));
        metricsClient.flush();
        log.info("Sleeping for 10 sec");
        Thread.sleep(10000);
        log.info("Closing connections");
        importer.close();
        System.exit(0);
    }



    private static MappedIdRepository createMappedIdRepository(boolean doImportData, String configDirectory) {
        MappedIdRepository mappedIdRepository = new MappedIdRepositoryImpl();
        if (doImportData) {
            if (!Paths.get(configDirectory).toFile().exists()) {
                throw new DesigoCloudConnectorException("Import of data from " + configDirectory + " failed. Directory does not exist.");
            }
            new DesigoSensorMappingImporter().importCsvConfig(configDirectory, mappedIdRepository);
        }
        return mappedIdRepository;
    }
    /*
   Primarily used for testing
    */
    protected void addImportableTrendId(MappedSensorId mappedSensorId) {
        importableTrendIds.add(mappedSensorId);
    }
}
