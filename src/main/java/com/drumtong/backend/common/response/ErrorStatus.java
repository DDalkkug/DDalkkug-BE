package com.drumtong.backend.common.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)

public enum ErrorStatus {
    /**
     * 400 BAD_REQUEST
     */
    VALIDATION_REQUEST_MISSING_EXCEPTION(HttpStatus.BAD_REQUEST, "요청 값이 입력되지 않았습니다."),
    ALREADY_REGISTER_EMAIL_EXCEPTION(HttpStatus.BAD_REQUEST, "이미 가입된 이메일 입니다."),
    ALREADY_REGISTER_NICKNAME_EXCEPTION(HttpStatus.BAD_REQUEST, "이미 등록된 이메일 입니다."),
    WRONG_EMAIL_VERIFICATION_CODE_EXCEPTION(HttpStatus.BAD_REQUEST,"이메일 인증코드가 올바르지 않습니다."),
    VALIDATION_EMAIL_FORMAT_EXCEPTION(HttpStatus.BAD_REQUEST,"올바른 이메일 형식이 아닙니다."),
    MISSING_EMAIL_VERIFICATION_EXCEPTION(HttpStatus.BAD_REQUEST,"이메일 인증을 진행해주세요."),
    WRONG_PASSWORD_EXCEPTION(HttpStatus.BAD_REQUEST,"아이디 또는 비밀번호가 일치하지 않습니다."),
    INVALID_PASSWORD_RESET_CODE_EXCEPTION(HttpStatus.BAD_REQUEST,"유효하지 않은 비밀번호 초기화 인증코드 입니다."),
    GROUP_ALREADY_EXISTS_EXCEPTION(HttpStatus.BAD_REQUEST,"이미 존재하는 그룹입니다."),
    /**
     * 401 UNAUTHORIZED
     */
    USER_UNAUTHORIZED(HttpStatus.UNAUTHORIZED,"인증되지 않은 사용자입니다."),
    UNAUTHORIZED_EMAIL_VERIFICATION_CODE_EXCEPTION(HttpStatus.UNAUTHORIZED,"이메일 인증코드가 만료되었습니다, 재인증 해주세요."),
    UNAUTHORIZED_SMS_VERIFICATION_CODE_EXCEPTION(HttpStatus.UNAUTHORIZED,"SMS 인증코드가 만료되었습니다, 재인증 해주세요."),
    EXPIRED_PASSWORD_RESET_CODE_EXCEPTION(HttpStatus.UNAUTHORIZED,"비밀번호 초기화 인증코드가 만료되었습니다, 재인증 해주세요."),

    /**
     * 404 NOT_FOUND
     */

    NOT_LOGIN_EXCEPTION(HttpStatus.NOT_FOUND,"로그인이 필요합니다."),
    USER_NOT_FOUND_EXCEPTION(HttpStatus.NOT_FOUND, "해당 사용자를 찾을 수 없습니다."),
    EMAIL_NOT_FOUND_EXCEPTION(HttpStatus.NOT_FOUND, "해당 이메일을 찾을 수 없습니다."),
    GROUP_NOT_FOUND_EXCEPTION(HttpStatus.NOT_FOUND, "해당 그룹을 찾을 수 없습니다."),

    /**
     * 500 SERVER_ERROR
     */
    FAIL_UPLOAD_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR,"파일 업로드 실패하였습니다."),

    ;

    private final HttpStatus httpStatus;
    private final String message;

    public int getStatusCode() {
        return this.httpStatus.value();
    }
}
