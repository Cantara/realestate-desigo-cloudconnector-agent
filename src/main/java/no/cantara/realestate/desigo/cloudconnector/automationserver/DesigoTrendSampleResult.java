package no.cantara.realestate.desigo.cloudconnector.automationserver;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/*
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
 */

public class DesigoTrendSampleResult {
    @JsonProperty("Id")
    private String trendId;
    @JsonProperty("SeriesPropertyId")
    String systemObjectAndPropertyId;

    @JsonProperty("Series")
    private List<DesigoTrendSample> series;


    public DesigoTrendSampleResult() {
    }


    public List<DesigoTrendSample> getSeriesWithObjectAndPropertyId() {
        String objectId = getObjectId();
        String propertyId = getPropertyId();
        for (DesigoTrendSample item : series) {
            item.setObjectId(objectId);
            item.setPropertyId(propertyId);
        }
        return series;
    }

    /*
    Deliberately not available for clients.
     */
    private void setSeries(List<DesigoTrendSample> series) {
        this.series = series;
    }

    public String getTrendId() {
        return trendId;
    }

    public void setTrendId(String trendId) {
        this.trendId = trendId;
    }

    public String getSystemObjectAndPropertyId() {
        return systemObjectAndPropertyId;
    }

    public void systemObjectAndPropertyId(String systemObjectAndPropertyId) {
        this.systemObjectAndPropertyId = systemObjectAndPropertyId;
    }

    public String getSystem() {
        String system = null;
        if (systemObjectAndPropertyId != null) {
            system = systemObjectAndPropertyId.split("\\:")[0];
        }
        return system;
    }

    public String getObjectAndPropertyId() {
        String seriesAndPropertyId = null;
        if (systemObjectAndPropertyId != null) {
            seriesAndPropertyId = systemObjectAndPropertyId.split("\\:")[1];
        }
        return seriesAndPropertyId;
    }

    public String getObjectId() {
        String objectId = null;
        String objectAndPropertyId = getObjectAndPropertyId();
        if (objectAndPropertyId != null) {
           objectId = objectAndPropertyId.split("\\.")[0];
        }
        return objectId;
    }
    public String getPropertyId() {
        String propertyId = null;
        String objectAndPropertyId = getObjectAndPropertyId();
        if (objectAndPropertyId != null) {
            propertyId = objectAndPropertyId.split("\\.")[1];
        }
        return propertyId;
    }

    public long getTotal() {
        long total = 0;
        if(series != null) {
            total = series.size();
        }
        return total;
    }

    @Override
    public String toString() {
        return "DesigoTrendSampleResult{" +
                "trendId='" + trendId + '\'' +
                ", systemObjectAndPropertyId='" + systemObjectAndPropertyId + '\'' +
                ", series=" + series +
                '}';
    }
}
