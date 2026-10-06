package com.solvix.backend.web.auth;

import com.solvix.backend.security.UserAdministrationService;
import com.solvix.backend.security.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@Profile("production")
public class UserAdministrationController {
    private final UserAdministrationService userAdministrationService;

    public UserAdministrationController(UserAdministrationService userAdministrationService) {
        this.userAdministrationService = userAdministrationService;
    }

    @GetMapping
    public List<UserAdministrationService.UserAccount> findAll() {
        return userAdministrationService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserAdministrationService.UserAccount provision(
            @Valid @RequestBody ProvisionUserRequest request,
            Authentication authentication
    ) {
        return userAdministrationService.provision(
                request.username(),
                request.auth0Subject(),
                request.role(),
                authentication.getName()
        );
    }

    @PatchMapping("/{username}/enabled")
    public UserAdministrationService.UserAccount setEnabled(
            @PathVariable String username,
            @Valid @RequestBody SetUserEnabledRequest request,
            Authentication authentication
    ) {
        return userAdministrationService.setEnabled(username, request.enabled(), authentication.getName());
    }

    public record ProvisionUserRequest(
            @NotBlank @Size(max = 255) String username,
            @NotBlank @Size(max = 255) String auth0Subject,
            @NotNull UserRole role
    ) {}

    public record SetUserEnabledRequest(@NotNull Boolean enabled) {}
}
