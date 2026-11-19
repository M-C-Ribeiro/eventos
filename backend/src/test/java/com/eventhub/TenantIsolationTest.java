package com.eventhub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Garante que uma organização nunca enxerga ou altera dados de outra. */
@SpringBootTest
@AutoConfigureMockMvc
class TenantIsolationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    String tokenA;
    String tokenB;
    String slugA;
    String slugB;

    @BeforeEach
    void setUp() throws Exception {
        slugA = "org-a-" + shortId();
        slugB = "org-b-" + shortId();
        tokenA = register(slugA, "admin@a.com");
        tokenB = register(slugB, "admin@b.com");
    }

    @Test
    void eventsAreIsolatedBetweenTenants() throws Exception {
        long eventId = createEvent(tokenA, "Conferência A");

        // A vê o próprio evento
        mvc.perform(auth(get("/api/events"), tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // B não vê nada na listagem
        mvc.perform(auth(get("/api/events"), tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // B não consegue ler, editar, excluir nem inscrever no evento de A
        mvc.perform(auth(get("/api/events/" + eventId), tokenB)).andExpect(status().isNotFound());
        mvc.perform(auth(put("/api/events/" + eventId), tokenB)
                        .content(eventJson("Invadido")))
                .andExpect(status().isNotFound());
        mvc.perform(auth(delete("/api/events/" + eventId), tokenB)).andExpect(status().isNotFound());
        mvc.perform(auth(post("/api/events/" + eventId + "/registrations"), tokenB)
                        .content(json.writeValueAsString(Map.of("name", "X", "email", "x@x.com"))))
                .andExpect(status().isNotFound());

        // O evento continua intacto para A
        mvc.perform(auth(get("/api/events/" + eventId), tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Conferência A"));
    }

    @Test
    void usersAreIsolatedAndSameEmailCanExistInTwoTenants() throws Exception {
        // O mesmo e-mail pode existir em organizações diferentes
        createUser(tokenA, "maria@exemplo.com");
        createUser(tokenB, "maria@exemplo.com");

        mvc.perform(auth(get("/api/users"), tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // Login escolhe o usuário certo pela organização
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "organizationSlug", slugB, "email", "maria@exemplo.com", "password", "senha-segura-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.tenant.slug").value(slugB));

        // Perfis de acesso também são isolados
        long roleOfA = TestSupport.roleId(mvc, json, tokenA, "Membro");
        mvc.perform(auth(delete("/api/roles/" + roleOfA), tokenB)).andExpect(status().isNotFound());

        // Admin de B não acessa usuário de A
        long userOfA = json.readTree(mvc.perform(auth(get("/api/users"), tokenA)).andReturn()
                .getResponse().getContentAsString()).get(0).get("id").asLong();
        mvc.perform(auth(delete("/api/users/" + userOfA), tokenB)).andExpect(status().isNotFound());
    }

    @Test
    void capacityIsEnforced() throws Exception {
        String body = json.writeValueAsString(Map.of(
                "title", "Workshop", "startAt", "2030-01-10T09:00:00", "endAt", "2030-01-10T12:00:00",
                "capacity", 1, "status", "PUBLISHED"));
        long id = readId(mvc.perform(auth(post("/api/events"), tokenA).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());

        String reg = "/api/events/" + id + "/registrations";
        mvc.perform(auth(post(reg), tokenA).content(json.writeValueAsString(Map.of("name", "Ana", "email", "ana@x.com"))))
                .andExpect(status().isCreated());
        mvc.perform(auth(post(reg), tokenA).content(json.writeValueAsString(Map.of("name", "Bia", "email", "bia@x.com"))))
                .andExpect(status().isConflict());
    }

    @Test
    void requestsWithoutTokenAreRejected() throws Exception {
        mvc.perform(get("/api/events")).andExpect(status().isUnauthorized());
    }

    // ---------- helpers ----------

    private String register(String slug, String email) throws Exception {
        String body = json.writeValueAsString(Map.of(
                "organizationName", "Org " + slug, "organizationSlug", slug,
                "name", "Admin", "email", email, "password", "senha-segura-1"));
        String response = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String token = json.readTree(response).get("token").asText();
        assertThat(token).isNotBlank();
        return token;
    }

    private long createEvent(String token, String title) throws Exception {
        String response = mvc.perform(auth(post("/api/events"), token).content(eventJson(title)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return readId(response);
    }

    private void createUser(String token, String email) throws Exception {
        long memberRole = TestSupport.roleId(mvc, json, token, "Membro");
        mvc.perform(auth(post("/api/users"), token).content(json.writeValueAsString(Map.of(
                        "name", "Maria", "email", email, "password", "senha-segura-1", "roleIds", java.util.List.of(memberRole)))))
                .andExpect(status().isCreated());
    }

    private String eventJson(String title) throws Exception {
        return json.writeValueAsString(Map.of(
                "title", title, "startAt", "2030-05-01T09:00:00", "endAt", "2030-05-01T18:00:00",
                "status", "PUBLISHED"));
    }

    private long readId(String body) throws Exception {
        JsonNode node = json.readTree(body);
        return node.get("id").asLong();
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder builder, String token) {
        return builder.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }

    private static String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
