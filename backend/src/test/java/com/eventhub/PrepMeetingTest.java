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

/** Reuniões preparatórias, lista de presença da equipe e quem pode ver/alterar. */
@SpringBootTest
@AutoConfigureMockMvc
class PrepMeetingTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    String slug;
    String admin;
    long eventId;
    long helperId;
    String helper;
    String outsider;

    @BeforeEach
    void setUp() throws Exception {
        slug = newSlug("reuniao");
        admin = register(mvc, json, slug, "admin@org.com");
        eventId = createEvent(mvc, json, admin, "Congresso", "PUBLISHED");

        long memberRole = roleId(mvc, json, admin, "Membro");
        helperId = createUser(mvc, json, admin, "apoio@org.com", List.of(memberRole));
        createUser(mvc, json, admin, "fora@org.com", List.of(memberRole));
        helper = login(mvc, json, slug, "apoio@org.com");
        outsider = login(mvc, json, slug, "fora@org.com");

        long apoio = functionId(mvc, json, admin, "Apoio");
        mvc.perform(auth(put("/api/events/" + eventId + "/team"), admin).content(json.writeValueAsString(Map.of(
                        "members", List.of(Map.of("userId", helperId, "functionId", apoio))))))
                .andExpect(status().isOk());
    }

    @Test
    void meetingCrudAndAttendanceFlow() throws Exception {
        String url = "/api/events/" + eventId + "/meetings";
        long meetingId = json.readTree(mvc.perform(auth(post(url), admin).content(meeting("Alinhamento", "Sala 2")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.presentCount").value(0))
                .andExpect(jsonPath("$.teamSize").value(1))
                .andReturn().getResponse().getContentAsString()).get("id").asLong();

        String att = url + "/" + meetingId + "/attendance";
        // Ninguém marcado: todos ausentes
        mvc.perform(auth(get(att), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].present").value(false))
                .andExpect(jsonPath("$[0].functionName").value("Apoio"));

        // Ausente com justificativa
        mvc.perform(auth(put(att + "/" + helperId), admin).content(mark(false, "Consulta médica")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.justification").value("Consulta médica"));

        // Justificativa não é aceita para presentes
        mvc.perform(auth(put(att + "/" + helperId), admin).content(mark(true, "x")))
                .andExpect(status().isBadRequest());

        // Marcar presente apaga a justificativa
        mvc.perform(auth(put(att + "/" + helperId), admin).content(mark(true, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.present").value(true))
                .andExpect(jsonPath("$.justification").value(org.hamcrest.Matchers.nullValue()));
        mvc.perform(auth(get(url), admin)).andExpect(jsonPath("$[0].presentCount").value(1));

        // Quem não está na equipe não pode ser marcado
        long adminId = json.readTree(mvc.perform(auth(get("/api/auth/me"), admin)).andReturn()
                .getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(auth(put(att + "/" + adminId), admin).content(mark(true, null))).andExpect(status().isBadRequest());

        // Editar e excluir
        mvc.perform(auth(put(url + "/" + meetingId), admin).content(meeting("Alinhamento final", "Online")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location").value("Online"))
                .andExpect(jsonPath("$.presentCount").value(1));
        mvc.perform(auth(delete(url + "/" + meetingId), admin)).andExpect(status().isNoContent());
        mvc.perform(auth(get(url), admin)).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void onlyTheTeamSeesMeetingsAndOnlyEditorsChangeThem() throws Exception {
        String url = "/api/events/" + eventId + "/meetings";
        long meetingId = json.readTree(mvc.perform(auth(post(url), admin).content(meeting("Briefing", "Sala 1")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();

        // Membro da equipe (função Apoio, sem permissões extras) vê, mas não altera
        mvc.perform(auth(get(url), helper)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(auth(get(url + "/" + meetingId + "/attendance"), helper)).andExpect(status().isOk());
        mvc.perform(auth(post(url), helper).content(meeting("X", "Y"))).andExpect(status().isForbidden());
        mvc.perform(auth(put(url + "/" + meetingId + "/attendance/" + helperId), helper)
                .content(mark(true, null))).andExpect(status().isForbidden());

        // Quem vê o evento publicado, mas não é da equipe, não vê as reuniões
        mvc.perform(auth(get(url), outsider)).andExpect(status().isForbidden());

        // Outra organização nem enxerga o evento
        String other = register(mvc, json, newSlug("outra"), "admin@outra.com");
        mvc.perform(auth(get(url), other)).andExpect(status().isNotFound());
    }

    @Test
    void validatesRequiredFields() throws Exception {
        mvc.perform(auth(post("/api/events/" + eventId + "/meetings"), admin).content(meeting(" ", " ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.title").exists())
                .andExpect(jsonPath("$.fields.location").exists());
    }

    private String meeting(String title, String location) throws Exception {
        return json.writeValueAsString(Map.of(
                "title", title, "date", "2030-04-20", "time", "19:00", "location", location));
    }

    private String mark(boolean present, String justification) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("present", present);
        body.put("justification", justification);
        return json.writeValueAsString(body);
    }
}
