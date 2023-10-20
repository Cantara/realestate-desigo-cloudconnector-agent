package no.cantara.realestate.desigo.cloudconnector.observations;

public interface PresentValueImporter {

    //Lifecycle events
    void startup();
    void flush();
    void close();

    //Functionality
    void importAllPresentValues();



}
