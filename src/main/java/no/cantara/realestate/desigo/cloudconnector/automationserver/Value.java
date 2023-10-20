package no.cantara.realestate.desigo.cloudconnector.automationserver;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Objects;

public class Value {
    /*
    "DataType": "BasicFloat",
    "Value": {
      "Value": "587",
      "Quality": "9439544818976425217",
      "QualityGood": true,
      "Timestamp": "2023-10-17T13:43:57.153Z"
    }
     */
    @JsonProperty("Value")
    private Number value;
    @JsonProperty("Timestamp")
    private Instant sampleDate;

    @JsonProperty("QualityGood")
    private Boolean isReliable;



    public Value() {

    }

    public Number getValue() {
        return value;
    }

    public void setValue(Number value) {
        this.value = value;
    }

    public Instant getSampleDate() {
        return sampleDate;
    }

    public void setSampleDate(Instant sampleDate) {
        this.sampleDate = sampleDate;
    }

    public Boolean getIsReliable() {
        return isReliable;
    }

    public void setIsReliable(Boolean isReliable) {
        this.isReliable = isReliable;
    }

    @Override
    public String toString() {
        return "Value{" +
                "value=" + value +
                ", sampleDate=" + sampleDate +
                ", isReliable=" + isReliable +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Value value1 = (Value) o;
        return Objects.equals(getValue(), value1.getValue()) && Objects.equals(getSampleDate(), value1.getSampleDate()) && Objects.equals(getIsReliable(), value1.getIsReliable());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getValue(), getSampleDate(), getIsReliable());
    }
}
