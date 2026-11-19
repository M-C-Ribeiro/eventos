package com.eventhub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Atalhos usados pelos testes de integração. */
final class TestSupport {

    static final String PASSWORD = "senha-segura-1";

    private TestSupport() {
    }

    static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder builder, String token) {
        return builder.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }

    static String newSlug(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    static String register(MockMvc mvc, ObjectMapper json, String slug, String email) throws Exception {
        String body = json.writeValueAsString(Map.of(
                "organizationName", "Org " + slug, "organizationSlug", slug,
                "name", "Admin", "email", email, "password", PASSWORD));
        String response = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("token").asText();
    }

    static String login(MockMvc mvc, ObjectMapper json, String slug, String email) throws Exception {
        String response = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("organizationSlug", slug, "email", email, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("token").asText();
    }

    static long roleId(MockMvc mvc, ObjectMapper json, String token, String name) throws Exception {
        return findIdByName(mvc, json, token, "/api/roles", name);
    }

    static long functionId(MockMvc mvc, ObjectMapper json, String token, String name) throws Exception {
        return findIdByName(mvc, json, token, "/api/team-functions", name);
    }

    static long createUser(MockMvc mvc, ObjectMapper json, String adminToken, String email, List<Long> roleIds) throws Exception {
        String response = mvc.perform(auth(post("/api/users"), adminToken).content(json.writeValueAsString(Map.of(
                        "name", email, "email", email, "password", PASSWORD, "roleIds", roleIds))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asLong();
    }

    static long createEvent(MockMvc mvc, ObjectMapper json, String token, String title, String status) throws Exception {
        String response = mvc.perform(auth(post("/api/events"), token).content(json.writeValueAsString(Map.of(
                        "title", title, "startAt", "2030-05-01T09:00:00", "endAt", "2030-05-01T18:00:00",
                        "status", status))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asLong();
    }

    private static long findIdByName(MockMvc mvc, ObjectMapper json, String token, String url, String name) throws Exception {
        JsonNode list = json.readTree(mvc.perform(auth(get(url), token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        for (JsonNode node : list) {
            if (name.equals(node.get("name").asText())) {
                return node.get("id").asLong();
            }
        }
        throw new AssertionError("Não encontrado: " + name + " em " + url);
    }
}
