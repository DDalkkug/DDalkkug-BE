package com.drumtong.backend.api.member.service;

import com.drumtong.backend.api.member.dto.*;
import com.drumtong.backend.api.member.entity.EmailVerification;
import com.drumtong.backend.api.member.entity.Member;
import com.drumtong.backend.api.member.entity.PasswordReset;
import com.drumtong.backend.api.member.jwt.service.JwtService;
import com.drumtong.backend.api.member.repository.EmailVerificationRepository;
import com.drumtong.backend.api.member.repository.MemberRepository;
import com.drumtong.backend.api.member.repository.PasswordResetRepository;
import com.drumtong.backend.common.exception.BadRequestException;
import com.drumtong.backend.common.exception.NotFoundException;
import com.drumtong.backend.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.drumtong.backend.common.response.ErrorStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationRepository emailVerificationRepository;
    private final JwtService jwtService;
    private final PasswordResetRepository passwordResetRepository;

    @Transactional
    public void registerMember(MemberRegisterRequestDTO memberRegisterRequestDTO) {

        // 이메일 중복 검증
        if (memberRepository.findByEmail(memberRegisterRequestDTO.getEmail()).isPresent()) {
            throw new BadRequestException(ErrorStatus.ALREADY_REGISTER_EMAIL_EXCEPTION.getMessage());
        }

        // 이메일 인증 여부 체크
        EmailVerification emailVerification = emailVerificationRepository.findByEmail(memberRegisterRequestDTO.getEmail())
                .orElseThrow(() -> new BadRequestException(ErrorStatus.MISSING_EMAIL_VERIFICATION_EXCEPTION.getMessage()));
        if (!emailVerification.isVerified()) {
            throw new BadRequestException(ErrorStatus.MISSING_EMAIL_VERIFICATION_EXCEPTION.getMessage());
        }

        // 닉네임 중복 체크
        if (memberRepository.findByNickname(memberRegisterRequestDTO.getNickname()).isPresent()) {
            throw new BadRequestException(ErrorStatus.ALREADY_REGISTER_NICKNAME_EXCPETION.getMessage());
        }

        // Member 엔티티 생성
        Member member = Member.builder()
                .email(memberRegisterRequestDTO.getEmail())
                .password(passwordEncoder.encode(memberRegisterRequestDTO.getPassword()))
                .nickname(memberRegisterRequestDTO.getNickname())
                .build();

        memberRepository.save(member);
    }

    // 로그인
    @Transactional(readOnly = true)
    public MemberLoginResponseDTO login(MemberLoginRequestDTO memberLoginRequestDTO) {

        // 이메일로 회원 검색
        Member member = memberRepository.findByEmail(memberLoginRequestDTO.getEmail())
                .orElseThrow(() -> new NotFoundException(ErrorStatus.EMAIL_NOT_FOUND_EXCEPTION.getMessage()));

        // 비밀번호 검증
        if (!passwordEncoder.matches(memberLoginRequestDTO.getPassword(), member.getPassword())) {
            throw new BadRequestException(ErrorStatus.WRONG_PASSWORD_EXCEPTION.getMessage());
        }

        // JWT 토큰 생성 (Access)
        String accessToken = jwtService.createAccessToken(member.getId());

        // 응답 데이터 구성
        return new MemberLoginResponseDTO(
                accessToken
        );
    }

    // 비밀번호 초기화
    @Transactional
    public void resetPassword(PasswordResetConfirmDTO passwordResetConfirmDTO) {

        // 인증 코드 체크
        PasswordReset passwordReset = passwordResetRepository.findByCode(passwordResetConfirmDTO.getCode())
                .orElseThrow(() -> new BadRequestException(ErrorStatus.INVALID_PASSWORD_RESET_CODE_EXCEPTION.getMessage()));

        // 인증 코드 만료 체크
        if (LocalDateTime.now().isAfter(passwordReset.getExpirationTime())) {
            throw new UnauthorizedException(ErrorStatus.EXPIRED_PASSWORD_RESET_CODE_EXCEPTION.getMessage());
        }

        Member member = memberRepository.findByEmail(passwordReset.getEmail())
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND_EXCEPTION.getMessage()));

        member.updatePassword(passwordEncoder.encode(passwordResetConfirmDTO.getNewPassword()));
        memberRepository.save(member);

        passwordResetRepository.delete(passwordReset);
    }

    // 사용자 정보 조회
    @Transactional(readOnly = true)
    public UserInfoResponseDTO getUserInfo(Long memberId) {

        // 해당 유저를 찾을 수 없을 경우 예외처리
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND_EXCEPTION.getMessage()));

        return new UserInfoResponseDTO(member.getId(), member.getNickname(), member.getEmail(), member.getTotalPaid());
    }

}
