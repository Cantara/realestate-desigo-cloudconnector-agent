package no.cantara.realestate.desigo.cloudconnector.notifications;

import no.cantara.config.ApplicationProperties;
import no.cantara.config.testsupport.ApplicationPropertiesTestHelper;
import no.cantara.realestate.desigo.cloudconnector.DesigoCloudconnectorApplicationFactory;
import no.cantara.realestate.desigo.cloudconnector.automationserver.DesigoApiClientRest;
import org.junit.jupiter.api.*;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.MediaType;

import java.net.URI;

import static no.cantara.realestate.desigo.cloudconnector.automationserver.DesigoApiClientRest.*;
import static no.cantara.realestate.mappingtable.Main.getConfigValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.JsonBody.json;
import static org.mockserver.model.Parameter.param;
import static org.mockserver.model.ParameterBody.params;

public class DesigoApiVerifyNotificationsTest {

    private static String apiUrl;
    private NotificationService notificationService;
    private static ClientAndServer mockServer;

    private static int HTTP_PORT = 1084;

    @BeforeAll
    static void beforeAll() {
        ApplicationPropertiesTestHelper.enableMutableSingleton();
        ApplicationProperties config = new DesigoCloudconnectorApplicationFactory()
                .conventions(ApplicationProperties.builder())
                .buildAndSetStaticSingleton();
        mockServer = ClientAndServer.startClientAndServer(HTTP_PORT);
    }

    @AfterAll
    static void afterAll() {
        mockServer.stop();
    }

    @BeforeEach
    void setUp() {
        notificationService = mock(NotificationService.class);
    }

    @AfterEach
    void tearDown() {
        mockServer.reset();
    }

    @Test
    void verifyMetasysHostUnreachable() {
        URI apiUri = URI.create("http://localhost:8080");
        DesigoApiClientRest metasysApiClient = new DesigoApiClientRest(apiUri, notificationService);
        try {
            metasysApiClient.logon();
            fail("Expected exception");
        } catch (Exception e) {
            verify(notificationService).sendAlarm(DESIGO_API,HOST_UNREACHABLE);
        }
        assertFalse(metasysApiClient.isHealthy());
    }

    @Test
    void verifyDesigoLoginOk() {
        String userName = getConfigValue("sd.api.username");
        String password = getConfigValue("sd.api.password");
        mockServer
                .when(
                        request()
                                .withMethod("POST")
                                .withPath("/testcontext/api/token")
                                .withContentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .withBody(params(
                                        param("username", userName),
                                        param("password", password),
                                        param("grant_type", "password")
                                ))
                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody("{\n" +
                                        "  \"access_token\": \"eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1...\",\n" +
                                        "  \"token_type\": \"bearer\",\n" +
                                        "  \"user_name\": \"" + userName + "\",\n" +
                                        "  \"user_descriptor\": \"eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1...\",\n" +
                                        "  \"user_profile\": \"DEFAULT.ldl\",\n" +
                                        "  \"flex_user_profile\": \"DEFAULT\",\n" +
                                        "  \"user_inactivity_timeout\": \"0\",\n" +
                                        "  \"expires_in\": 2591999 \n" +
                                        "}\n")
                                .withHeader(
                                        "Content-Type", "application/json"
                                )
                );
        String apiUrl = "http://localhost:" + mockServer.getPort() + "/testcontext/api/";
        URI apiUri = URI.create(apiUrl);
        DesigoApiClientRest metasysApiClient = new DesigoApiClientRest(apiUri, notificationService);
        try {
            metasysApiClient.logon();
            verify(notificationService).clearService(DESIGO_API);
        } catch (Exception e) {
            fail("No exception expected");
        }
        assertTrue(metasysApiClient.isHealthy());
//        UserToken userToken = metasysApiClient.getUserToken();
//        assertNotNull(userToken);
    }
    @Test
    void verifyMetasysLoginFailed() {
        mockServer
                .when(
                        request()
                                .withMethod("POST")
                                .withPath("/api/v4/login")
                                .withContentType(MediaType.APPLICATION_JSON)
                                .withBody(json("{\"username\": \"jane-doe\", \"password\": \"strongPassword\"}"))
                )
                .respond(
                        response()
                                .withStatusCode(401)
                );
        String apiUrl = "http://localhost:" + mockServer.getPort() + "/api/v4/";
        URI apiUri = URI.create(apiUrl);
        DesigoApiClientRest metasysApiClient = new DesigoApiClientRest(apiUri, notificationService);
        try {
            metasysApiClient.logon();
            fail("Expected exception");
        } catch (Exception e) {
            verify(notificationService).sendWarning(DESIGO_API, LOGON_FAILED);
        }
        assertFalse(metasysApiClient.isHealthy());
    }
}
