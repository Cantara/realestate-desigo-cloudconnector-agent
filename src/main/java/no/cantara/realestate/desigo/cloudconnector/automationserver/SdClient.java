package no.cantara.realestate.desigo.cloudconnector.automationserver;

import no.cantara.realestate.mappingtable.SensorId;

import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Set;

public interface SdClient {
    Set<DesigoTrendSample> findTrendSamples(String bearerToken, String trendId) throws URISyntaxException;

    Set<DesigoTrendSample> findTrendSamples(String trendId, int take, int skip) throws URISyntaxException, SdLogonFailedException;

    Set<DesigoTrendSample> findTrendSamplesByDate(String trendId, int take, int skip, Instant onAndAfterDateTime) throws URISyntaxException, SdLogonFailedException;

    Integer subscribePresentValueChange(String subscriptionId, String objectId) throws URISyntaxException, SdLogonFailedException;

    void logon() throws SdLogonFailedException;

    boolean isLoggedIn();

    String getName();

    boolean isHealthy();

    long getNumberOfTrendSamplesReceived();

    UserToken getUserToken();

    UserToken refreshToken() throws SdLogonFailedException;

    DesigoPresentValue findPresentValue(SensorId sensorId) throws URISyntaxException, SdLogonFailedException;
}
