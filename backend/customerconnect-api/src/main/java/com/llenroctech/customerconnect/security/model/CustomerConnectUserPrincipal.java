package com.llenroctech.customerconnect.security.model;

import com.llenroctech.customerconnect.domain.User;
import com.llenroctech.customerconnect.dto.UserDTO;
import com.llenroctech.customerconnect.dtomapper.UserDTOMapper;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class CustomerConnectUserPrincipal implements UserDetails {

    private final UserDTO user;
    private final String encodedPassword;
    private final String permissions;

    public CustomerConnectUserPrincipal(User user, String permissions) {
        this.user = UserDTOMapper.fromUser(user);
        this.encodedPassword = user.getPassword();
        this.permissions = permissions;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (permissions == null || permissions.isBlank()) {
            return List.of();
        }

        return Arrays.stream(permissions.split(","))
                .map(String::trim)
                .filter(permission -> !permission.isBlank())
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    public Long getUserId() {
        return user.getId();
    }

    public UserDTO getUserDto() {
        return user;
    }

    @Override
    public String getPassword() {
        return encodedPassword;
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.isNotLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }
}
