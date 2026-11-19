package com.eventhub;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static com.eventhub.TestSupport.*;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Perfis de acesso (RBAC) e permissões concedidas pela função na equipe do evento. */
@SpringBootTest
@AutoConfigureMockMvc
class RbacAndTeamTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    String slug;
    String admin;

    @BeforeEach
    void setUp() throws Exception {
        slug = newSlug("rbac");
        admin = register(mvc, json, slug, "admin@org.com");
    }

    @Test
    void newOrganizationGetsDefaultRolesAndAdminHasAllPermissions() throws Exception {
        mvc.perform(auth(get("/api/auth/me"), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("Administrador"))
                .andExpect(jsonPath("$.permissions").value(hasItem("ROLE_MANAGE")));

        mvc.perform(auth(get("/api/roles"), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
        mvc.perform(auth(get("/api/team-functions"), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void memberCanOnlySeePublishedEventsAndCannotManage() throws Exception {
        long memberRole = roleId(mvc, json, admin, "Membro");
        createUser(mvc, json, admin, "membro@org.com", List.of(memberRole));
        String member = login(mvc, json, slug, "membro@org.com");

        long draft = createEvent(mvc, json, admin, "Rascunho", "DRAFT");
        long published = createEvent(mvc, json, admin, "Publicado", "PUBLISHED");

        mvc.perform(auth(get("/api/events"), member))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(published));
        mvc.perform(auth(get("/api/events/" + draft), member)).andExpect(status().isNotFound());

        mvc.perform(auth(post("/api/events"), member).content(eventJson("Novo"))).andExpect(status().isForbidden());
        mvc.perform(auth(get("/api/users"), member)).andExpect(status().isForbidden());
        mvc.perform(auth(get("/api/events/" + published + "/registrations"), member)).andExpect(status().isForbidden());

        // E não inscreve pessoas: inscrições são feitas por quem gerencia o evento
        mvc.perform(auth(post("/api/events/" + published + "/registrations"), member)
                .content(json.writeValueAsString(Map.of("name", "Ana", "email", "ana@x.com")))).andExpect(status().isForbidden());
    }

    @Test
    void teamFunctionGrantsPermissionsOnlyOnThatEvent() throws Exception {
        long memberRole = roleId(mvc, json, admin, "Membro");
        long receptionist = createUser(mvc, json, admin, "recepcao@org.com", List.of(memberRole));
        String reception = login(mvc, json, slug, "recepcao@org.com");
        long receptionFn = functionId(mvc, json, admin, "Recepção");

        long draft = createEvent(mvc, json, admin, "Evento interno", "DRAFT");
        long other = createEvent(mvc, json, admin, "Outro evento", "PUBLISHED");

        // Coloca a pessoa na equipe do evento em rascunho, como Recepção
        mvc.perform(auth(put("/api/events/" + draft + "/team"), admin).content(json.writeValueAsString(Map.of(
                        "members", List.of(Map.of("userId", receptionist, "functionId", receptionFn))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].functionName").value("Recepção"));

        // Ela passa a ver o rascunho, com as permissões da função
        mvc.perform(auth(get("/api/events/" + draft), reception))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myPermissions").value(hasItem("CHECKIN")))
                .andExpect(jsonPath("$.myPermissions").value(not(hasItem("EVENT_EDIT"))));

        // Admin inscreve alguém; a recepção vê a lista e faz o check-in
        String regBody = json.writeValueAsString(Map.of("name", "Ana", "email", "ana@x.com"));
        long regId = json.readTree(mvc.perform(auth(post("/api/events/" + draft + "/registrations"), admin).content(regBody))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(auth(get("/api/events/" + draft + "/registrations"), reception)).andExpect(status().isOk());
        mvc.perform(auth(post("/api/events/" + draft + "/registrations/" + regId + "/check-in"), reception))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHECKED_IN"));

        // ...mas não edita o evento, não inscreve pessoas e não mexe na equipe
        mvc.perform(auth(put("/api/events/" + draft), reception).content(eventJson("Alterado"))).andExpect(status().isForbidden());
        mvc.perform(auth(post("/api/events/" + draft + "/registrations"), reception)
                .content(json.writeValueAsString(Map.of("name", "Bia", "email", "bia@x.com")))).andExpect(status().isForbidden());
        mvc.perform(auth(put("/api/events/" + draft + "/team"), reception)
                .content(json.writeValueAsString(Map.of("members", List.of())))).andExpect(status().isForbidden());

        // E em outro evento, onde não está na equipe, não tem a permissão
        mvc.perform(auth(get("/api/events/" + other + "/registrations"), reception)).andExpect(status().isForbidden());
    }

    @Test
    void customRoleControlsAccess() throws Exception {
        String roleBody = json.writeValueAsString(Map.of(
                "name", "Gestão de pessoas", "description", "RH", "permissions", List.of("USER_MANAGE")));
        long hrRole = json.readTree(mvc.perform(auth(post("/api/roles"), admin).content(roleBody))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();

        createUser(mvc, json, admin, "rh@org.com", List.of(hrRole));
        String hr = login(mvc, json, slug, "rh@org.com");

        mvc.perform(auth(get("/api/users"), hr)).andExpect(status().isOk());
        mvc.perform(auth(post("/api/events"), hr).content(eventJson("X"))).andExpect(status().isForbidden());
        mvc.perform(auth(post("/api/roles"), hr).content(roleBody)).andExpect(status().isForbidden());

        // Ao ganhar EVENT_CREATE, a mudança vale na próxima requisição (sem novo login)
        mvc.perform(auth(put("/api/roles/" + hrRole), admin).content(json.writeValueAsString(Map.of(
                        "name", "Gestão de pessoas", "permissions", List.of("USER_MANAGE", "EVENT_CREATE")))))
                .andExpect(status().isOk());
        mvc.perform(auth(post("/api/events"), hr).content(eventJson("Agora pode"))).andExpect(status().isCreated());
    }

    @Test
    void safetyRules() throws Exception {
        long adminRole = roleId(mvc, json, admin, "Administrador");
        long memberRole = roleId(mvc, json, admin, "Membro");

        // Perfil de administrador do sistema é protegido
        mvc.perform(auth(delete("/api/roles/" + adminRole), admin)).andExpect(status().isBadRequest());

        // Função de equipe só aceita permissões de evento
        mvc.perform(auth(post("/api/team-functions"), admin).content(json.writeValueAsString(Map.of(
                        "name", "Inválida", "permissions", List.of("USER_MANAGE")))))
                .andExpect(status().isBadRequest());

        // Ninguém remove o próprio acesso de administrador
        long myId = json.readTree(mvc.perform(auth(get("/api/auth/me"), admin)).andReturn()
                .getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(auth(put("/api/users/" + myId), admin).content(json.writeValueAsString(Map.of(
                        "name", "Admin", "roleIds", List.of(memberRole), "active", true))))
                .andExpect(status().isBadRequest());
    }

    private String eventJson(String title) throws Exception {
        return json.writeValueAsString(Map.of(
                "title", title, "startAt", "2030-06-01T09:00:00", "endAt", "2030-06-01T12:00:00", "status", "PUBLISHED"));
    }
}
