package com.llenroctech.customerconnect.security.service;

import com.llenroctech.customerconnect.domain.Role;
import com.llenroctech.customerconnect.domain.User;
import com.llenroctech.customerconnect.repository.RoleRepository;
import com.llenroctech.customerconnect.repository.UserRepository;
import com.llenroctech.customerconnect.exception.RoleNotFoundException;
import com.llenroctech.customerconnect.security.model.CustomerConnectUserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerConnectUserDetailsService implements UserDetailsService {

    private final UserRepository<User> userRepository;
    private final RoleRepository<Role> roleRepository;

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        User user = userRepository.getUserByEmail(email);

        if (user == null) {
            log.warn("Authentication user lookup failed");
            throw new UsernameNotFoundException("User not found");
        }

        Role role;
        try {
            role = roleRepository.getRoleByUserId(user.getId());
        } catch (RoleNotFoundException exception) {
            log.error("Role not found for user ID: {}", user.getId());
            throw new UsernameNotFoundException("User role not found", exception);
        }

        if (role == null) {
            log.error("Role not found for user ID: {}", user.getId());
            throw new UsernameNotFoundException("User role not found");
        }

        log.debug("Authentication user loaded for user ID {}", user.getId());

        return new CustomerConnectUserPrincipal(
                user,
                role.getPermissions()
        );
    }
}
