package no.cantara.realestate.desigo.cloudconnector;

import no.cantara.config.ApplicationProperties;
import no.cantara.realestate.azure.AzureObservationDistributionClient;
import no.cantara.realestate.desigo.cloudconnector.automationserver.DesigoApiClientRest;
import no.cantara.realestate.desigo.cloudconnector.automationserver.SdClient;
import no.cantara.realestate.desigo.cloudconnector.automationserver.SdClientSimulator;
import no.cantara.realestate.desigo.cloudconnector.automationserver.SdLogonFailedException;
import no.cantara.realestate.desigo.cloudconnector.distribution.MetricsDistributionClient;
import no.cantara.realestate.desigo.cloudconnector.distribution.MetricsDistributionServiceStub;
import no.cantara.realestate.desigo.cloudconnector.distribution.ObservationDistributionResource;
import no.cantara.realestate.desigo.cloudconnector.distribution.ObservationDistributionServiceStub;
import no.cantara.realestate.desigo.cloudconnector.notifications.NotificationService;
import no.cantara.realestate.desigo.cloudconnector.notifications.SlackNotificationService;
import no.cantara.realestate.desigo.cloudconnector.observations.DesigoMappedIdQueryBuilder;
import no.cantara.realestate.desigo.cloudconnector.observations.MappedIdBasedImporter;
import no.cantara.realestate.desigo.cloudconnector.observations.ScheduledImportManager;
import no.cantara.realestate.desigo.cloudconnector.observations.TrendLogsImporter;
import no.cantara.realestate.desigo.cloudconnector.sensors.DesigoSensorMappingImporter;
import no.cantara.realestate.desigo.cloudconnector.sensors.SensorMappingImporter;
import no.cantara.realestate.desigo.cloudconnector.sensors.SensorType;
import no.cantara.realestate.distribution.ObservationDistributionClient;
import no.cantara.realestate.mappingtable.repository.MappedIdQuery;
import no.cantara.realestate.mappingtable.repository.MappedIdQueryBuilder;
import no.cantara.realestate.mappingtable.repository.MappedIdRepository;
import no.cantara.realestate.mappingtable.repository.MappedIdRepositoryImpl;
import no.cantara.realestate.observations.ObservationMessage;
import no.cantara.stingray.application.AbstractStingrayApplication;
import no.cantara.stingray.application.health.StingrayHealthService;
import no.cantara.stingray.security.StingraySecurity;
import org.slf4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.ServiceLoader;

import static no.cantara.realestate.desigo.cloudconnector.ObservationMesstageStubs.buildStubObservation;
import static org.slf4j.LoggerFactory.getLogger;

public class DesigoCloudconnectorApplication extends AbstractStingrayApplication<DesigoCloudconnectorApplication> {
    private static final Logger log = getLogger(DesigoCloudconnectorApplication.class);
    private boolean enableStream;
    private boolean enableScheduledImport;
    private NotificationService notificationService;

    public DesigoCloudconnectorApplication(ApplicationProperties config) {
        super("DesigoCloudconnector",
                readMetaInfMavenPomVersion("no.cantara.realestate", "desigo-cloudconnector-agent"),
                config
        );
    }


    public static void main(String[] args) {
        ApplicationProperties config = new DesigoCloudconnectorApplicationFactory()
                .conventions(ApplicationProperties.builder())
                .buildAndSetStaticSingleton();

        try {
            DesigoCloudconnectorApplication application = new DesigoCloudconnectorApplication(config).init().start();
            log.info("Server started. See status on {}:{}{}/health", "http://localhost", config.get("server.port"), config.get("server.context-path"));
            application.startImportingObservations();
        } catch (Exception e) {
            log.error("Failed to start DesigoCloudconnectorApplication", e);
        }

    }

    private void startImportingObservations() {
        if (enableScheduledImport) {
            get(ScheduledImportManager.class).startScheduledImportOfTrendIds();
        }
    }



