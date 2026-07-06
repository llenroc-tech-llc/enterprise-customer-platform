package com.llenroctech.customerconnect.dtomapper;

import com.llenroctech.customerconnect.domain.User;
import com.llenroctech.customerconnect.dto.UserDTO;
import org.springframework.beans.BeanUtils;


public final class UserDTOMapper {

    private UserDTOMapper() {
    }

    public static UserDTO fromUser(User user) {
        UserDTO dto = new UserDTO();
        BeanUtils.copyProperties(user, dto);
        return dto;
    }

    public static User toUser(UserDTO dto) {
        User user = new User();
        BeanUtils.copyProperties(dto, user);
        return user;
    }
}