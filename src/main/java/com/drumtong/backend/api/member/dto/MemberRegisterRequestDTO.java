package com.drumtong.backend.api.member.dto;

import lombok.Getter;
import org.hibernate.annotations.Bag;

@Getter
public class MemberRegisterRequestDTO {

    private String email;
    private String password;
    private String nickname;
}
