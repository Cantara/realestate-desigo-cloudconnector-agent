package no.cantara.realestate.desigo.cloudconnector.automationserver;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.cantara.realestate.json.RealEstateObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneOffset;

import static java.lang.Math.abs;
import static org.junit.jupiter.api.Assertions.*;

class UserTokenTest {
    private String desigoToken = """
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
            """;

    @Test
    void verifyUserToken() throws JsonProcessingException {
        Instant expectedExpires = Instant.now().plusSeconds(2591999);
        DesigoUserToken userToken = RealEstateObjectMapper.getInstance().getObjectMapper().readValue(desigoToken, DesigoUserToken.class);
        assertNotNull(userToken);
        assertEquals(2591999, userToken.getValidSeconds());
        assertEquals("jane-doe", userToken.getUserName());
        assertEquals("eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1...", userToken.getAccessToken());
        assertEquals("eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1...", userToken.getUserDescriptor());

        assertEquals(expectedExpires.atZone(ZoneOffset.UTC).getMinute(), userToken.getExpires().atZone(ZoneOffset.UTC).getMinute());
        int secDiff = abs(expectedExpires.atZone(ZoneOffset.UTC).getSecond() - userToken.getExpires().atZone(ZoneOffset.UTC).getSecond());
        assertTrue(secDiff < 2,"Expected difference in seconds to be less than 2, but was " + secDiff);
        assertFalse(userToken.tokenNeedRefresh());
        userToken.setValidSeconds(20);
        assertTrue(userToken.tokenNeedRefresh());
    }
}