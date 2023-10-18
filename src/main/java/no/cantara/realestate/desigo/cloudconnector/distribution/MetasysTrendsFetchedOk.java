package no.cantara.realestate.desigo.cloudconnector.distribution;

public class MetasysTrendsFetchedOk extends Metric {

    public MetasysTrendsFetchedOk(Integer numberOfTrendsFetched) {
        super("metrics-metasys-cloudconnector", numberOfTrendsFetched);
        addTag("metasys-api-trends-fetched", "ok");
    }
}
