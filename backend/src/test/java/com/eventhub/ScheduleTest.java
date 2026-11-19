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

/** Cronograma do evento: cadastro, ordem, validação de datas e permissões. */
@SpringBootTest
@AutoConfigureMockMvc
class ScheduleTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    String slug;
    String admin;
    long eventId;

    @BeforeEach
    void setUp() throws Exception {
        slug = newSlug("agenda");
        admin = register(mvc, json, slug, "admin@org.com");
        // TestSupport.createEvent cria o evento em 01/05/2030, das 09:00 às 18:00.
        eventId = createEvent(mvc, json, admin, "Congresso", "PUBLISHED");
    }

    @Test
    void createsListsInOrderUpdatesAndDeletes() throws Exception {
        String url = "/api/events/" + eventId + "/schedule";
        mvc.perform(auth(post(url), admin).content(item("2030-05-01", "14:00", 60, "Bruno", null)))
                .andExpect(status().isCreated());
        long firstId = json.readTree(mvc.perform(auth(post(url), admin)
                        .content(item("2030-05-01", "09:30", 45, "Ana Souza", "ana@exemplo.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.time").value("09:30"))
                .andReturn().getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(auth(get(url), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].speaker").value("Ana Souza"))
                .andExpect(jsonPath("$[0].title").value("Palestra de Ana Souza"))
                .andExpect(jsonPath("$[0].contact").value("ana@exemplo.com"))
                .andExpect(jsonPath("$[1].speaker").value("Bruno"));

        mvc.perform(auth(put(url + "/" + firstId), admin)
                        .content(item("2030-05-01", "10:00", 90, "Ana Souza", "(45) 99999-0000")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durationMinutes").value(90));

        mvc.perform(auth(delete(url + "/" + firstId), admin)).andExpect(status().isNoContent());
        mvc.perform(auth(get(url), admin)).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void rejectsDateOutsideTheEventAndInvalidFields() throws Exception {
        String url = "/api/events/" + eventId + "/schedule";
        mvc.perform(auth(post(url), admin).content(item("2030-05-02", "09:00", 30, "Ana", null)))
                .andExpect(status().isBadRequest());
        mvc.perform(auth(post(url), admin).content(item("2030-05-01", "09:00", 0, "Ana", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.durationMinutes").exists());
        mvc.perform(auth(post(url), admin).content(item("2030-05-01", "09:00", 30, " ", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.speaker").exists());
        mvc.perform(auth(post(url), admin).content(item(" ", "2030-05-01", "09:00", 30, "Ana", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.title").exists());
    }

    @Test
    void membersCanSeeButNotChangeAndOtherTenantsCannotSee() throws Exception {
        String url = "/api/events/" + eventId + "/schedule";
        mvc.perform(auth(post(url), admin).content(item("2030-05-01", "09:00", 30, "Ana", null)))
                .andExpect(status().isCreated());

        long memberRole = roleId(mvc, json, admin, "Membro");
        createUser(mvc, json, admin, "membro@org.com", List.of(memberRole));
        String member = login(mvc, json, slug, "membro@org.com");

        mvc.perform(auth(get(url), member)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(auth(post(url), member).content(item("2030-05-01", "11:00", 30, "X", null)))
                .andExpect(status().isForbidden());

        String other = register(mvc, json, newSlug("outra"), "admin@outra.com");
        mvc.perform(auth(get(url), other)).andExpect(status().isNotFound());
    }

    private String item(String date, String time, int duration, String speaker, String contact) throws Exception {
        return item("Palestra de " + speaker, date, time, duration, speaker, contact);
    }

    private String item(String title, String date, String time, int duration, String speaker, String contact) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("date", date);
        body.put("time", time);
        body.put("durationMinutes", duration);
        body.put("speaker", speaker);
        body.put("contact", contact);
        return json.writeValueAsString(body);
    }
}
