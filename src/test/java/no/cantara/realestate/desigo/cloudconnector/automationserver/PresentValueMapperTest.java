package no.cantara.realestate.desigo.cloudconnector.automationserver;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class PresentValueMapperTest {

    private String observedValue = """
              {
                "DataType": "BasicFloat",
                "Value": {
                  "Value": "587",
                  "Quality": "9439544818976425217",
                  "QualityGood": true,
                  "Timestamp": "2023-10-17T13:43:57.153Z"
                },
                "OriginalObjectOrPropertyId": "System1:GmsDevice_2_1414052_121634835.RAQual_Present_Value",
                "ObjectId": "System1:GmsDevice_2_1414052_121634835",
                "PropertyName": "RAQual_Present_Value",
                "AttributeId": "System1:GmsDevice_2_1414052_121634835.RAQual_Present_Value:_online.._value",
                "ErrorCode": 0,
                "IsArray": false
              }
            """;
    @Test
    void mapFromJson() {

        DesigoPresentValue presentValue = PresentValueMapper.mapFromJson(observedValue);
        assertNotNull(presentValue);

        assertEquals("System1:GmsDevice_2_1414052_121634835", presentValue.getObjectId());
        assertEquals("RAQual_Present_Value", presentValue.getPropertyId());
        assertEquals(587, presentValue.getValue());
        assertEquals("BasicFloat", presentValue.getDataType());
        assertEquals(true, presentValue.getReliable());
        assertEquals(Instant.parse("2023-10-17T13:43:57.153Z"), presentValue.getSampleDate());
    }
    @Test
    void mapFromJsonArray() {
        String observedValueArr = "[" + observedValue + "]";
        DesigoPresentValue[] presentValues = PresentValueMapper.mapFromJsonArray(observedValueArr);
        assertNotNull(presentValues);
        assertEquals(1, presentValues.length);
        DesigoPresentValue presentValue = presentValues[0];
        assertNotNull(presentValue);

        assertEquals("System1:GmsDevice_2_1414052_121634835", presentValue.getObjectId());
        assertEquals("RAQual_Present_Value", presentValue.getPropertyId());
        assertEquals(587, presentValue.getValue());
        assertEquals("BasicFloat", presentValue.getDataType());
        assertEquals(true, presentValue.getReliable());
        assertEquals(Instant.parse("2023-10-17T13:43:57.153Z"), presentValue.getSampleDate());
    }
}
