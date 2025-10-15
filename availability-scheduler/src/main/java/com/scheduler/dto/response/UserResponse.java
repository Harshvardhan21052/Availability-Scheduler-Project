package com.scheduler.dto.response;

import com.scheduler.entity.User;
import lombok.Data;

@Data
public class UserResponse {

    private Long id;
    private String username;

    public static UserResponse from(User user) {
        UserResponse res = new UserResponse();
        res.setId(user.getId());
        res.setUsername(user.getUsername());
        return res;
    }
}
