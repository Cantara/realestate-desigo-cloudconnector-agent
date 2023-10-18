package no.cantara.realestate.desigo.cloudconnector.distribution;

public class MetasysLogonFailed extends Metric {

    public MetasysLogonFailed() {
        super("metrics-metasys-cloudconnector");
        addTag("metasys-api-logon", "failed");
    }
}
