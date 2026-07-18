package com.llenroctech.customerconnect.repository;

import com.llenroctech.customerconnect.domain.Role;

import java.util.Collection;

public interface RoleRepository<T extends Role> {
    T create(T data);

    Collection<T> list(int page, int pageSize);

    T get(Long id);

    T update(T data);

    Boolean delete(Long id);

    void addRoleToUser(Long userId, String roleName);

    T getRoleByUserId(Long userId);

    T getRoleByUserEmail(String email);

    void updateUserRole(Long userId, String roleName);
}