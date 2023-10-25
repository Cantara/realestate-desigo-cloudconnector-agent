package no.cantara.realestate.desigo.cloudconnector.automationserver;

import no.cantara.realestate.json.RealEstateObjectMapper;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class TrendSamplesMapper {
    private static final Logger log = getLogger(TrendSamplesMapper.class);
    public static DesigoTrendSampleResult mapFromJson(String trendSampleJson) {
        DesigoTrendSampleResult result = null;
        try {
            result = RealEstateObjectMapper.getInstance().getObjectMapper().readValue(trendSampleJson, DesigoTrendSampleResult.class);
            /*
            if (result != null) {
                String objectUrl = result.getObjectUrl();
                if (objectUrl != null) {
                    int lastSlash = objectUrl.lastIndexOf("/");
                    if (lastSlash > 0) {
                        String objectId = objectUrl.substring(lastSlash + 1);

                        for (DesigoTrendSample sample : result.getItems()) {
                            sample.setObjectId(objectId);
                        }
                    }
                }

            }

             */
        } catch (Exception var2) {
            log.error("Unable to unmarshal SensorReading", var2);
        }
        return result;
    }
}
