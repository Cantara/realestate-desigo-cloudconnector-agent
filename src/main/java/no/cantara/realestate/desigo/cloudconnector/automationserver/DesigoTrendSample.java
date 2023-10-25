package no.cantara.realestate.desigo.cloudconnector.automationserver;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.cantara.realestate.RealEstateException;

import java.text.NumberFormat;
import java.text.ParseException;
import java.time.Instant;
import java.util.Objects;

/*
    {
     "Value": "455",
     "Quality": "0",
     "QualityGood": true,
     "Timestamp": "2022-09-05T02:45:42.11Z"
    }
 */
public class DesigoTrendSample {
    private String trendId = null;

    @JsonProperty("Quality")
    private String quality = null;
    @JsonProperty("QualityGood")
    private Boolean isReliable = null;
    @JsonProperty("Timestamp")
    private Instant sampleDate;

    @JsonProperty("Value")
    private String value;
    private String objectId;

    private String propertyId;

    public DesigoTrendSample() {
    }

    public String getTrendId() {
        return trendId;
    }

    public void setTrendId(String trendId) {
        this.trendId = trendId;
    }




    public Instant getSampleDate() {
        return sampleDate;
    }

//    public void setSampleDate(Instant sampleDate) {
//        this.sampleDate = sampleDate;
//    }

    public void setTimestamp(String timestamp) {
//        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
//        LocalDate parsedDate = LocalDate.parse(timestamp, formatter);
        this.sampleDate = Instant.parse(timestamp);
    }

    public String getQuality() {
        return quality;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }

    public Boolean isReliable() {
        return isReliable;
    }

    public void setReliable(Boolean reliable) {
        isReliable = reliable;
    }

    public void setSampleDate(Instant sampleDate) {
        this.sampleDate = sampleDate;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(String propertyId) {
        this.propertyId = propertyId;
    }

    public boolean isValid() {
        return true; //FIXME validate DesigoTrendSample
    }

    public Number getValue() throws RealEstateException {
        Number valNum = null;
        if (value != null) {
            try {
                valNum = NumberFormat.getInstance().parse(value);
            } catch (ParseException e) {
                throw  new RealEstateException("Failed to parse value: " + value + ". Consider use getValueAsString()", e);
            }
        }
        return valNum;
    }

    public String getValueAsString() {
        return value;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getObjectId() {
        return objectId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DesigoTrendSample that = (DesigoTrendSample) o;
        return Objects.equals(getTrendId(), that.getTrendId()) && Objects.equals(getQuality(), that.getQuality()) && Objects.equals(isReliable, that.isReliable) && Objects.equals(getSampleDate(), that.getSampleDate()) && Objects.equals(getValue(), that.getValue()) && Objects.equals(getObjectId(), that.getObjectId()) && Objects.equals(getPropertyId(), that.getPropertyId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTrendId(), getQuality(), isReliable, getSampleDate(), getValue(), getObjectId(), getPropertyId());
    }

    @Override
    public String toString() {
        return "DesigoTrendSample{" +
                "trendId='" + trendId + '\'' +
                ", quality='" + quality + '\'' +
                ", isReliable=" + isReliable +
                ", sampleDate=" + sampleDate +
                ", value='" + value + '\'' +
                ", objectId='" + objectId + '\'' +
                ", propertyId='" + propertyId + '\'' +
                '}';
    }
}
