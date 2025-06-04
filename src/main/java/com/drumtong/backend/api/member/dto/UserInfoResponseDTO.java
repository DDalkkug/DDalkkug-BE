package com.drumtong.backend.api.member.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserInfoResponseDTO {

    private long id;
    private String nickname;
    private String email;
}
