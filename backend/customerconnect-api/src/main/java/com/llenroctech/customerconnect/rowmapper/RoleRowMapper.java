package com.llenroctech.customerconnect.rowmapper;

import com.llenroctech.customerconnect.domain.Role;

import java.sql.ResultSet;
import java.sql.SQLException;

public class RoleRowMapper implements org.springframework.jdbc.core.RowMapper<Role> {
    @Override
    public Role mapRow(ResultSet rs, int rowNum) throws SQLException {
        return Role.builder()
                .id(rs.getLong("id"))
                .name(rs.getString("name"))
                .permissions(rs.getString("permission"))
                .build();
    }
}
