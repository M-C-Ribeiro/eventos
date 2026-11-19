package com.eventhub;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.eventhub.TestSupport.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Demandas do evento e atribuição por função ou por pessoa. */
@SpringBootTest
@AutoConfigureMockMvc
class DemandTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    String slug;
    String admin;
    long eventId;
    long anaId;
    long beaId;
    long recepcao;
    long apoio;

    @BeforeEach
    void setUp() throws Exception {
        slug = newSlug("demanda");
        admin = register(mvc, json, slug, "admin@org.com");
        eventId = createEvent(mvc, json, admin, "Congresso", "PUBLISHED");

        long memberRole = roleId(mvc, json, admin, "Membro");
        anaId = createUser(mvc, json, admin, "ana@org.com", List.of(memberRole));
        beaId = createUser(mvc, json, admin, "bea@org.com", List.of(memberRole));
        createUser(mvc, json, admin, "fora@org.com", List.of(memberRole));
        recepcao = functionId(mvc, json, admin, "Recepção");
        apoio = functionId(mvc, json, admin, "Apoio");
        setTeam(List.of(Map.of("userId", anaId, "functionId", recepcao), Map.of("userId", beaId, "functionId", apoio)));
    }

    @Test
    void createsAssignsByFunctionAndUserAndLists() throws Exception {
        String url = "/api/events/" + eventId + "/demands";
        long id = create(url, "Imprimir crachás", "2030-04-25");
        create(url, "Reservar som", "2030-04-10");

        mvc.perform(auth(put(url + "/" + id + "/assignments"), admin).content(json.writeValueAsString(Map.of(
                        "functionIds", List.of(recepcao), "userIds", List.of(beaId)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.functions[0].name").value("Recepção"))
                .andExpect(jsonPath("$.functions[0].members[0]").value("ana@org.com"))
                .andExpect(jsonPath("$.users[0].id").value(beaId));

        // Lista em ordem de entrega
        mvc.perform(auth(get(url), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Reservar som"))
                .andExpect(jsonPath("$[1].users.length()").value(1));

        // Atribuir de novo substitui as atribuições
        mvc.perform(auth(put(url + "/" + id + "/assignments"), admin).content(json.writeValueAsString(Map.of(
                        "functionIds", List.of(), "userIds", List.of(anaId)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.functions.length()").value(0))
                .andExpect(jsonPath("$.users[0].id").value(anaId));

        // Quem sai da equipe perde a atribuição nesse evento
        setTeam(List.of(Map.of("userId", beaId, "functionId", apoio)));
        mvc.perform(auth(get(url), admin)).andExpect(jsonPath("$[1].users.length()").value(0));

        // Editar e excluir
        mvc.perform(auth(put(url + "/" + id), admin).content(demand("Imprimir crachás e pulseiras", "2030-04-26")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dueDate").value("2030-04-26"));
        mvc.perform(auth(delete(url + "/" + id), admin)).andExpect(status().isNoContent());
        mvc.perform(auth(get(url), admin)).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void onlyTeamMembersCanBeAssignedAndRequiredFieldsAreChecked() throws Exception {
        String url = "/api/events/" + eventId + "/demands";
        long id = create(url, "Contratar buffet", "2030-04-15");
        long adminId = json.readTree(mvc.perform(auth(get("/api/auth/me"), admin)).andReturn()
                .getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(auth(put(url + "/" + id + "/assignments"), admin).content(json.writeValueAsString(Map.of(
                        "functionIds", List.of(), "userIds", List.of(adminId)))))
                .andExpect(status().isBadRequest());

        Map<String, Object> blank = new HashMap<>();
        blank.put("title", " ");
        blank.put("dueDate", null);
        mvc.perform(auth(post(url), admin).content(json.writeValueAsString(blank)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.title").exists())
                .andExpect(jsonPath("$.fields.dueDate").exists());
    }

    @Test
    void teamSeesButCannotChangeAndOutsidersCannotSee() throws Exception {
        String url = "/api/events/" + eventId + "/demands";
        create(url, "Montar palco", "2030-04-29");

        String ana = login(mvc, json, slug, "ana@org.com");
        String outsider = login(mvc, json, slug, "fora@org.com");

        mvc.perform(auth(get(url), ana)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(auth(post(url), ana).content(demand("X", "2030-04-01"))).andExpect(status().isForbidden());
        mvc.perform(auth(get(url), outsider)).andExpect(status().isForbidden());
    }

    @Test
    void completionPerAssigneeAndMyDemands() throws Exception {
        String url = "/api/events/" + eventId + "/demands";
        long id = create(url, "Imprimir crachás", "2030-04-25");
        create(url, "Reservar som", "2030-04-10");
        // Ana pela função Recepção, Bea diretamente
        mvc.perform(auth(put(url + "/" + id + "/assignments"), admin).content(json.writeValueAsString(Map.of(
                        "functionIds", List.of(recepcao), "userIds", List.of(beaId)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignees.length()").value(2))
                .andExpect(jsonPath("$.completedCount").value(0));

        String ana = login(mvc, json, slug, "ana@org.com");
        String bea = login(mvc, json, slug, "bea@org.com");

        // "Minhas demandas": Ana vê a demanda como dela (pela função), a outra não
        mvc.perform(auth(get(url), ana))
                .andExpect(jsonPath("$[0].assignedToMe").value(false))
                .andExpect(jsonPath("$[1].assignedToMe").value(true))
                .andExpect(jsonPath("$[1].completedByMe").value(false));

        // A própria pessoa informa a conclusão
        mvc.perform(auth(put(url + "/" + id + "/completion/" + anaId), ana).content("{\"completed\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedCount").value(1))
                .andExpect(jsonPath("$.completedByMe").value(true));

        // ...mas não a de outra pessoa
        mvc.perform(auth(put(url + "/" + id + "/completion/" + beaId), ana).content("{\"completed\":true}"))
                .andExpect(status().isForbidden());

        // Quem edita o evento informa por qualquer responsável, e pode voltar para pendente
        mvc.perform(auth(put(url + "/" + id + "/completion/" + beaId), admin).content("{\"completed\":true}"))
                .andExpect(jsonPath("$.completedCount").value(2));
        mvc.perform(auth(put(url + "/" + id + "/completion/" + anaId), admin).content("{\"completed\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedCount").value(1));
        mvc.perform(auth(get(url), bea)).andExpect(jsonPath("$[1].completedByMe").value(true));

        // Só responsáveis podem ser marcados
        long adminId = json.readTree(mvc.perform(auth(get("/api/auth/me"), admin)).andReturn()
                .getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(auth(put(url + "/" + id + "/completion/" + adminId), admin).content("{\"completed\":true}"))
                .andExpect(status().isBadRequest());

        // Quem deixa de ser responsável perde a conclusão
        mvc.perform(auth(put(url + "/" + id + "/assignments"), admin).content(json.writeValueAsString(Map.of(
                        "functionIds", List.of(recepcao), "userIds", List.of()))))
                .andExpect(jsonPath("$.assignees.length()").value(1))
                .andExpect(jsonPath("$.completedCount").value(0));
    }

    private long create(String url, String title, String due) throws Exception {
        return json.readTree(mvc.perform(auth(post(url), admin).content(demand(title, due)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asLong();
    }

    private String demand(String title, String due) throws Exception {
        return json.writeValueAsString(Map.of("title", title, "description", "Detalhes", "dueDate", due));
    }

    private void setTeam(List<Map<String, Long>> members) throws Exception {
        mvc.perform(auth(put("/api/events/" + eventId + "/team"), admin)
                        .content(json.writeValueAsString(Map.of("members", members))))
                .andExpect(status().isOk());
    }
}
