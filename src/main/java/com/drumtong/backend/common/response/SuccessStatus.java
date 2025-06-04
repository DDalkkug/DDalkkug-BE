package com.drumtong.backend.common.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public enum SuccessStatus {

    /**
     * 200
     */
    SEND_REGISTER_SUCCESS(HttpStatus.OK,"회원가입 성공"),
    SEND_HEALTH_SUCCESS(HttpStatus.OK,"서버 상태 OK"),
    SEND_EMAIL_VERIFICATION_CODE_SUCCESS(HttpStatus.OK,"이메일 인증코드 발송 성공"),
    SEND_EMAIL_VERIFICATION_SUCCESS(HttpStatus.OK,"이메일 코드 인증 성공"),
    SEND_LOGIN_SUCCESS(HttpStatus.OK, "로그인 성공"),
    SEND_UPDATE_USER_PASSWORD(HttpStatus.OK, "비밀번호 변경 성공"),
    SEND_PASSWORD_RESET_CODE_SUCCESS(HttpStatus.OK,"비밀번호 초기화 코드 전송 성공"),
    GET_USER_INFO_SUCCESS(HttpStatus.OK, "사용자 정보 조회 성공"),

    /**
     * 201
     */
    CREATE_RECRUIT_ARTICLE_SUCCESS(HttpStatus.CREATED, "공고 등록 성공"),

    ;

    private final HttpStatus httpStatus;
    private final String message;

    public int getStatusCode() {
        return this.httpStatus.value();
    }
}