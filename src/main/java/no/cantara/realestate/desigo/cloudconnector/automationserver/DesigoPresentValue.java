package no.cantara.realestate.desigo.cloudconnector.automationserver;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Objects;

public class DesigoPresentValue {

    /*
    Content model
    [
  {
    "DataType": "string",
    "Value": {
      "Value": "string",
      "DisplayValue": "string",
      "Quality": "string",
      "QualityGood": true,
      "Timestamp": "2023-10-17T13:45:58.564Z",
      "TimestampNotify": "2023-10-17T13:45:58.564Z",
      "IsPropertyAbsent": true
    },
    "OriginalObjectOrPropertyId": "string",
    "ObjectId": "string",
    "PropertyName": "string",
    "AttributeId": "string",
    "ErrorCode": 0,
    "IsArray": true,
    "BackgroundColor": {
      "A": 0,
      "R": 0,
      "G": 0,
      "B": 0
    },
    "SubscriptionKey": 0
  }
]
     */

    /*
    Example content
    [
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
]
     */
    @JsonProperty("PropertyName")
    private String propertyId = null;

    @JsonProperty("DataType")
    private String dataType = null;

    @JsonProperty("Value")
    private Value value;

    @JsonProperty("ObjectId")
    private String objectId;


    public DesigoPresentValue() {
    }

    public String getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(String propertyId) {
        this.propertyId = propertyId;
    }


    public Boolean getReliable() {
        return ((value == null) ? null : value.getIsReliable());
    }


    public void setReliable(Boolean reliable) {

        if (value == null) {
            value = new Value();
        }
        value.setIsReliable(reliable);
    }

    public Instant getSampleDate() {
        return ((value == null) ? null : value.getSampleDate());
    }

    public void setSampleDate(Instant timestamp) {
        if (value == null) {
            value = new Value();
        }
        value.setSampleDate(timestamp);
    }

    public boolean isValid() {
        return true;
    }

    public Number getValue() {
        Number valNum = null;
        if (value != null) {
            valNum = value.getValue();
        }
        return valNum;
    }

    public void setValueDeep(Integer valueDeep) {
        if (value == null) {
            value = new Value();
        }
        value.setValue(valueDeep);
    }


    public void setValue(Value value) {
        this.value = value;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    @Override
    public String toString() {
        return "DesigoPresentValue{" +
                "propertyId='" + propertyId + '\'' +
                ", dataType='" + dataType + '\'' +
                ", value=" + value +
                ", objectId='" + objectId + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DesigoPresentValue that = (DesigoPresentValue) o;
        return Objects.equals(getPropertyId(), that.getPropertyId()) && Objects.equals(getDataType(), that.getDataType()) && Objects.equals(getValue(), that.getValue()) && Objects.equals(getObjectId(), that.getObjectId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPropertyId(), getDataType(), getValue(), getObjectId());
    }
}
