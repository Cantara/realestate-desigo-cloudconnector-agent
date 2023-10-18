package no.cantara.realestate.desigo.cloudconnector.notifications;

public interface NotificationService {
    boolean sendWarning(String service, String warningMessage) ;

    boolean sendAlarm(String service, String alarmMessage);

    boolean clearService(String service);
}
