package no.cantara.realestate.desigo.cloudconnector.automationserver;

import java.util.Set;

public interface TrendSampleService {
    Set<MetasysTrendSample> findTrendSamples(String s, String prefixedUrlEncodedTrendId, int take, int skip);

    Set<MetasysTrendSample> findTrendSamples(String s, String string);

    String findTrendSamplesByDateJson(String s, String prefixedUrlEncodedTrendId, int pageSize, int page, String startTime, String endTime);
}
