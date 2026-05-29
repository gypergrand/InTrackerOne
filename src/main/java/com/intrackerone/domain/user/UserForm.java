package com.intrackerone.domain.user;

import lombok.Data;

@Data
public class UserForm {
    private String username;
    private String password;
    private String email;
    private Role role;
    private Boolean enabled;
}
