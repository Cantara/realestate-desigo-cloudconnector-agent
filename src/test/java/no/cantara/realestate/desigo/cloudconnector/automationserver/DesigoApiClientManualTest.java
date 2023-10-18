package no.cantara.realestate.desigo.cloudconnector.automationserver;

import no.cantara.config.ApplicationProperties;
import no.cantara.realestate.desigo.cloudconnector.DesigoCloudconnectorApplicationFactory;
import no.cantara.realestate.desigo.cloudconnector.MockServerSetup;
import no.cantara.realestate.desigo.cloudconnector.notifications.SlackNotificationService;
import org.slf4j.Logger;

import java.net.URI;

import static junit.framework.Assert.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;

public class DesigoApiClientManualTest {
    private static final Logger log = getLogger(DesigoApiClientManualTest.class);

    public static void main(String[] args) throws SdLogonFailedException {
        ApplicationProperties config = new DesigoCloudconnectorApplicationFactory()
                .conventions(ApplicationProperties.builder())
                .buildAndSetStaticSingleton();
        String apiUrl = config.get("sd.api.url");
        MockServerSetup.clearAndSetLoginMock();
        MockServerSetup.clearAndSetSensorMockData("8648f9cf-c135-5471-9906-9b3861e0b5ab");
        //MockServerSetup.clearAndSetSensorMockData("208540b1-ab8a-566a-8a41-8b4cee515baf");
        DesigoApiClientRest apiClient = new DesigoApiClientRest(URI.create(apiUrl), new SlackNotificationService());
        apiClient.logon("jane-doe","strongPassword");
        assertTrue(apiClient.isHealthy());
    }
}
