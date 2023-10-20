package no.cantara.realestate.desigo.cloudconnector.automationserver;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/*
{
  "access_token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1...",
  "token_type": "bearer",
  "user_name": "jane-doe",
  "user_descriptor": "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1...",
  "user_profile": "DEFAULT.ldl",
  "flex_user_profile": "DEFAULT",
  "user_inactivity_timeout": "0",
  "expires_in": 2591999
}
 */
public class DesigoUserToken extends UserToken {
    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("user_name")
    private String userName;
    @JsonProperty("user_descriptor")
    private String userDescriptor;
    @JsonProperty("expires_in")
    private long validSeconds = -1;



    public DesigoUserToken() {
        super();
        setAccessToken(accessToken);
        setExpires(Instant.now().plusSeconds(validSeconds));
    }

    public int getValidSeconds() {
        return Long.valueOf(validSeconds).intValue();
    }

    public void setValidSeconds(int validSeconds) {
        this.validSeconds = validSeconds;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserDescriptor() {
        return userDescriptor;
    }

    public void setUserDescriptor(String userDescriptor) {
        this.userDescriptor = userDescriptor;
    }

    @Override
    public String toString() {
        return "DesigoUserToken{" +
                "accessToken='" + accessToken + '\'' +
                ", userName='" + userName + '\'' +
                ", userDescriptor='" + userDescriptor + '\'' +
                ", validSeconds=" + validSeconds +
                "} " + super.toString();
    }
}
