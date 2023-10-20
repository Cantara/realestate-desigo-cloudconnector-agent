package no.cantara.realestate.desigo.cloudconnector.automationserver;

import org.slf4j.Logger;

import java.time.Instant;

import static org.slf4j.LoggerFactory.getLogger;

public class UserToken {
    private static final Logger log = getLogger(UserToken.class);
    private String accessToken;

    private Instant expires;
    private Instant createdAt;
    private String refreshToken;

    public UserToken() {
        createdAt = Instant.now();
    }
    public UserToken(String accessToken, Instant expires, String refreshToken) {
        this.accessToken = accessToken;
        this.expires = expires;
        this.createdAt = Instant.now();
        this.refreshToken = refreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public Instant getExpires() {
        return expires;
    }

    public void setExpires(Instant expires) {
        log.trace("***Setting expires to {}", expires);
        this.expires = expires;
    }

    public int getValidSeconds() {
        return getExpires().compareTo(Instant.now()) ;
    }


    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean tokenNeedRefresh() {
        try {
            long validSeconds = getValidSeconds();
            if (validSeconds < 30) {
                return true;
            }
            if (accessToken == null || accessToken.isEmpty()) {
                return true;
            }
        } catch (Exception e) {
            //#11 FIXME NPE for expires
            log.warn("Unable to check if token need refresh. This will cause login-attempt on every request to the Desigo Server", e);
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "UserToken{" +
                "accessToken='" + accessToken + '\'' +
                ", expires=" + expires +
                ", validSeconds=" + getValidSeconds() +
                ", createdAt=" + createdAt +
                '}';
    }
}
