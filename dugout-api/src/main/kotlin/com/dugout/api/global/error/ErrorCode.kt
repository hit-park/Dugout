package com.dugout.api.global.error

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val status: HttpStatus,
    val message: String,
) {
    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력입니다"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다"),

    // Auth
    OAUTH_PROVIDER_ERROR(HttpStatus.BAD_GATEWAY, "OAuth 인증 제공자 오류가 발생했습니다"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다"),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 존재하지 않습니다"),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다"),

    // Team
    TEAM_NOT_FOUND(HttpStatus.NOT_FOUND, "팀을 찾을 수 없습니다"),
    TEAM_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "팀 멤버를 찾을 수 없습니다"),
    ALREADY_TEAM_MEMBER(HttpStatus.CONFLICT, "이미 팀에 소속되어 있습니다"),
    INVALID_INVITE_CODE(HttpStatus.BAD_REQUEST, "유효하지 않은 초대 코드입니다"),
    NOT_TEAM_MEMBER(HttpStatus.FORBIDDEN, "팀 멤버가 아닙니다"),
    TEAM_ROLE_NOT_ALLOWED(HttpStatus.FORBIDDEN, "해당 작업에 대한 권한이 없습니다"),
    CAPTAIN_CANNOT_LEAVE(HttpStatus.BAD_REQUEST, "주장은 역할을 위임한 후 탈퇴할 수 있습니다"),

    // Match
    MATCH_NOT_FOUND(HttpStatus.NOT_FOUND, "경기를 찾을 수 없습니다"),

    // Attendance
    ALREADY_VOTED(HttpStatus.CONFLICT, "이미 투표했습니다"),
    VOTE_DEADLINE_PASSED(HttpStatus.BAD_REQUEST, "투표 마감 시간이 지났습니다"),
}
