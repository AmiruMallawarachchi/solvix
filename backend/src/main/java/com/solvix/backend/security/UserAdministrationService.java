package com.solvix.backend.security;

import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Profile("production")
public class UserAdministrationService {
    private final UserRepository userRepository;
    private final UserAdministrationAuditRepository auditRepository;

    public UserAdministrationService(
            UserRepository userRepository,
            UserAdministrationAuditRepository auditRepository
    ) {
        this.userRepository = userRepository;
        this.auditRepository = auditRepository;
    }

    @Transactional(readOnly = true)
    public List<UserAccount> findAll() {
        return userRepository.findAll().stream()
                .map(UserAdministrationService::account)
                .sorted(Comparator.comparing(UserAccount::username))
                .toList();
    }

    @Transactional
    public UserAccount provision(String username, String auth0Subject, UserRole role, String actorUsername) {
        if (userRepository.findByUsername(username).isPresent()
                || userRepository.findByAuth0Subject(auth0Subject).isPresent()) {
            throw conflict("Username or Auth0 subject is already provisioned.");
        }

        UserEntity user;
        try {
            user = userRepository.save(new UserEntity(username, null, role, auth0Subject));
        } catch (DataIntegrityViolationException exception) {
            throw conflict("Username or Auth0 subject is already provisioned.");
        }
        record(actorUsername, username, "ACCOUNT_PROVISIONED");
        return account(user);
    }

    @Transactional
    public UserAccount setEnabled(String username, boolean enabled, String actorUsername) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User account was not found."));
        if (user.isEnabled() == enabled) {
            return account(user);
        }
        if (!enabled && user.getRole() == UserRole.ADMINISTRATOR) {
            long enabledAdministrators = userRepository
                    .findEnabledByRoleForUpdate(UserRole.ADMINISTRATOR)
                    .size();
            if (enabledAdministrators <= 1) {
                throw conflict("The last enabled administrator cannot be disabled.");
            }
        }

        user.setEnabled(enabled);
        UserEntity updated = userRepository.save(user);
        record(actorUsername, username, enabled ? "ACCOUNT_ENABLED" : "ACCOUNT_DISABLED");
        return account(updated);
    }

    private void record(String actorUsername, String targetUsername, String action) {
        auditRepository.save(new UserAdministrationAuditEntity(
                UUID.randomUUID(),
                actorUsername,
                targetUsername,
                action,
                Instant.now()
        ));
    }

    private static UserAccount account(UserEntity user) {
        return new UserAccount(user.getUsername(), user.getRole(), user.isEnabled());
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    public record UserAccount(String username, UserRole role, boolean enabled) {}
}
