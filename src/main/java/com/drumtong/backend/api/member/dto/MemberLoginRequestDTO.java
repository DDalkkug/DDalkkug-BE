package com.drumtong.backend.api.member.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemberLoginRequestDTO {
    private String email;
    private String password;

    public MemberLoginRequestDTO(String email, String password) {
        this.email = email;
        this.password = password;
    }
}