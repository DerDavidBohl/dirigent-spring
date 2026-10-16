package org.davidbohl.dirigent.auth;

import org.davidbohl.dirigent.auth.entity.OperatorAccountEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// SEC-012: manual test environment only. Lives in test sources so it is never packaged, and needs the "dev" profile.
@Component
@Profile("dev")
public class DevAdminSeeder implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DevAdminSeeder.class);

    private final OperatorAccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public DevAdminSeeder(OperatorAccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (accountRepository.existsByUsernameIgnoreCase("admin")) {
            return;
        }
        accountRepository.save(new OperatorAccountEntity("admin", passwordEncoder.encode("admin")));
        logger.warn("Manual test environment: created local account admin/admin");
    }
}
