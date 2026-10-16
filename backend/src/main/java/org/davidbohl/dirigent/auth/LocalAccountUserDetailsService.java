package org.davidbohl.dirigent.auth;

import org.davidbohl.dirigent.auth.entity.OperatorAccountEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class LocalAccountUserDetailsService implements UserDetailsService {

    private final OperatorAccountRepository accountRepository;

    public LocalAccountUserDetailsService(OperatorAccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        OperatorAccountEntity account = accountRepository.findByUsernameIgnoreCase(username)
            .filter(entity -> entity.isEnabled())
                .filter(entity -> entity.getPasswordHash() != null)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid username or password"));

        return User.withUsername(account.getUsername())
                .password(account.getPasswordHash())
                .roles("ADMIN")
                .build();
    }
}