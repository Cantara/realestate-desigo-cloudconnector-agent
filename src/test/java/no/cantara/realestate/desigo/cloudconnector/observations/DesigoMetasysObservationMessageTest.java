package no.cantara.realestate.desigo.cloudconnector.observations;

import no.cantara.realestate.desigo.cloudconnector.automationserver.DesigoTrendSample;
import no.cantara.realestate.mappingtable.MappedSensorId;
import no.cantara.realestate.mappingtable.SensorId;
import no.cantara.realestate.mappingtable.rec.SensorRecObject;
import no.cantara.realestate.mappingtable.tfm.Tfm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DesigoMetasysObservationMessageTest {

    private DesigoTrendSample trendSample;
    private MappedSensorId mappedSensorId;
    private SensorRecObject rec;
    private SensorId sensorId;

    @BeforeEach
    void setUp() {
        trendSample = mock(DesigoTrendSample.class);
        mappedSensorId = mock(MappedSensorId.class);
        rec = mock(SensorRecObject.class);
        sensorId = mock(SensorId.class);
        when(mappedSensorId.getSensorId()).thenReturn(sensorId);
        when(mappedSensorId.getRec()).thenReturn(rec);
    }

    @Test
    void setSensorIdWhenEmpty() {

        when(sensorId.getId()).thenReturn(null);
        when(rec.getRecId()).thenReturn("rec1");
        DesigoObservationMessage message = new DesigoObservationMessage(trendSample, mappedSensorId);
        assertEquals("rec1", message.getSensorId());
    }

    @Test
    void verifyTfmIsSet() {
        Tfm tfm = new Tfm("testTfm");
        when(rec.getTfm()).thenReturn(tfm);
        DesigoObservationMessage message = new DesigoObservationMessage(trendSample, mappedSensorId);
        assertEquals("testTfm", message.getTfm());

    }

    @Test
    void verifySensorIdIsSet() {
        when(mappedSensorId.getSensorId()).thenReturn(sensorId);
        when(sensorId.getId()).thenReturn("sensorId1");
        assertEquals("sensorId1", mappedSensorId.getSensorId().getId());
        DesigoObservationMessage message = new DesigoObservationMessage(trendSample, mappedSensorId);
        assertEquals("sensorId1", message.getSensorId());
    }
}