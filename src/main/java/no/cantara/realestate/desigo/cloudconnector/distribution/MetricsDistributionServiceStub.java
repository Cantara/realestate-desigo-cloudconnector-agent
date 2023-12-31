package no.cantara.realestate.desigo.cloudconnector.distribution;

import no.cantara.realestate.desigo.cloudconnector.automationserver.DesigoPresentValue;
import no.cantara.realestate.desigo.cloudconnector.automationserver.DesigoTrendSample;
import no.cantara.realestate.mappingtable.MappedSensorId;

import java.util.Set;

public class MetricsDistributionServiceStub implements MetricsDistributionClient {
    public MetricsDistributionServiceStub(String measurementName) {
    }

    @Override
    public void sendMetrics(Metric metric) {

    }

    @Override
    public void openDb() {

    }

    @Override
    public void flush() {

    }

    @Override
    public void closeDb() {

    }

    @Override
    public void populate(Set<DesigoTrendSample> trendSamples, MappedSensorId mappedSensorId) {

    }

    @Override
    public void populate(DesigoPresentValue desigoPresentValue, MappedSensorId mappedSensorId) {

    }
}
