package no.cantara.realestate.desigo.cloudconnector.automationserver;

import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TrendSamplesMapperTest {

    @Test
    void mapFromJson() throws ParseException {
        //Test that TrendSamplesMapper can map from json to DesigoTrendSampleResult
        String trendSampleJson = """
                {
                  "Id": "System1:GmsDevice_2_1414052_83886086.general.Data:_offline.._value",
                  "SeriesPropertyId": "System1:GmsDevice_2_1414052_121634835.RAQual_Present_Value",
                  "Series": [
                    {
                      "Value": "455",
                      "Quality": "0",
                      "QualityGood": true,
                      "Timestamp": "2022-09-05T02:45:42.11Z"
                    },
                    {
                      "Value": "455",
                      "Quality": "0",
                      "QualityGood": true,
                      "Timestamp": "2022-09-05T03:00:42.11Z"
                    },
                    {
                      "Value": "455",
                      "Quality": "0",
                      "QualityGood": true,
                      "Timestamp": "2022-09-05T03:15:42.11Z"
                    }
                  ]
                }
                """;
        DesigoTrendSampleResult result = TrendSamplesMapper.mapFromJson(trendSampleJson);
        assertEquals("System1:GmsDevice_2_1414052_83886086.general.Data:_offline.._value", result.getTrendId());
        assertEquals("System1", result.getSystem());
        assertEquals("GmsDevice_2_1414052_121634835", result.getObjectId());
        assertEquals("RAQual_Present_Value", result.getPropertyId());
        List<DesigoTrendSample> series = result.getSeriesWithObjectAndPropertyId();
        assertEquals(3, series.size());
        DesigoTrendSample sample = series.get(0);
        assertEquals("GmsDevice_2_1414052_121634835", sample.getObjectId());
        assertEquals("RAQual_Present_Value", sample.getPropertyId());
        assertEquals(455, sample.getValue().intValue());
        assertEquals("0", sample.getQuality());
        assertEquals(true, sample.isReliable());
        assertEquals(Instant.parse("2022-09-05T02:45:42.11Z"), sample.getSampleDate());


    }
}