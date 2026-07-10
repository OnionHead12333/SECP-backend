package com.smartelderly.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class BackendIntegrationIT {

    private static final AtomicInteger SEQ = new AtomicInteger(1000);
    private static final String PASSWORD = "Test@123456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void familyCareHappyPath_shouldCoverAuthBindingWaterLocationAndSos() throws Exception {
        TestUsers users = newUsers();

        postJson("/v1/auth/register", Map.of(
                        "username", users.elderUsername(),
                        "password", PASSWORD,
                        "role", "elder",
                        "phone", users.elderPhone(),
                        "name", "IT Elder"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.role").value("elder"));

        postJson("/v1/auth/register-child-with-elders", Map.of(
                        "child", Map.of(
                                "name", "IT Child",
                                "phone", users.childPhone(),
                                "password", PASSWORD),
                        "elders", new Object[] {
                                Map.of(
                                        "name", "IT Elder",
                                        "phone", users.elderPhone(),
                                        "relation", "child")
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.role").value("child"))
                .andExpect(jsonPath("$.data.familyCount").value(1));

        String elderToken = loginAndToken(users.elderUsername(), PASSWORD);
        String childToken = loginAndToken(users.childPhone(), PASSWORD);

        MvcResult boundResult = mockMvc.perform(get("/v1/child/bound-elders")
                        .header("Authorization", bearer(childToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].phone").value(users.elderPhone()))
                .andReturn();
        long elderProfileId = read(boundResult).at("/data/0/elderProfileId").asLong();

        MvcResult reminderResult = postJsonWithToken("/v1/child/water-reminders", childToken, Map.of(
                        "elderProfileId", elderProfileId,
                        "title", "Morning water",
                        "dailyTargetMl", 1500,
                        "intervalMinutes", 60,
                        "startTime", "08:00:00",
                        "endTime", "20:00:00",
                        "remindTime", "2026-06-06T09:00:00Z",
                        "sourceType", "child",
                        "status", "pending",
                        "createdBy", "child"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.elderProfileId").value(elderProfileId))
                .andReturn();
        long reminderId = read(reminderResult).at("/data/id").asLong();

        mockMvc.perform(get("/v1/child/water-reminders")
                        .param("elderProfileId", String.valueOf(elderProfileId))
                        .header("Authorization", bearer(childToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(reminderId));

        postJsonWithToken("/v1/elder/location-tracks", elderToken, Map.of(
                        "latitude", 39.9042,
                        "longitude", 116.4074,
                        "locationType", "outdoor",
                        "source", "gps",
                        "recordedAt", Instant.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.locationId").exists());

        mockMvc.perform(get("/v1/child/elders/{elderId}/location-summary", elderProfileId)
                        .header("Authorization", bearer(childToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.latitude").value(39.9042))
                .andExpect(jsonPath("$.data.longitude").value(116.4074));

        MvcResult alertResult = postJsonWithToken("/v1/elder/emergency-alerts", elderToken, Map.of(
                        "alertType", "sos",
                        "triggerMode", "button",
                        "remark", "integration test sos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("pending_revoke"))
                .andReturn();
        long alertId = read(alertResult).at("/data/alertId").asLong();

        mockMvc.perform(get("/v1/elder/emergency-alerts/{alertId}", alertId)
                        .header("Authorization", bearer(elderToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.alertId").value(alertId));
    }

    @Test
    void loginWithWrongPassword_shouldReturnBusinessUnauthorizedCode() throws Exception {
        TestUsers users = newUsers();
        registerElder(users);

        postJson("/v1/auth/login", Map.of(
                        "username", users.elderUsername(),
                        "password", "WrongPassword"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void privateApiWithoutToken_shouldReturnUnauthorizedCode() throws Exception {
        mockMvc.perform(get("/v1/child/bound-elders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4010));
    }

    @Test
    void roleCrossAccess_shouldReturnForbiddenCode() throws Exception {
        TestUsers users = newUsers();
        registerElder(users);
        registerChildWithElder(users);

        String elderToken = loginAndToken(users.elderUsername(), PASSWORD);
        String childToken = loginAndToken(users.childPhone(), PASSWORD);

        mockMvc.perform(get("/v1/child/bound-elders")
                        .header("Authorization", bearer(elderToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4030));

        postJsonWithToken("/v1/elder/location-tracks", childToken, Map.of(
                        "latitude", 39.9042,
                        "longitude", 116.4074,
                        "locationType", "outdoor",
                        "source", "gps",
                        "recordedAt", Instant.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4030));
    }

    @Test
    void invalidRequestParameters_shouldReturnValidationCode() throws Exception {
        postJson("/v1/auth/register", Map.of(
                        "username", "bad-phone-user",
                        "password", PASSWORD,
                        "role", "elder",
                        "phone", "123",
                        "name", "Bad Phone"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4000));
    }

    private void registerElder(TestUsers users) throws Exception {
        postJson("/v1/auth/register", Map.of(
                        "username", users.elderUsername(),
                        "password", PASSWORD,
                        "role", "elder",
                        "phone", users.elderPhone(),
                        "name", "IT Elder"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private void registerChildWithElder(TestUsers users) throws Exception {
        postJson("/v1/auth/register-child-with-elders", Map.of(
                        "child", Map.of(
                                "name", "IT Child",
                                "phone", users.childPhone(),
                                "password", PASSWORD),
                        "elders", new Object[] {
                                Map.of(
                                        "name", "IT Elder",
                                        "phone", users.elderPhone(),
                                        "relation", "child")
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private String loginAndToken(String username, String password) throws Exception {
        MvcResult result = postJson("/v1/auth/login", Map.of(
                        "username", username,
                        "password", password))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn();
        return read(result).at("/data/token").asText();
    }

    private org.springframework.test.web.servlet.ResultActions postJson(String path, Object body) throws Exception {
        return mockMvc.perform(post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8)
                .content(objectMapper.writeValueAsString(body)));
    }

    private org.springframework.test.web.servlet.ResultActions postJsonWithToken(
            String path,
            String token,
            Object body) throws Exception {
        return mockMvc.perform(post(path)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8)
                .content(objectMapper.writeValueAsString(body)));
    }

    private JsonNode read(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static TestUsers newUsers() {
        int suffix = SEQ.incrementAndGet();
        return new TestUsers(
                "it_elder_" + suffix,
                "13988" + String.format("%06d", suffix),
                "13888" + String.format("%06d", suffix));
    }

    private record TestUsers(String elderUsername, String elderPhone, String childPhone) {
    }
}
