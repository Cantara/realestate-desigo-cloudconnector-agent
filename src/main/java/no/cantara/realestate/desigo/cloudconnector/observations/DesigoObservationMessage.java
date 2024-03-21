package no.cantara.realestate.desigo.cloudconnector.observations;

import no.cantara.realestate.desigo.cloudconnector.automationserver.DesigoPresentValue;
import no.cantara.realestate.desigo.cloudconnector.automationserver.DesigoTrendSample;
import no.cantara.realestate.desigo.cloudconnector.sensors.MeasurementUnit;
import no.cantara.realestate.desigo.cloudconnector.sensors.SensorType;
import no.cantara.realestate.mappingtable.MappedSensorId;
import no.cantara.realestate.mappingtable.SensorId;
import no.cantara.realestate.mappingtable.rec.SensorRecObject;
import no.cantara.realestate.observations.ObservationMessage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;

import static no.cantara.realestate.utils.StringUtils.hasValue;

public class DesigoObservationMessage extends ObservationMessage {

    private final DesigoTrendSample trendSample;

    private final DesigoPresentValue presentValue;
    private final MappedSensorId mappedSensorId;

    private final ObservedValueNumber observedValue;

    public DesigoObservationMessage(DesigoTrendSample trendSample, MappedSensorId mappedSensorId) {
        this.trendSample = trendSample;
        this.mappedSensorId = mappedSensorId;
        observedValue = null;
        presentValue = null;
        buildObservation();
    }

    public DesigoObservationMessage(ObservedValueNumber observedValue, MappedSensorId mappedSensorId) {
        this.observedValue = observedValue;
        this.mappedSensorId = mappedSensorId;
        trendSample = null;
        presentValue = null;
        buildObservation();
    }
    public DesigoObservationMessage(DesigoPresentValue presentValue, MappedSensorId mappedSensorId) {
        this.observedValue = null;
        this.mappedSensorId = mappedSensorId;
        trendSample = null;
        this.presentValue = presentValue;
        buildObservation();
    }

    protected void buildObservation() {
        SensorRecObject rec = mappedSensorId.getRec();
        SensorId sensorId = mappedSensorId.getSensorId();
        if (hasValue(sensorId.getId())) {
            setSensorId(sensorId.getId());
        } else {
            setSensorId(rec.getRecId());
        }
        if (rec.getTfm() != null) {
            setTfm(rec.getTfm().getTfm());
        }
        setRealEstate(rec.getRealEstate());
        setBuilding(rec.getBuilding());
        setFloor(rec.getFloor());
        setSection(rec.getSection());
        setServesRoom(rec.getServesRoom());
        setPlacementRoom(rec.getPlacementRoom());
        setSensorType(rec.getSensorType());
        setClimateZone(rec.getClimateZone());
        setElectricityZone(rec.getElectricityZone());
        if (rec.getSensorType() != null) {
            SensorType sensorType = SensorType.from(rec.getSensorType());
            MeasurementUnit measurementUnit = MeasurementUnit.mapFromSensorType(sensorType);
            setMeasurementUnit(measurementUnit.name());
        }

        Number value = null;
        Instant observedAt = null;
        if (trendSample != null) {
            value = trendSample.getValue();
            if (value instanceof BigDecimal) {
                value = ((BigDecimal) value).setScale(2, RoundingMode.CEILING);
            }
            observedAt = trendSample.getSampleDate();
        } else if (observedValue != null) {
            value = observedValue.getValue();
            if (value instanceof BigDecimal) {
                value = ((BigDecimal) value).setScale(2, RoundingMode.CEILING);
            }
            observedAt = observedValue.getObservedAt();
        } else if (presentValue != null) {

            value = presentValue.getValue();
            if (value instanceof BigDecimal) {
                value = ((BigDecimal) value).setScale(2, RoundingMode.CEILING);
            }
            observedAt = presentValue.getSampleDate();
        }
        setObservationTime(observedAt);
        Instant receivedAt = Instant.now();
        setValue(value);
        setReceivedAt(receivedAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DesigoObservationMessage that = (DesigoObservationMessage) o;
        return Objects.equals(trendSample, that.trendSample) && Objects.equals(mappedSensorId, that.mappedSensorId) && Objects.equals(observedValue, that.observedValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(trendSample, mappedSensorId, observedValue);
    }

    @Override
    public String toString() {
        return "DesigoObservationMessage{" +
                "trendSample=" + trendSample +
                ", mappedSensorId=" + mappedSensorId +
                ", observedValue=" + observedValue +
                "} " + super.toString();
    }
}
