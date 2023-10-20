package no.cantara.realestate.desigo.cloudconnector.distribution;

import no.cantara.realestate.desigo.cloudconnector.automationserver.DesigoPresentValue;
import no.cantara.realestate.desigo.cloudconnector.automationserver.MetasysTrendSample;
import no.cantara.realestate.mappingtable.MappedSensorId;

import java.util.Set;

public interface MetricsDistributionClient {
    void sendMetrics(Metric metric);

    void openDb();

    void flush();

    void closeDb();

    void populate(Set<MetasysTrendSample> trendSamples, MappedSensorId mappedSensorId);
    void populate(DesigoPresentValue desigoPresentValue, MappedSensorId mappedSensorId);
}
