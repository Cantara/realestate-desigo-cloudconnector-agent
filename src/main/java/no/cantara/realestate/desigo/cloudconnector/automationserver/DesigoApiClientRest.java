package no.cantara.realestate.desigo.cloudconnector.automationserver;

import jakarta.ws.rs.core.HttpHeaders;
import no.cantara.config.ApplicationProperties;
import no.cantara.realestate.desigo.cloudconnector.DesigoCloudConnectorException;
import no.cantara.realestate.desigo.cloudconnector.DesigoCloudconnectorApplicationFactory;
import no.cantara.realestate.desigo.cloudconnector.notifications.NotificationService;
import no.cantara.realestate.desigo.cloudconnector.notifications.SlackNotificationService;
import no.cantara.realestate.desigo.cloudconnector.status.TemporaryHealthResource;
import no.cantara.realestate.json.RealEstateObjectMapper;
import no.cantara.realestate.mappingtable.SensorId;
import no.cantara.realestate.mappingtable.desigo.DesigoSensorId;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static no.cantara.realestate.desigo.cloudconnector.utils.UrlEncoder.urlEncode;
import static no.cantara.realestate.mappingtable.Main.getConfigValue;
import static org.slf4j.LoggerFactory.getLogger;

public class DesigoApiClientRest implements SdClient {
    private static final Logger log = getLogger(DesigoApiClientRest.class);
    private static final String DESIGO_SUBSCRIBE_HEADER = "DESIGO-SUBSCRIBE";
    private final URI apiUri;

    public static final String DESIGO_API = "Desigo";
    public static final String HOST_UNREACHABLE = "HOST_UNREACHABLE";
    public static final String LOGON_FAILED = "Logon to Desigo Api Failed";
    public static final String SERVICE_FAILED = "Desigo Api is failing.";
    public static final String UNKNOWN_HOST = "UNKNOWN_HOST";

    private static final String LATEST_BY_DATE = "SampleDateDescending";
    private final NotificationService notificationService;
    private DesigoUserToken userToken = null;
    private long numberOfTrendSamplesReceived = 0;
    private boolean isHealthy = true;

    public DesigoApiClientRest(URI apiUri, NotificationService notificationService) {
        this.apiUri = apiUri;
        this.notificationService = notificationService;
    }

