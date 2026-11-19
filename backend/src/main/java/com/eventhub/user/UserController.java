package com.eventhub.user;

import com.eventhub.user.UserDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public List<UserResponse> list() {
        return userService.list();
    }

    /** Lista simples de pessoas ativas (nome e e-mail), usada para montar equipes. */
    @GetMapping("/options")
    @PreAuthorize("hasAnyAuthority('USER_MANAGE','EVENT_CREATE','EVENT_EDIT','TEAM_MANAGE') or @eventAccess.managesAnyTeam()")
    public List<UserOption> options() {
        return userService.activeOptions();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }
}
