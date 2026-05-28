package com.bharathisilks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper om;

    @Test
    void apiRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/state")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/products")).andExpect(status().isUnauthorized());
    }

    @Test
    void configIsPublicAndReportsAvailableMethods() throws Exception {
        JsonNode cfg = om.readTree(mvc.perform(get("/api/auth/config"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertFalse(cfg.path("googleEnabled").asBoolean(), "no client id configured in tests");
        assertTrue(cfg.path("otpDevMode").asBoolean(), "dev code is exposed by default");
    }

    @Test
    void otpLoginIssuesUsableToken() throws Exception {
        String phone = "9991112222";

        JsonNode requested = om.readTree(mvc.perform(post("/api/auth/otp/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("phone", phone))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        String code = requested.path("devCode").asText();
        assertTrue(code.matches("\\d{6}"), "dev code should be a 6-digit number");

        JsonNode auth = om.readTree(mvc.perform(post("/api/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("phone", phone, "code", code))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        String jwt = auth.path("token").asText();
        assertFalse(jwt.isBlank(), "a JWT should be issued");
        assertEquals("phone", auth.path("user").path("provider").asText());

        // The token unlocks the gated API...
        mvc.perform(get("/api/state").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());

        // ...and identifies the signed-in user.
        JsonNode me = om.readTree(mvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertEquals(phone, me.path("phone").asText());
    }

    @Test
    void wrongCodeIsRejected() throws Exception {
        String phone = "9993334444";
        mvc.perform(post("/api/auth/otp/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("phone", phone))))
                .andExpect(status().isOk());

        mvc.perform(post("/api/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("phone", phone, "code", "000000"))))
                .andExpect(status().isBadRequest());
    }
}
