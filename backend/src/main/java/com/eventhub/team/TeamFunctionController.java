package com.eventhub.team;

import com.eventhub.team.TeamDtos.TeamFunctionRequest;
import com.eventhub.team.TeamDtos.TeamFunctionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/team-functions")
public class TeamFunctionController {

    private final TeamFunctionService service;

    public TeamFunctionController(TeamFunctionService service) {
        this.service = service;
    }

    /** Qualquer usuário autenticado pode ver o catálogo (necessário para exibir e montar equipes). */
    @GetMapping
    public List<TeamFunctionResponse> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('TEAM_FUNCTION_MANAGE')")
    public TeamFunctionResponse create(@Valid @RequestBody TeamFunctionRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('TEAM_FUNCTION_MANAGE')")
    public TeamFunctionResponse update(@PathVariable Long id, @Valid @RequestBody TeamFunctionRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TEAM_FUNCTION_MANAGE')")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
