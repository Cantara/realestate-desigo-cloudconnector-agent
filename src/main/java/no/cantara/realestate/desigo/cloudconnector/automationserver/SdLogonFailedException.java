package no.cantara.realestate.desigo.cloudconnector.automationserver;

public class SdLogonFailedException extends Exception {
    public SdLogonFailedException(String msg) {
        super(msg);
    }

    public SdLogonFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
