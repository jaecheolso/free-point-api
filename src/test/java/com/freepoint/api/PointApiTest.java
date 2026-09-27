package com.freepoint.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freepoint.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class PointApiTest extends IntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void 명세_예시를_API_로_수행한다() throws Exception {
        String a = post("/api/points/earn", """
                {"userId": 1, "amount": 1000, "expiresAt": "%s", "requestId": "%s"}
                """.formatted(now().plusDays(10), requestId()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String aKey = objectMapper.readTree(a).get("pointKey").asText();

        post("/api/points/earn", """
                {"userId": 1, "amount": 500, "requestId": "%s"}
                """.formatted(requestId()))
                .andExpect(status().isOk());

        JsonNode c = json(post("/api/points/use", """
                {"userId": 1, "orderNo": "A1234", "amount": 1200, "requestId": "%s"}
                """.formatted(requestId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usedLots[0].earnPointKey").value(aKey))
                .andExpect(jsonPath("$.usedLots[0].amount").value(1000))
                .andExpect(jsonPath("$.usedLots[1].amount").value(200)));

        clock.advance(Duration.ofDays(10));

        post("/api/points/use/" + c.get("pointKey").asText() + "/cancel", """
                {"amount": 1100, "requestId": "%s"}
                """.formatted(requestId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restoredLots[0].restoreType").value("REISSUED"))
                .andExpect(jsonPath("$.restoredLots[1].restoreType").value("RESTORED"))
                .andExpect(jsonPath("$.remainingCancelableAmount").value(100));

        mockMvc.perform(get("/api/points/users/1/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1400));
    }

    @Test
    void 관리자_수기지급은_MANUAL_로_적립된다() throws Exception {
        post("/api/admin/points/manual-earn", """
                {"userId": 1, "amount": 1000, "grantedBy": "admin01", "requestId": "%s", "memo": "CS 보상"}
                """.formatted(requestId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("MANUAL"));
    }

    @Test
    void 적립을_취소한다() throws Exception {
        String earnKey = json(post("/api/points/earn", """
                {"userId": 1, "amount": 1000, "requestId": "%s"}
                """.formatted(requestId()))).get("pointKey").asText();

        post("/api/points/earn/" + earnKey + "/cancel", """
                {"requestId": "%s"}
                """.formatted(requestId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.earnPointKey").value(earnKey))
                .andExpect(jsonPath("$.amount").value(1000));
    }

    @Test
    void 도메인_규칙_위반은_에러코드와_메시지로_응답한다() throws Exception {
        post("/api/points/use", """
                {"userId": 1, "orderNo": "O-1", "amount": 1, "requestId": "%s"}
                """.formatted(requestId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_BALANCE"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void 없는_계정은_404_로_응답한다() throws Exception {
        mockMvc.perform(get("/api/points/users/999/balance"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void 필수값이_없으면_400_으로_응답한다() throws Exception {
        post("/api/points/earn", """
                {"userId": 1, "amount": 1000}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value(containsString("requestId")));
    }

    @Test
    void 요청_본문을_해석할_수_없으면_400_으로_응답한다() throws Exception {
        post("/api/points/earn", """
                {"userId": 1, "amount": "천원", "requestId": "r-1"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void Swagger_문서가_생성된다() throws Exception {
        String docs = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(docs).contains("/api/points/earn", "/api/points/use", "/api/admin/points/manual-earn");
    }

    private ResultActions post(String url, String body) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private JsonNode json(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private String requestId() {
        return UUID.randomUUID().toString();
    }
}
