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
    INVALID_MATCH_DATE(HttpStatus.BAD_REQUEST, "유효하지 않은 경기 날짜입니다"),
    MATCH_ALREADY_CANCELLED(HttpStatus.BAD_REQUEST, "이미 취소된 경기입니다"),

    // Attendance
    ALREADY_VOTED(HttpStatus.CONFLICT, "이미 투표했습니다"),
    VOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "투표 내역을 찾을 수 없습니다"),
    VOTE_DEADLINE_PASSED(HttpStatus.BAD_REQUEST, "투표 마감 시간이 지났습니다"),
    INVALID_ATTENDANCE_STATUS(HttpStatus.BAD_REQUEST, "유효하지 않은 출석 상태입니다"),

    // Fee
    FEE_NOT_FOUND(HttpStatus.NOT_FOUND, "회비 항목을 찾을 수 없습니다"),
    FEE_PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "회비 납부 내역을 찾을 수 없습니다"),
    INVALID_FEE_AMOUNT(HttpStatus.BAD_REQUEST, "회비 금액이 유효하지 않습니다"),
    INVALID_PAYMENT_STATUS(HttpStatus.BAD_REQUEST, "유효하지 않은 납부 상태입니다"),

    // Ground
    GROUND_NOT_FOUND(HttpStatus.NOT_FOUND, "구장을 찾을 수 없습니다"),
    GROUND_REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "구장 리뷰를 찾을 수 없습니다"),
    DUPLICATE_GROUND_REVIEW(HttpStatus.CONFLICT, "이미 작성한 구장 리뷰가 있습니다"),
    INVALID_GROUND_RATING(HttpStatus.BAD_REQUEST, "구장 평점은 1~5 사이여야 합니다"),
    INVALID_LOCATION_QUERY(HttpStatus.BAD_REQUEST, "위치 검색 인자가 유효하지 않습니다"),

    // Mercenary
    MERCENARY_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "용병 프로필을 찾을 수 없습니다"),
    MERCENARY_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "용병 모집을 찾을 수 없습니다"),
    MERCENARY_APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "용병 지원 내역을 찾을 수 없습니다"),
    MERCENARY_REQUEST_NOT_OPEN(HttpStatus.BAD_REQUEST, "마감되었거나 취소된 용병 모집입니다"),
    ALREADY_APPLIED_MERCENARY(HttpStatus.CONFLICT, "이미 지원한 용병 모집입니다"),
    INACTIVE_MERCENARY_PROFILE(HttpStatus.BAD_REQUEST, "비활성화된 용병 프로필로는 지원할 수 없습니다"),

    // Lineup
    LINEUP_NOT_FOUND(HttpStatus.NOT_FOUND, "라인업을 찾을 수 없습니다"),
    LINEUP_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 라인업이 등록된 경기입니다"),
    LINEUP_ALREADY_CONFIRMED(HttpStatus.BAD_REQUEST, "이미 확정된 라인업은 수정할 수 없습니다"),
    INSUFFICIENT_ATTENDEES(HttpStatus.BAD_REQUEST, "라인업을 구성할 출석자가 부족합니다"),
    INVALID_LINEUP_POSITION(HttpStatus.BAD_REQUEST, "유효하지 않은 포지션 코드입니다"),

    // Matching
    MATCHING_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "매칭 요청을 찾을 수 없습니다"),
    MATCHING_PROPOSAL_NOT_FOUND(HttpStatus.NOT_FOUND, "매칭 제안을 찾을 수 없습니다"),
    MATCHING_REQUEST_NOT_OPEN(HttpStatus.BAD_REQUEST, "마감되었거나 취소된 매칭 요청입니다"),
    MATCHING_PROPOSAL_NOT_PENDING(HttpStatus.BAD_REQUEST, "이미 수락 또는 거절된 제안입니다"),
    TEAM_RATING_NOT_FOUND(HttpStatus.NOT_FOUND, "팀 레이팅을 찾을 수 없습니다"),
    DUPLICATE_MATCHING_PROPOSAL(HttpStatus.CONFLICT, "이미 제안된 팀입니다"),
    INVALID_MATCH_RESULT(HttpStatus.BAD_REQUEST, "유효하지 않은 경기 결과입니다"),
    SELF_MATCHING_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "자기 팀에 매칭 제안할 수 없습니다"),
}
