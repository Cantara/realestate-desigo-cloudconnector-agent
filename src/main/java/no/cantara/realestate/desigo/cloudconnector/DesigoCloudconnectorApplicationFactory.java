package no.cantara.realestate.desigo.cloudconnector;

import no.cantara.config.ApplicationProperties;
import no.cantara.stingray.application.StingrayApplication;
import no.cantara.stingray.application.StingrayApplicationFactory;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class DesigoCloudconnectorApplicationFactory implements StingrayApplicationFactory<DesigoCloudconnectorApplication> {
    private static final Logger log = getLogger(DesigoCloudconnectorApplicationFactory.class);

    @Override
    public Class<?> providerClass() {
        return DesigoCloudconnectorApplication.class;
    }

    @Override
    public String alias() {
        return "DesigoCloudconnector";
    }

    @Override
    public StingrayApplication<DesigoCloudconnectorApplication> create(ApplicationProperties applicationProperties) {
        return new DesigoCloudconnectorApplication(applicationProperties);
    }

}
