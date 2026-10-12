package org.davidbohl.dirigent.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.davidbohl.dirigent.auth.entity.OperatorAccountEntity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class OperatorAccountServiceTest {

    private final OperatorAccountRepository repository = Mockito.mock(OperatorAccountRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final OperatorAccountService service = new OperatorAccountService(repository, encoder);

    @Test
    void storesOnlyTheEncodedLocalPassword() {
        when(repository.existsByUsernameIgnoreCase("operator")).thenReturn(false);
        when(repository.save(any(OperatorAccountEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OperatorAccountEntity account = service.createLocalAccount("Operator", "long-enough-password");

        assertEquals("operator", account.getUsername());
        assertNotEquals("long-enough-password", account.getPasswordHash());
        assertTrue(encoder.matches("long-enough-password", account.getPasswordHash()));
    }

    @Test
    void rejectsDuplicateAndOutOfRangePasswords() {
        when(repository.existsByUsernameIgnoreCase("operator")).thenReturn(true);
        assertThrows(IllegalArgumentException.class,
                () -> service.createLocalAccount("operator", "long-enough-password"));

        when(repository.existsByUsernameIgnoreCase("new-user")).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> service.createLocalAccount("new-user", "short"));
        assertThrows(IllegalArgumentException.class,
                () -> service.createLocalAccount("new-user", "x".repeat(73)));
    }

    @Test
    void resetsPasswordForLocalAccount() {
        OperatorAccountEntity account = new OperatorAccountEntity(
                "operator", encoder.encode("old-password-value"));
        when(repository.findByUsernameIgnoreCase("operator")).thenReturn(Optional.of(account));
        when(repository.save(account)).thenReturn(account);

        service.resetLocalPassword("operator", "replacement-password");

        assertTrue(encoder.matches("replacement-password", account.getPasswordHash()));
        assertFalse(encoder.matches("old-password-value", account.getPasswordHash()));
        verify(repository).save(account);
    }

    @Test
    void refusesPasswordResetForOidcOnlyAccount() {
        OperatorAccountEntity account = new OperatorAccountEntity(
                "oidc-admin", null);
        when(repository.findByUsernameIgnoreCase("oidc-admin")).thenReturn(Optional.of(account));

        assertThrows(IllegalArgumentException.class,
                () -> service.resetLocalPassword("oidc-admin", "replacement-password"));
    }

    @Test
    void refusesDeletingFinalLocalAccountAndOidcAccounts() {
        OperatorAccountEntity localAccount = new OperatorAccountEntity(
                "operator", encoder.encode("long-enough-password"));
        when(repository.findByUsernameIgnoreCase("operator")).thenReturn(Optional.of(localAccount));
        when(repository.countByEnabledTrueAndPasswordHashIsNotNull()).thenReturn(1L);

        assertThrows(IllegalStateException.class, () -> service.deleteLocalAccount("operator"));

        OperatorAccountEntity oidcAccount = new OperatorAccountEntity(
                "oidc-admin", null);
        when(repository.findByUsernameIgnoreCase("oidc-admin")).thenReturn(Optional.of(oidcAccount));
        assertThrows(IllegalArgumentException.class, () -> service.deleteLocalAccount("oidc-admin"));
    }

    @Test
    void deletesLocalAccountWhenAnotherLocalAdministratorRemains() {
        OperatorAccountEntity account = new OperatorAccountEntity(
                "operator", encoder.encode("long-enough-password"));
        when(repository.findByUsernameIgnoreCase("operator")).thenReturn(Optional.of(account));
        when(repository.countByEnabledTrueAndPasswordHashIsNotNull()).thenReturn(2L);

        service.deleteLocalAccount("operator");

        verify(repository).delete(account);
    }
}