    @Override
    protected void doInit() {
        initBuiltinDefaults();
        StingraySecurity.initSecurity(this);

        initNotificationServices();
        boolean doImportData = config.asBoolean("import.data");
        enableStream = config.asBoolean("sd.stream.enabled");
        enableScheduledImport = config.asBoolean("sd.scheduledImport.enabled");
        log.info("*** Logon to SD ***");
        SdClient sdClient = createSdClient(config);
        log.info("*** Logon to SD done ***");

        ServiceLoader<ObservationDistributionClient> observationDistributionClients = ServiceLoader.load(ObservationDistributionClient.class);
        ObservationDistributionClient observationDistributionClient = null;
        for (ObservationDistributionClient distributionClient : observationDistributionClients) {
            if (distributionClient != null && distributionClient instanceof AzureObservationDistributionClient) {
                log.info("Found implementation of ObservationDistributionClient on classpath: {}", distributionClient.toString());
                observationDistributionClient = distributionClient;
            }
            get(StingrayHealthService.class).registerHealthProbe(observationDistributionClient.getName() +"-isConnected: ", observationDistributionClient::isConnectionEstablished);
            get(StingrayHealthService.class).registerHealthProbe(observationDistributionClient.getName() +"-numberofMessagesObserved: ", observationDistributionClient::getNumberOfMessagesObserved);
        }
        if (observationDistributionClient == null) {
            log.warn("No implementation of ObservationDistributionClient was found on classpath. Creating a ObservationDistributionServiceStub explicitly.");
            observationDistributionClient = new ObservationDistributionServiceStub();
            get(StingrayHealthService.class).registerHealthProbe(observationDistributionClient.getName() +"-isConnected: ", observationDistributionClient::isConnectionEstablished);
            get(StingrayHealthService.class).registerHealthProbe(observationDistributionClient.getName() +"-numberofMessagesDistributed: ", observationDistributionClient::getNumberOfMessagesObserved);
        }
        observationDistributionClient.openConnection();
        log.info("Establishing and verifying connection to Azure.");
        if (observationDistributionClient.isConnectionEstablished()) {
            ObservationMessage stubMessage = buildStubObservation();
            observationDistributionClient.publish(stubMessage);
        }
        String measurementsName = config.get("measurements.name");
        MetricsDistributionClient metricsDistributionClient = new MetricsDistributionServiceStub(measurementsName);
        MappedIdRepository mappedIdRepository = init(MappedIdRepository.class, () -> createMappedIdRepository(doImportData));
        ObservationDistributionClient finalObservationDistributionClient = observationDistributionClient;
        ScheduledImportManager scheduledImportManager = init(ScheduledImportManager.class, () -> wireScheduledImportManager(sdClient, finalObservationDistributionClient, metricsDistributionClient, mappedIdRepository));
        ObservationDistributionResource observationDistributionResource = initAndRegisterJaxRsWsComponent(ObservationDistributionResource.class, () -> createObservationDistributionResource(finalObservationDistributionClient));

        get(StingrayHealthService.class).registerHealthProbe("mappedIdRepository.size", mappedIdRepository::size);
        get(StingrayHealthService.class).registerHealthProbe(sdClient.getName() + "-isHealthy: ", sdClient::isHealthy);
        get(StingrayHealthService.class).registerHealthProbe(sdClient.getName() + "-isLogedIn: ", sdClient::isLoggedIn);
        get(StingrayHealthService.class).registerHealthProbe(sdClient.getName() + "-numberofTrendsSamples: ", sdClient::getNumberOfTrendSamplesReceived);
        get(StingrayHealthService.class).registerHealthProbe("observationDistribution.message.count", observationDistributionResource::getDistributedCount);
        //Random Example
        init(Random.class, this::createRandom);
        RandomizerResource randomizerResource = initAndRegisterJaxRsWsComponent(RandomizerResource.class, this::createRandomizerResource);

        //Wire up the stream importer

    }

    private void initNotificationServices() {
        ServiceLoader<NotificationService> notificationServices = ServiceLoader.load(NotificationService.class);
        if (notificationServices != null && notificationServices.iterator().hasNext()) {
            notificationService = notificationServices.findFirst().orElse(null);
            log.trace("Alerts and Warnings will be sent with NotificationService: {}", notificationService);
        } else {
            log.warn("ServiceLoader could not find any implementation of NotificationService. Using SlackNotificationService.");
            notificationService = new SlackNotificationService();
        }
    }

