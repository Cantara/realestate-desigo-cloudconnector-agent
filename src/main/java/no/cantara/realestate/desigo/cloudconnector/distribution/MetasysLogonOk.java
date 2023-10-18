package no.cantara.realestate.desigo.cloudconnector.distribution;

public class MetasysLogonOk extends Metric {

    public MetasysLogonOk() {
        super("metrics-metasys-cloudconnector");
        addTag("metasys-api-logon", "ok");
    }
}
