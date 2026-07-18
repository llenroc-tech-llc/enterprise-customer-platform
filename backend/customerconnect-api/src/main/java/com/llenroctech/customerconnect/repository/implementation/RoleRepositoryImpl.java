package com.llenroctech.customerconnect.repository.implementation;

import com.llenroctech.customerconnect.domain.Role;
import com.llenroctech.customerconnect.exception.RoleNotFoundException;
import com.llenroctech.customerconnect.repository.RoleRepository;
import com.llenroctech.customerconnect.rowmapper.RoleRowMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.llenroctech.customerconnect.enumeration.RoleType.ROLE_USER;
import static com.llenroctech.customerconnect.query.RoleQuery.*;

@Repository
@RequiredArgsConstructor
@Slf4j
public class RoleRepositoryImpl implements RoleRepository<Role> {
    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public Role create(Role data) {
        return null;
    }

    @Override
    public Collection<Role> list(int page, int pageSize) {
        return List.of();
    }

    @Override
    public Role get(Long id) {
        return null;
    }

    @Override
    public Role update(Role data) {
        return null;
    }

    @Override
    public Boolean delete(Long id) {
        return null;
    }

    @Override
    public void addRoleToUser(Long userId, String roleName) {
        try {
            Role role = jdbc.queryForObject(
                    SELECT_ROLE_BY_NAME_QUERY,
                    Map.of("name", roleName),
                    new RoleRowMapper()
            );

            jdbc.update(
                    INSERT_ROLE_TO_USER_QUERY,
                    Map.of(
                            "userId", userId,
                            "roleId", role.getId()
                    )
            );
        } catch (EmptyResultDataAccessException exception) {
            throw new RoleNotFoundException(
                    "No role found with name: " + ROLE_USER.name()
            );
        } catch (Exception exception) {
            log.error(
                    "Failed to assign role {} to user {}",
                    roleName,
                    userId,
                    exception
            );

            throw exception;
        }
    }

    @Override
    public Role getRoleByUserId(Long userId) {
        try {
            return jdbc.queryForObject(
                    SELECT_ROLE_BY_USER_ID_QUERY,
                    Map.of("userId", userId),
                    new RoleRowMapper()
            );
        } catch (EmptyResultDataAccessException exception) {
            throw new RoleNotFoundException(
                    "No role found for user ID: " + userId
            );
        } catch (Exception exception) {
            log.error(
                    "Failed to retrieve role for user ID: {}",
                    userId,
                    exception
            );

            throw exception;
        }
    }

    @Override
    public Role getRoleByUserEmail(String email) {
        return null;
    }

    @Override
    public void updateUserRole(Long userId, String roleName) {

    }
}