    private SdClient createSdClient(ApplicationProperties config) {
        SdClient sdClient;
        String useSDProdValue = config.get("sd.api.prod");

        if (Boolean.valueOf(useSDProdValue)) {
            String apiUrl = config.get("sd.api.url");
            try {
                URI apiUri = new URI(apiUrl);
                sdClient = new DesigoApiClientRest(apiUri, notificationService);
                log.info("Logon to SdClient with username: {}", config.get("sd.api.username"));
                sdClient.logon();
                log.info("Running with a live REST SD.");
            } catch (URISyntaxException e) {
                throw new DesigoCloudConnectorException("Failed to connect SD Client to URL: " + apiUrl, e);
            } catch (SdLogonFailedException e) {
                throw new DesigoCloudConnectorException("Failed to logon SD Client. URL used: " + apiUrl, e);
            }
        } else {
            URI simulatorUri = URI.create("https://simulator.totto.org:8080/SD");
            sdClient = new SdClientSimulator();
            log.info("Running with a simulator of SD.");
        }
        return sdClient;
    }

    protected MappedIdRepository createMappedIdRepository(boolean doImportSensorMappings) {
        MappedIdRepository mappedIdRepository = new MappedIdRepositoryImpl();
        if (doImportSensorMappings) {
            SensorMappingImporter sensorMappingImporter = new DesigoSensorMappingImporter();
            sensorMappingImporter.importSensorMappings(config, mappedIdRepository);
        }
        return mappedIdRepository;
    }

    private ScheduledImportManager wireScheduledImportManager(SdClient sdClient, ObservationDistributionClient distributionClient, MetricsDistributionClient metricsClient, MappedIdRepository mappedIdRepository) {

        ScheduledImportManager scheduledImportManager = null;

        List<String> importAllFromRealestates = findListOfRealestatesToImportFrom();
        log.info("Importallres: {}", importAllFromRealestates);
        if (importAllFromRealestates != null && importAllFromRealestates.size() > 0) {
            for (String realestate : importAllFromRealestates) {
                MappedIdQuery mappedIdQuery = new DesigoMappedIdQueryBuilder().realEstate(realestate).build();
                TrendLogsImporter trendLogsImporter = new MappedIdBasedImporter(mappedIdQuery, sdClient, distributionClient, metricsClient, mappedIdRepository);
                if (scheduledImportManager == null) {
                    scheduledImportManager = new ScheduledImportManager(trendLogsImporter, config);
                } else {
                    scheduledImportManager.addTrendLogsImporter(trendLogsImporter);
                }
            }
        } else {
            log.warn("Using Template import config for RealEstates: REstate1 and RealEst2");
            MappedIdQuery mappedIdQuery = new DesigoMappedIdQueryBuilder().realEstate("REstate1").build();
            TrendLogsImporter trendLogsImporter = new MappedIdBasedImporter(mappedIdQuery, sdClient, distributionClient, metricsClient, mappedIdRepository);
            scheduledImportManager = new ScheduledImportManager(trendLogsImporter, config);

            MappedIdQuery energyOnlyQuery = new MappedIdQueryBuilder().realEstate("RealEst2")
                    .sensorType(SensorType.energy.name())
                    .build();

            TrendLogsImporter mappedIdBasedImporter = new MappedIdBasedImporter(energyOnlyQuery, sdClient, distributionClient, metricsClient, mappedIdRepository);
            scheduledImportManager.addTrendLogsImporter(mappedIdBasedImporter);

            MappedIdQuery mysteryHouseQuery = new MappedIdQueryBuilder().realEstate("511")
                    .build();
            TrendLogsImporter mysteryImporter = new MappedIdBasedImporter(mysteryHouseQuery, sdClient, distributionClient, metricsClient, mappedIdRepository);
            scheduledImportManager.addTrendLogsImporter(mysteryImporter);
        }

        return scheduledImportManager;
    }

    private List<String> findListOfRealestatesToImportFrom() {
        List<String> realEstates = null;
        try {
            String reCsvSplitted = config.get("importsensorsQuery.realestates");
            if (reCsvSplitted != null) {
                realEstates = Arrays.asList(reCsvSplitted.split(","));
            }
        } catch (Exception e) {
            log.warn("Failed to read list of RealEstates used for import.", e);
        }
        return realEstates;
    }

    private ObservationDistributionResource createObservationDistributionResource(ObservationDistributionClient observationDistributionClient) {
        return new ObservationDistributionResource(observationDistributionClient);
    }

    private Random createRandom() {
        return new Random(System.currentTimeMillis());
    }

    private RandomizerResource createRandomizerResource() {
        Random random = get(Random.class);
        return new RandomizerResource(random);
    }

}
