package com.solvix.backend.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.solvix.backend.domain.kb.ArticleStatus;
import com.solvix.backend.web.kb.KbArticleController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class KbArticleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Support agent can create and publish KB article")
    @WithMockUser(username = "agent1", roles = "SUPPORT_AGENT")
    void createAndPublishArticle() throws Exception {
        KbArticleController.CreateArticleRequest createReq = new KbArticleController.CreateArticleRequest(
                "How to Reset Password",
                "Follow these steps to reset your customer account password...",
                "Account Management",
                Set.of("auth", "password", "security")
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/kb/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("How to Reset Password"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.tags", hasItem("password")))
                .andReturn();

        KbArticleController.ArticleResponse createdArticle = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), KbArticleController.ArticleResponse.class
        );

        // Publish article
        mockMvc.perform(patch("/api/v1/kb/articles/" + createdArticle.id() + "/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.publishedAt").nonNull());

        // Get details (increments view count)
        mockMvc.perform(get("/api/v1/kb/articles/" + createdArticle.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewCount").value(1));
    }

    @Test
    @DisplayName("Search articles by tag and keyword")
    @WithMockUser(username = "agent1", roles = "SUPPORT_AGENT")
    void searchArticlesByTagAndKeyword() throws Exception {
        KbArticleController.CreateArticleRequest req1 = new KbArticleController.CreateArticleRequest(
                "Kafka Queue Lag Debugging",
                "Detailed instructions for diagnosing consumer group offsets",
                "Infrastructure",
                Set.of("kafka", "queue", "devops")
        );
        KbArticleController.CreateArticleRequest req2 = new KbArticleController.CreateArticleRequest(
                "PostgreSQL Connection Pooling",
                "Optimizing HikariCP connection pool settings for high throughput",
                "Database",
                Set.of("postgres", "database", "devops")
        );

        mockMvc.perform(post("/api/v1/kb/articles").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req1)));
        mockMvc.perform(post("/api/v1/kb/articles").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req2)));

        mockMvc.perform(get("/api/v1/kb/articles")
                        .param("search", "Kafka")
                        .param("tag", "kafka"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Kafka Queue Lag Debugging"));
    }
}
