package no.cantara.realestate.desigo.cloudconnector.automationserver;

import no.cantara.realestate.json.RealEstateObjectMapper;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class PresentValueMapper {
    private static final Logger log = getLogger(PresentValueMapper.class);

    public static DesigoPresentValue mapFromJson(String presentValueJson) {
        DesigoPresentValue presentValue = null;
        try {
            presentValue = RealEstateObjectMapper.getInstance().getObjectMapper().readValue(presentValueJson, DesigoPresentValue.class);
        } catch (Exception e) {
            log.error("Unable to unmarshal PresentValue: {}",presentValueJson, e);
        }
        return presentValue;
    }
    public static DesigoPresentValue[] mapFromJsonArray(String presentValueJsonArray) {
        DesigoPresentValue[] presentValues = null;
        try {
            presentValues = RealEstateObjectMapper.getInstance().getObjectMapper().readValue(presentValueJsonArray, DesigoPresentValue[].class);
            /*
            if (presentValues != null) {
                String objectUrl = presentValues.getObjectUrl();
                if (objectUrl != null) {
                    int lastSlash = objectUrl.lastIndexOf("/");
                    if (lastSlash > 0) {
                        String objectId = objectUrl.substring(lastSlash + 1);

                        for (DesigoPresentValueResult sample : presentValues.getItems()) {
                            sample.setObjectId(objectId);
                        }
                    }
                }
            }

             */
        } catch (Exception var2) {
            log.error("Unable to unmarshal SensorReading", var2);
        }
        return presentValues;
    }
}
