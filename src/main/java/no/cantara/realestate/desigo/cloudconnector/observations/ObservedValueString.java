package no.cantara.realestate.desigo.cloudconnector.observations;

public class ObservedValueString extends ObservedValue<String>{
    public ObservedValueString(String id, String value, String itemReference) {
        super(id, value, itemReference);
    }

    @Override
    public String getValue() {
        return super.value;
    }
}
