package org.davidbohl.dirigent.auth;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.davidbohl.dirigent.auth.entity.OperatorAccountEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperatorAccountService {

    private final OperatorAccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public OperatorAccountService(OperatorAccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public OperatorAccountEntity createLocalAccount(String username, String password) {
        String normalizedUsername = normalizeUsername(username);
        validatePassword(password);
        ensureUsernameAvailable(normalizedUsername);
        return accountRepository.save(new OperatorAccountEntity(
            normalizedUsername, passwordEncoder.encode(password)));
    }

    @Transactional
    public void resetLocalPassword(String username, String password) {
        validatePassword(password);
        OperatorAccountEntity account = requireLocalAccount(username);
        account.changePasswordHash(passwordEncoder.encode(password));
        accountRepository.save(account);
    }

    @Transactional
    public void deleteLocalAccount(String username) {
        OperatorAccountEntity account = requireLocalAccount(username);
        if (accountRepository.countByEnabledTrueAndPasswordHashIsNotNull() <= 1) {
            throw new IllegalStateException("Cannot delete the final local administrator account");
        }
        accountRepository.delete(account);
    }

    private String normalizeUsername(String username) {
        if (username == null || username.isBlank() || username.length() > 128) {
            throw new IllegalArgumentException("Username must contain between 1 and 128 characters");
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private void ensureUsernameAvailable(String username) {
        if (accountRepository.existsByUsernameIgnoreCase(username)) {
            throw new IllegalArgumentException("An account with that username already exists");
        }
    }

    private OperatorAccountEntity requireLocalAccount(String username) {
        String normalizedUsername = normalizeUsername(username);
        OperatorAccountEntity account = accountRepository.findByUsernameIgnoreCase(normalizedUsername)
                .orElseThrow(() -> new IllegalArgumentException("Local account not found"));
        if (!account.isEnabled() || account.getPasswordHash() == null) {
            throw new IllegalArgumentException("Account is not an enabled local account");
        }
        return account;
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Local account passwords must contain 12 to 72 UTF-8 bytes");
        }
    }
}