    public static void main(String[] args) throws URISyntaxException, SdLogonFailedException {

        ApplicationProperties config = new DesigoCloudconnectorApplicationFactory()
                .conventions(ApplicationProperties.builder())
                .buildAndSetStaticSingleton();
        java.util.logging.Logger.getLogger("org.apache.http.wire").setLevel(java.util.logging.Level.FINEST);
        java.util.logging.Logger.getLogger("org.apache.http.headers").setLevel(java.util.logging.Level.FINEST);

        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "debug");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "debug");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.headers", "debug");

        String trendId = getConfigValue("trend.id");
        String apiUrl = getConfigValue("sd.api.url");
        URI apiUri = new URI(apiUrl);

        DesigoApiClientRest apiClient = new DesigoApiClientRest(apiUri, new SlackNotificationService());
        String bearerToken = apiClient.findAccessToken();
        Set<DesigoTrendSample> trends = apiClient.findTrendSamples(bearerToken, trendId);
        for (DesigoTrendSample trend : trends) {
            if (trend != null) {
                log.info("Trend id={}, value={}, valid={}", trend.getTrendId(), trend.getValue(), trend.isValid());
            } else {
                log.info("Trend is null");
            }
        }
    }

    @Override
    public Set<DesigoTrendSample> findTrendSamples(String bearerToken, String trendId) throws URISyntaxException {
        String apiUrl = getConfigValue("sd.api.url"); //getConfigProperty("sd.api.url");

        URI apiUri = new URI(apiUrl);
        TrendSampleService trendSampleService = RestClientBuilder.newBuilder()
                .baseUri(apiUri)
                .build(TrendSampleService.class);
        return trendSampleService.findTrendSamples("Bearer " + bearerToken, trendId.toString());
    }

    @Override
    public Set<DesigoTrendSample> findTrendSamples(String trendId, int take, int skip) throws URISyntaxException, SdLogonFailedException {
        String apiUrl = getConfigValue("sd.api.url");
        String prefixedUrlEncodedTrendId = encodeAndPrefix(trendId);
        String bearerToken = findAccessToken();
        URI apiUri = new URI(apiUrl);
        TrendSampleService trendSampleService = RestClientBuilder.newBuilder()
                .baseUri(apiUri)
                .build(TrendSampleService.class);
        return trendSampleService.findTrendSamples("Bearer " + bearerToken, prefixedUrlEncodedTrendId, take, skip);
    }

    @Override
    public Set<DesigoTrendSample> findTrendSamplesByDate(String trendId, int take, int skip, Instant onAndAfterDateTime) throws URISyntaxException, SdLogonFailedException {

        String apiUrl = getConfigValue("sd.api.url");
        String prefixedUrlEncodedTrendId = encodeAndPrefix(trendId);
        String bearerToken = findAccessToken();
        URI samplesUri = new URI(apiUrl + "trendseries/" + trendId);
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet request = null;
        List<DesigoTrendSample> trendSamples = new ArrayList<>();
        try {

            String startTime = onAndAfterDateTime.toString();
            int page=1;
            int pageSize=1000;
            String endTime = Instant.now().plusSeconds(60).toString();

//        DesigoTrendSampleResult trendSampleResult = trendSampleService.findTrendSamplesByDate("Bearer " + bearerToken, prefixedUrlEncodedTrendId, pageSize, page, startTime, endTime);
            log.trace("findTrendSamplesByDate. trendId: {}. From date: {}. To date: {}. Page: {}. PageSize: {}. Take: {}. Skip: {}",
                    trendId, onAndAfterDateTime, endTime, page, pageSize, take, skip);
            List<NameValuePair> nvps = new ArrayList<>();
            // GET Query Parameters
            nvps.add(new BasicNameValuePair("from", startTime));
            nvps.add(new BasicNameValuePair("to", endTime));
//            nvps.add(new BasicNameValuePair("page", "1"));
//            nvps.add(new BasicNameValuePair("pageSize", "1000"));
//            nvps.add(new BasicNameValuePair("skip", "0"));

            URI uri = new URIBuilder(samplesUri)
                    .addParameters(nvps)
                    .build();
            request = new HttpGet(uri);
            request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
            CloseableHttpResponse response = httpClient.execute(request);
            try {
                int httpCode = response.getCode();
                if (httpCode == 200) {
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        String body = EntityUtils.toString(entity);
                        log.trace("Received body: {}", body);
                        DesigoTrendSampleResult trendSampleResult = TrendSamplesMapper.mapFromJson(body);
                        log.trace("Found: {} trends from trendId: {}", trendSampleResult.getTotal(), trendId);
                         trendSamples = trendSampleResult.getSeriesWithObjectAndPropertyId();
                        if (trendSamples != null) {
                            for (DesigoTrendSample trendSample : trendSamples) {
                                trendSample.setTrendId(trendId);
                                log.trace("imported trendSample: {}", trendSample);
                            }
                        }

                    }
                }
            } catch (Exception e) {
                setUnhealthy();
                throw new DesigoCloudConnectorException("Failed to fetch trendsamples for objectId " + trendId
                        + ", after date "+ onAndAfterDateTime + ". Reason: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            setUnhealthy();
            throw new DesigoCloudConnectorException("Failed to fetch trendsamples for objectId " + trendId
                    + ", after date "+ onAndAfterDateTime + ". Reason: " + e.getMessage(), e);
        }

        /*
        String startTime = onAndAfterDateTime.toString();
        //FIXME make dynamic
        int page=1;
        int pageSize=1000;
        String endTime = Instant.now().plusSeconds(60).toString();



//        DesigoTrendSampleResult trendSampleResult = trendSampleService.findTrendSamplesByDate("Bearer " + bearerToken, prefixedUrlEncodedTrendId, pageSize, page, startTime, endTime);
        log.trace("findTrendSamplesByDate. trendId: {}. From date: {}. To date: {}. Page: {}. PageSize: {}. Take: {}. Skip: {}",
                objectId, onAndAfterDateTime, endTime, page, pageSize, take, skip);
        String trendSamplesJson = trendSampleService.findTrendSamplesByDateJson("Bearer " + bearerToken, prefixedUrlEncodedTrendId, pageSize, page, startTime, endTime);


        DesigoTrendSampleResult trendSampleResult = TrendSamplesMapper.mapFromJson(trendSamplesJson);
        log.trace("Found: {} trends from trendId: {}", trendSampleResult.getTotal(), objectId);
        List<DesigoTrendSample> trendSamples = trendSampleResult.getItems();
        if (trendSamples != null) {
            for (DesigoTrendSample trendSample : trendSamples) {
                trendSample.setTrendId(objectId);
                log.trace("imported trendSample: {}", trendSample);
            }
        }

         */
        isHealthy = true;
        return new HashSet<>(trendSamples);
    }

    @Override
    public DesigoPresentValue findPresentValue(SensorId sensorId) throws URISyntaxException, SdLogonFailedException {
        DesigoPresentValue presentValue = null;
        String apiUrl = getConfigValue("sd.api.url");
        DesigoSensorId desigoSensorId = (DesigoSensorId) sensorId;
        String objectOrPropertyId = desigoSensorId.getDesigoId() + "." + desigoSensorId.getDesigoPropertyId();
        String bearerToken = findAccessToken();
        URI presentValueUri = new URI(apiUrl + "values/" + objectOrPropertyId);
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet request = null;
        List<DesigoTrendSample> trendSamples = new ArrayList<>();
        try {
            log.trace("findPresentValue. objectOrPropertyId: {}.",objectOrPropertyId);
            request = new HttpGet(presentValueUri);
            request.addHeader(HttpHeaders.ACCEPT, "application/json");
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
            CloseableHttpResponse response = httpClient.execute(request);
            try {
                int httpCode = response.getCode();
                if (httpCode == 200) {
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        String body = EntityUtils.toString(entity);
                        log.trace("Received body: {}", body);
                        if (body != null && body.startsWith("[")) {

                        DesigoPresentValue[] presentValues = PresentValueMapper.mapFromJsonArray(body);
                        if (presentValues != null) {
                            log.trace("Found: {} presentValues from objectOrPropertyId: {}", presentValues.length, objectOrPropertyId);
                            presentValue = presentValues[0];
                        }
                        } else {
                            presentValue = PresentValueMapper.mapFromJson(body);
                        }
                    }
                }
            } catch (Exception e) {
                setUnhealthy();
                throw new DesigoCloudConnectorException("Failed to fetch presentValue for sensorId " + desigoSensorId + ". Reason: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            setUnhealthy();
            throw new DesigoCloudConnectorException("Failed to fetch presentValue for sensorId " + desigoSensorId + ". Reason: " + e.getMessage(), e);
        }
        return presentValue;
    }

    @Override
    public Integer subscribePresentValueChange(String subscriptionId, String objectId) throws URISyntaxException, SdLogonFailedException {
        Integer statusCode = null;
        String apiUrl = getConfigValue("sd.api.url"); //getConfigProperty("sd.api.url");

        String bearerToken = findAccessToken();
        URI subscribeUri = new URI(apiUrl + "objects/" + objectId+"/attributes/presentValue?includeSchema=false");
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet request = null;
        try {
            request = new HttpGet(subscribeUri);
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
            request.addHeader(DESIGO_SUBSCRIBE_HEADER, subscriptionId);
            CloseableHttpResponse response = httpClient.execute(request);
            try {
                HttpEntity entity = response.getEntity();
                statusCode = response.getCode();
                if (statusCode == 202) {
                   log.trace("Subscribing ok for objectId: {}", objectId);
                } else {
                    String body = "";
                    if (entity != null) {
                        body = EntityUtils.toString(entity);
                    }
                    log.trace("Could not subscribe to subscription {} for objectId {} using URL: {}. Status: {}. Body text: {}", subscriptionId, objectId, subscribeUri, statusCode, body);
                }
            } catch (Exception e) {
                setUnhealthy();
                throw new DesigoCloudConnectorException("Failed to subscribe to objectId " + objectId
                        + ". Reason: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            setUnhealthy();
            throw new DesigoCloudConnectorException("Failed to subscribe to objectId " + objectId
                    + ". Reason: " + e.getMessage(), e);
        }
        return statusCode;

    }

    public String findObjectId(String metasysDbReference) throws SdLogonFailedException, URISyntaxException {
        String encodedDbReference = urlEncode(metasysDbReference);
        String bearerToken = findAccessToken();
        ObjectIdentifiersService objectIdentifiersService = RestClientBuilder.newBuilder()
                .baseUri(apiUri)
                .build(ObjectIdentifiersService.class);
        String objectId = objectIdentifiersService.findObjectId("Bearer " + bearerToken, encodedDbReference);
        return objectId;
    }

    String encodeAndPrefix(String trendId) {
        if (trendId != null) {
            return urlEncode(trendId);
        } else {
            return null;
        }
    }

    private String findAccessToken() throws SdLogonFailedException {
        try {
            String accessToken = null;
            if (userToken == null || tokenNeedRefresh()) {
                logon();
            }
            if (userToken != null) {
                accessToken = userToken.getAccessToken();
            } else {
                notificationService.clearService(DESIGO_API);
            }

            return accessToken;
        } catch (SdLogonFailedException e){
            notificationService.sendAlarm(DESIGO_API,LOGON_FAILED);
            isHealthy = false;
            throw e;
        }
    }

    boolean tokenNeedRefresh() {
        if (userToken == null) {
            return true;
        }
        boolean tokenNeedRefresh = userToken.tokenNeedRefresh();
        return tokenNeedRefresh;
    }

    public DesigoUserToken refreshToken() throws SdLogonFailedException {
        DesigoUserToken refreshedUserToken = null;
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String refreshTokenUrl = apiUri + "refreshToken";
        HttpGet request = null;
        String truncatedAccessToken = null;
        try {

            request = new HttpGet(refreshTokenUrl);
            String accessToken = userToken.getAccessToken();
            if (accessToken != null && accessToken.length() > 11) {
                truncatedAccessToken = accessToken.substring(0, 10) + "...";
            } else {
                truncatedAccessToken = accessToken;
            }
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

            log.trace("Try to logon to Desigo at uri: {}",refreshTokenUrl);
            CloseableHttpResponse response = httpClient.execute(request);
            try {
                int httpCode = response.getCode();
                if (httpCode == 200) {
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        String body = EntityUtils.toString(entity);
                        log.trace("Received body: {}", body);
                        userToken = RealEstateObjectMapper.getInstance().getObjectMapper().readValue(body, DesigoUserToken.class);
                        log.trace("Converted to userToken: {}", userToken);
                        refreshedUserToken = userToken;
                        log.debug("Logged on to Desigo. Refreshed accessToken. Expires: {}", userToken.getExpires().toString());
                        setHealthy();
                    }
                } else {
                    String msg = "Failed to refresh userToken to Desigo at uri: " + request.getRequestUri() +
                            ". accessToken: " + truncatedAccessToken +
                            ". ResponseCode: " + httpCode +
                            ". ReasonPhrase: " + response.getReasonPhrase();
                    SdLogonFailedException logonFailedException = new SdLogonFailedException(msg);
                    log.warn("Failed to refresh accessToken on Desigo. Reason {}", logonFailedException.getMessage());
                    setUnhealthy();
                    TemporaryHealthResource.addRegisteredError("Failed to refresh accessToken on Desigo. Reason: " + logonFailedException.getMessage());
                    throw logonFailedException;
                }

            } finally {
                response.close();
            }
        } catch (IOException e) {
            notificationService.sendAlarm(DESIGO_API,HOST_UNREACHABLE);
            String msg = "Failed to refresh accessToken on Desigo at uri: " + refreshTokenUrl + ", with accessToken: " + truncatedAccessToken;
            SdLogonFailedException logonFailedException = new SdLogonFailedException(msg, e);
            log.warn(msg);
            setUnhealthy();
            TemporaryHealthResource.addRegisteredError(msg + " Reason: " + logonFailedException.getMessage());
            throw logonFailedException;
        } catch (ParseException e) {
            notificationService.sendWarning(DESIGO_API,"Parsing of AccessToken information failed.");
            String msg = "Failed to refresh accessToken on Desigo at uri: " + refreshTokenUrl + ", with accessToken: " + truncatedAccessToken +
                    ". Failure parsing the response.";
            SdLogonFailedException logonFailedException = new SdLogonFailedException(msg, e);
            log.warn(msg);
            setUnhealthy();
            TemporaryHealthResource.addRegisteredError(msg + " Reason: " + logonFailedException.getMessage());
            throw logonFailedException;
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                //Do nothing
            }
        }
        return refreshedUserToken;
    }

    @Override
    public void logon() throws SdLogonFailedException {
        String username = getConfigValue("sd.api.username");
        String password = getConfigValue("sd.api.password");
        logon(username, password);
    }
    protected void logon(String username, String password) throws SdLogonFailedException {
        log.trace("Logon: {}", username);
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String loginUri = apiUri + "token";
        HttpPost request = null;
        try {
            request = new HttpPost(loginUri);
            request.addHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");
            request.addHeader(HttpHeaders.ACCEPT, "application/json");
            List<NameValuePair> nvps = new ArrayList<>();
            nvps.add(new BasicNameValuePair("username", username));
            nvps.add(new BasicNameValuePair("password", password));
            nvps.add(new BasicNameValuePair("grant_type", "password"));
            HttpEntity bodyEntity = new UrlEncodedFormEntity(nvps);
            request.setEntity(bodyEntity);

            CloseableHttpResponse response = httpClient.execute(request);
            try {
                int httpCode = response.getCode();
                if (httpCode == 200) {
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        String body = EntityUtils.toString(entity);
                        log.trace("Received body: {}", body);
                        userToken = RealEstateObjectMapper.getInstance().getObjectMapper().readValue(body, DesigoUserToken.class);
                        log.trace("Converted to userToken: {}", userToken);
                        setHealthy();
                        notificationService.clearService(DESIGO_API);
                    }
                } else {
                    String requestHeaders = "";
                    for (NameValuePair nvp : request.getHeaders()) {
                        requestHeaders += nvp.getName() + "=" + nvp.getValue() + ", ";
                    }
                    String responseHeaders = "";
                    for (NameValuePair nvp : response.getHeaders()) {
                        responseHeaders += nvp.getName() + "=" + nvp.getValue() + ", ";
                    }
                    String body = "";
                    if (response.getEntity() != null) {
                        body = EntityUtils.toString(response.getEntity());
                    }
                    String msg = "Failed to logon to Desigo at uri: " + loginUri +
                            ". Username: " + username +
                            ". RequestHeaders: " + requestHeaders +
                            ". RequestMethod: " + request.getMethod() +
                            ". ResponseCode: " + httpCode +
                            ". ReasonPhrase: " + response.getReasonPhrase() +
                            ". ResponseHeaders: " + responseHeaders  +
                            ". Body text: " + body;
                    SdLogonFailedException logonFailedException = new SdLogonFailedException(msg);
                    log.warn("Failed to logon to Desigo. Reason {}", logonFailedException.getMessage());
                    setUnhealthy();
                    notificationService.sendWarning(DESIGO_API,LOGON_FAILED);
                    TemporaryHealthResource.addRegisteredError("Failed to logon to Desigo. Reason: " + logonFailedException.getMessage());
                    throw logonFailedException;
                }

            } finally {
                response.close();
            }
        } catch (IOException e) {
            notificationService.sendAlarm(DESIGO_API,HOST_UNREACHABLE);
            String msg = "Failed to logon to Desigo at uri: " + loginUri + ", with username: " + username;
            SdLogonFailedException logonFailedException = new SdLogonFailedException(msg, e);
            log.warn(msg);
            setUnhealthy();
            TemporaryHealthResource.addRegisteredError(msg + " Reason: " + logonFailedException.getMessage());
            throw logonFailedException;
        } catch (ParseException e) {
            notificationService.sendWarning(DESIGO_API,"Parsing of login information failed.");
            String msg = "Failed to logon to Desigo at uri: " + loginUri + ", with username: " + username +
                    ". Failure parsing the response.";
            SdLogonFailedException logonFailedException = new SdLogonFailedException(msg, e);
            log.warn(msg);
            setUnhealthy();
            TemporaryHealthResource.addRegisteredError(msg + " Reason: " + logonFailedException.getMessage());
            throw logonFailedException;
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                //Do nothing
            }
        }
    }

    @Override
    public boolean isLoggedIn() {
        return userToken != null;
    }

    @Override
    public String getName() {
        return "DesigoApiClientRest";
    }
    void setHealthy() {
        this.isHealthy = true;
        log.debug("Desigo is Healthy");
        TemporaryHealthResource.setHealthy();
    }

    void setUnhealthy() {
        log.warn("Desigo is Unhealthy");
        this.isHealthy = false;
        TemporaryHealthResource.setUnhealthy();
    }


    public boolean isHealthy() {
        return isHealthy;
    }

    @Override
    public long getNumberOfTrendSamplesReceived() {
        return numberOfTrendSamplesReceived;
    }

    synchronized void addNumberOfTrendSamplesReceived() {
        if (numberOfTrendSamplesReceived < Long.MAX_VALUE) {
            numberOfTrendSamplesReceived ++;
        } else {
            numberOfTrendSamplesReceived = 1;
        }
    }

    public UserToken getUserToken() {
        return userToken;
    }
}
