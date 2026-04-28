# TDD (Technical Design Document)
# 사회인 야구 AI 플랫폼 — "더그아웃(Dugout)"

**문서 버전**: v1.0  
**작성일**: 2026.03.19  
**상태**: Draft  
**관련 문서**: PRD v1.0

---

## 1. 시스템 개요

### 1-1. 아키텍처 원칙

- **모바일 퍼스트**: 사회인 야구인은 PC보다 모바일에서 소통 (밴드/카톡 모두 모바일 중심)
- **오프라인 내성**: 야구장은 전파 상태가 나쁜 경우가 많으므로 핵심 데이터는 로컬 캐시
- **알림 중심 설계**: 밴드/카톡 대체이므로, 알림의 도달률과 적시성이 최우선
- **점진적 AI**: 데이터 없는 초기에는 규칙 기반, 축적 후 ML 모델 전환
- **1인 개발 최적화**: 복잡도를 최소화하고 관리 포인트를 줄이는 방향

### 1-2. 시스템 아키텍처

```
┌──────────────────────────────────────────────────────────────┐
│                         Clients                               │
│  ┌─────────────┐  ┌─────────────┐  ┌──────────────────────┐  │
│  │   iOS App   │  │ Android App │  │  Mobile Web (PWA)    │  │
│  │  Swift 6    │  │   Kotlin    │  │  React (Phase 2)     │  │
│  │  SwiftUI    │  │   Compose   │  │                      │  │
│  └──────┬──────┘  └──────┬──────┘  └──────────┬───────────┘  │
└─────────┼────────────────┼────────────────────┼──────────────┘
          │                │                    │
          ▼                ▼                    ▼
┌──────────────────────────────────────────────────────────────┐
│                    Load Balancer (AWS ALB)                     │
│                    + SSL Termination                           │
└──────────────────────────┬───────────────────────────────────┘
                           │
          ┌────────────────┼─────────────────┐
          ▼                ▼                 ▼
  ┌──────────────┐ ┌──────────────┐ ┌──────────────────┐
  │  Auth API    │ │  Core API    │ │  AI Service      │
  │  (Kotlin)    │ │  (Kotlin)    │ │  (Python)        │
  │  Spring Boot │ │  Spring Boot │ │  FastAPI          │
  │              │ │              │ │                    │
  │  - OAuth     │ │  - Team      │ │  - 출석 예측       │
  │  - JWT       │ │  - Match     │ │  - 라인업 추천     │
  │  - Session   │ │  - Attend    │ │  - 매칭 엔진       │
  │              │ │  - Lineup    │ │  - 레이팅 계산     │
  │              │ │  - Finance   │ │                    │
  │              │ │  - Matching  │ │                    │
  │              │ │  - Ground    │ │                    │
  └──────┬───────┘ └──────┬───────┘ └────────┬───────────┘
         │                │                   │
         ▼                ▼                   ▼
  ┌─────────────────────────────────────────────────────┐
  │                  Data Layer                          │
  │  ┌────────────┐ ┌────────┐ ┌─────────────────────┐  │
  │  │ PostgreSQL │ │ Redis  │ │ S3                  │  │
  │  │ (RDS)      │ │(Cache) │ │ (이미지/파일)        │  │
  │  │            │ │        │ │                     │  │
  │  │ + PostGIS  │ │ - 세션  │ │                     │  │
  │  │            │ │ - 캐시  │ │                     │  │
  │  │            │ │ - 실시간│ │                     │  │
  │  └────────────┘ └────────┘ └─────────────────────┘  │
  └─────────────────────────────────────────────────────┘
         │
  ┌──────┴───────────────────────────────────────┐
  │              External Services                │
  │  ┌───────────┐ ┌──────────┐ ┌─────────────┐  │
  │  │ FCM       │ │ 카카오    │ │ 기상청 API  │  │
  │  │ (Push)    │ │ 알림톡   │ │ (날씨)      │  │
  │  │           │ │ + OAuth  │ │             │  │
  │  └───────────┘ └──────────┘ └─────────────┘  │
  │  ┌───────────┐ ┌──────────┐ ┌─────────────┐  │
  │  │ 네이버    │ │ Google   │ │ Apple Sign  │  │
  │  │ OAuth     │ │ OAuth    │ │ In (JWKS)   │  │
  │  └───────────┘ └──────────┘ └─────────────┘  │
  │  ┌───────────┐                                 │
  │  │ LLM API   │                                 │
  │  │ (Claude)  │                                 │
  │  └───────────┘                                 │
  └───────────────────────────────────────────────┘
```

### 1-3. 기술 스택 상세

| 레이어 | 기술 | 버전 | 선정 이유 |
|--------|------|------|-----------|
| iOS | Swift + SwiftUI | Swift 6 | Strict Concurrency, 네이티브 성능 |
| Android | Kotlin + Jetpack Compose | Kotlin 2.x | 네이티브, 백엔드와 언어 통일 |
| Backend API | Kotlin + Spring Boot | 3.x | 코루틴 지원, 풍부한 생태계 |
| AI Service | Python + FastAPI | 3.12+ | ML 라이브러리 생태계 |
| Database | PostgreSQL + PostGIS | 16 | 지리 쿼리, JSONB, 안정성 |
| Cache | Redis | 7.x | 실시간 출석 상태, 세션 |
| Message Queue | Redis Streams (초기) | — | 별도 MQ 없이 Redis로 통합 |
| Push | Firebase Cloud Messaging | — | iOS/Android 통합 |
| 알림톡 | 카카오 비즈메시지 API | — | 한국 시장 필수 도달 채널 |
| Storage | AWS S3 | — | 프로필 이미지, 라인업 카드 |
| Infra | AWS (ECS Fargate + RDS) | — | 서버리스 컨테이너, 관리 최소화 |
| CI/CD | GitHub Actions | — | 자동 빌드/배포 |
| Monitoring | CloudWatch + Sentry | — | 로그, 에러 추적 |

---

## 2. 백엔드 설계

### 2-1. 모듈 구조

```
dugout-api/
├── build.gradle.kts
├── src/main/kotlin/com/dugout/api/
│   ├── DugoutApplication.kt
│   │
│   ├── global/
│   │   ├── config/           # Spring 설정
│   │   │   ├── SecurityConfig.kt
│   │   │   ├── RedisConfig.kt
│   │   │   ├── S3Config.kt
│   │   │   └── WebConfig.kt
│   │   ├── auth/             # 인증/인가
│   │   │   ├── JwtProvider.kt
│   │   │   ├── JwtFilter.kt
│   │   │   ├── OAuthClient.kt          # 공통 인터페이스
│   │   │   ├── OAuthUserInfo.kt        # 통합 사용자 정보 DTO
│   │   │   ├── OAuthClientFactory.kt   # provider → client 매핑
│   │   │   ├── KakaoOAuthClient.kt
│   │   │   ├── NaverOAuthClient.kt
│   │   │   ├── GoogleOAuthClient.kt
│   │   │   └── AppleOAuthClient.kt     # JWKS 기반 JWT 검증
│   │   ├── error/            # 글로벌 예외 처리
│   │   │   ├── ErrorCode.kt
│   │   │   ├── BusinessException.kt
│   │   │   └── GlobalExceptionHandler.kt
│   │   ├── notification/     # 알림 통합 모듈
│   │   │   ├── NotificationService.kt
│   │   │   ├── FcmClient.kt
│   │   │   ├── KakaoAlimtalkClient.kt
│   │   │   └── NotificationTemplate.kt
│   │   └── common/
│   │       ├── BaseEntity.kt
│   │       └── PageResponse.kt
│   │
│   ├── domain/
│   │   ├── user/
│   │   │   ├── entity/User.kt
│   │   │   ├── repository/UserRepository.kt
│   │   │   ├── service/UserService.kt
│   │   │   ├── dto/UserDto.kt
│   │   │   └── controller/UserController.kt
│   │   │
│   │   ├── team/
│   │   │   ├── entity/
│   │   │   │   ├── Team.kt
│   │   │   │   └── TeamMember.kt
│   │   │   ├── repository/
│   │   │   ├── service/TeamService.kt
│   │   │   ├── dto/
│   │   │   └── controller/TeamController.kt
│   │   │
│   │   ├── match/            # 경기 일정
│   │   │   ├── entity/Match.kt
│   │   │   ├── service/MatchService.kt
│   │   │   └── controller/MatchController.kt
│   │   │
│   │   ├── attendance/       # 출석 관리
│   │   │   ├── entity/Attendance.kt
│   │   │   ├── service/AttendanceService.kt
│   │   │   ├── scheduler/AttendanceReminderScheduler.kt
│   │   │   └── controller/AttendanceController.kt
│   │   │
│   │   ├── lineup/           # 라인업
│   │   │   ├── entity/
│   │   │   │   ├── Lineup.kt
│   │   │   │   └── LineupEntry.kt
│   │   │   ├── service/LineupService.kt
│   │   │   └── controller/LineupController.kt
│   │   │
│   │   ├── finance/          # 회비
│   │   │   ├── entity/
│   │   │   │   ├── Fee.kt
│   │   │   │   └── FeePayment.kt
│   │   │   ├── service/FinanceService.kt
│   │   │   └── controller/FinanceController.kt
│   │   │
│   │   ├── matching/         # 팀/용병 매칭
│   │   │   ├── entity/
│   │   │   │   ├── MatchRequest.kt
│   │   │   │   ├── MercenaryProfile.kt
│   │   │   │   ├── MercenaryRequest.kt
│   │   │   │   └── TeamRating.kt
│   │   │   ├── service/
│   │   │   │   ├── TeamMatchingService.kt
│   │   │   │   └── MercenaryService.kt
│   │   │   └── controller/
│   │   │       ├── TeamMatchingController.kt
│   │   │       └── MercenaryController.kt
│   │   │
│   │   └── ground/           # 구장
│   │       ├── entity/Ground.kt
│   │       ├── service/GroundService.kt
│   │       └── controller/GroundController.kt
│   │
│   └── infra/
│       ├── ai/               # AI Service 연동
│       │   ├── AiServiceClient.kt
│       │   ├── dto/AttendancePredictionRequest.kt
│       │   ├── dto/LineupRecommendationRequest.kt
│       │   └── dto/MatchingScoreRequest.kt
│       ├── weather/          # 기상청 API
│       │   └── WeatherClient.kt
│       └── image/            # 라인업 카드 이미지 생성
│           └── LineupCardGenerator.kt
```

### 2-2. API 설계

#### 인증

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | /api/v1/auth/kakao | 카카오 로그인 (Access Token) |
| POST | /api/v1/auth/naver | 네이버 로그인 (Access Token) |
| POST | /api/v1/auth/google | Google 로그인 (Access Token) |
| POST | /api/v1/auth/apple | Apple 로그인 (Identity Token / JWT) |
| POST | /api/v1/auth/refresh | 토큰 갱신 (Refresh Token Rotation) |
| DELETE | /api/v1/auth/logout | 로그아웃 (Refresh Token 무효화) |

#### 사용자

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | /api/v1/users/me | 현재 토큰의 사용자 정보 조회 |

##### GET /api/v1/users/me

현재 토큰의 사용자 정보 조회. 세션 복원 / 마이페이지 보강 용도.

- 인증: Bearer JWT 필수
- 응답: 200 OK + `UserResponse { id, email, nickname, profile_img_url, provider }`
- 에러: 401 (토큰 만료/무효), 404 (USER_NOT_FOUND — 토큰 sub가 가리키는 user가 DB에 없음)

#### 팀 관리

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | /api/v1/teams | 팀 생성 |
| GET | /api/v1/teams/{teamId} | 팀 정보 조회 |
| PUT | /api/v1/teams/{teamId} | 팀 정보 수정 |
| POST | /api/v1/teams/{teamId}/invite | 초대 링크 생성 |
| POST | /api/v1/teams/{teamId}/join | 팀 가입 |
| GET | /api/v1/teams/{teamId}/members | 멤버 목록 |
| PUT | /api/v1/teams/{teamId}/members/{userId} | 멤버 정보 수정 |
| DELETE | /api/v1/teams/{teamId}/members/{userId} | 멤버 탈퇴/제외 |

#### 경기 일정 & 출석

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | /api/v1/teams/{teamId}/matches | 경기 일정 등록 |
| GET | /api/v1/teams/{teamId}/matches | 경기 목록 (캘린더) |
| GET | /api/v1/matches/{matchId} | 경기 상세 |
| PUT | /api/v1/matches/{matchId} | 경기 수정 |
| POST | /api/v1/matches/{matchId}/attendance | 출석 투표 |
| PUT | /api/v1/matches/{matchId}/attendance | 출석 변경 |
| GET | /api/v1/matches/{matchId}/attendance | 출석 현황 |
| GET | /api/v1/matches/{matchId}/attendance/predict | 🤖 AI 출석 예측 |

#### 라인업

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | /api/v1/matches/{matchId}/lineup/recommend | 🤖 AI 라인업 추천 |
| POST | /api/v1/matches/{matchId}/lineup | 라인업 저장 |
| PUT | /api/v1/matches/{matchId}/lineup | 라인업 수정 |
| POST | /api/v1/matches/{matchId}/lineup/confirm | 라인업 확정 (알림 발송) |
| GET | /api/v1/matches/{matchId}/lineup | 라인업 조회 |
| GET | /api/v1/matches/{matchId}/lineup/card | 라인업 카드 이미지 |

#### 회비

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | /api/v1/teams/{teamId}/fees | 회비 생성 |
| GET | /api/v1/teams/{teamId}/fees | 회비 목록 |
| GET | /api/v1/teams/{teamId}/fees/{feeId}/payments | 납부 현황 |
| POST | /api/v1/fees/{feeId}/payments/{userId} | 납부 처리 |
| GET | /api/v1/teams/{teamId}/finance/summary | 재정 요약 |

#### 매칭

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | /api/v1/matching/requests | 매칭 요청 등록 |
| GET | /api/v1/matching/requests | 매칭 요청 목록 |
| GET | /api/v1/matching/requests/{id}/recommend | 🤖 AI 매칭 추천 |
| POST | /api/v1/matching/requests/{id}/propose/{teamId} | 매칭 제안 |
| POST | /api/v1/matching/requests/{id}/accept | 매칭 수락 |
| POST | /api/v1/matching/requests/{id}/reject | 매칭 거절 |
| POST | /api/v1/matching/results/{id} | 경기 결과 입력 |
| POST | /api/v1/matching/results/{id}/review | 상대팀 평가 |

#### 용병

| Method | Endpoint | 설명 |
|--------|----------|------|
| PUT | /api/v1/mercenary/profile | 용병 프로필 수정 |
| GET | /api/v1/mercenary/profile | 용병 프로필 조회 |
| POST | /api/v1/mercenary/requests | 용병 모집 등록 |
| GET | /api/v1/mercenary/requests | 용병 모집 목록 |
| GET | /api/v1/mercenary/requests/{id}/recommend | 🤖 AI 용병 추천 |
| POST | /api/v1/mercenary/requests/{id}/apply | 용병 지원 |
| POST | /api/v1/mercenary/requests/{id}/accept/{userId} | 용병 수락 |
| POST | /api/v1/mercenary/reviews | 용병 상호 평가 |

#### 구장

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | /api/v1/grounds | 구장 목록 (위치 기반) |
| GET | /api/v1/grounds/{id} | 구장 상세 |
| POST | /api/v1/grounds/{id}/reviews | 구장 리뷰 작성 |

### 2-3. 데이터베이스 스키마

```sql
-- ============================================
-- 사용자
-- ============================================
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) UNIQUE,
    nickname        VARCHAR(50) NOT NULL,
    phone           VARCHAR(20),
    profile_img_url VARCHAR(500),
    provider        VARCHAR(20) NOT NULL,    -- KAKAO, NAVER, GOOGLE, APPLE
    provider_id     VARCHAR(255) NOT NULL,
    fcm_token       VARCHAR(500),
    kakao_alimtalk_agreed BOOLEAN DEFAULT FALSE,  -- 알림톡 수신 동의
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (provider, provider_id)
);

-- 사용자 야구 프로필
CREATE TABLE user_baseball_profiles (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id),
    positions       VARCHAR(20)[] NOT NULL,    -- {SS, 2B, OF}
    batting_side    VARCHAR(5) NOT NULL,        -- L, R, S(스위치)
    throwing_arm    VARCHAR(5) NOT NULL,        -- L, R
    skill_level     INTEGER DEFAULT 4,          -- 1~4부급
    bio             TEXT,
    UNIQUE (user_id)
);

-- ============================================
-- 팀
-- ============================================
CREATE TABLE teams (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    logo_url        VARCHAR(500),
    region          VARCHAR(50) NOT NULL,       -- 서울 강남구
    division        INTEGER NOT NULL DEFAULT 4, -- 1~4부
    home_ground_id  BIGINT REFERENCES grounds(id),
    activity_days   VARCHAR(10)[],              -- {SAT, SUN}
    activity_time   VARCHAR(20),                -- 08:00~12:00
    invite_code     VARCHAR(20) UNIQUE,
    lineup_mode     VARCHAR(20) DEFAULT 'BALANCED', -- BALANCED, COMPETITIVE
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE team_members (
    id              BIGSERIAL PRIMARY KEY,
    team_id         BIGINT NOT NULL REFERENCES teams(id),
    user_id         BIGINT NOT NULL REFERENCES users(id),
    role            VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
                    -- CAPTAIN, MANAGER, ACCOUNTANT, MEMBER
    jersey_number   INTEGER,
    positions       VARCHAR(20)[] NOT NULL,     -- 팀 내 포지션
    is_active       BOOLEAN DEFAULT TRUE,
    joined_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (team_id, user_id)
);

-- ============================================
-- 경기 일정
-- ============================================
CREATE TABLE matches (
    id              BIGSERIAL PRIMARY KEY,
    team_id         BIGINT NOT NULL REFERENCES teams(id),
    opponent_name   VARCHAR(100),               -- 상대팀 이름 (비회원 팀)
    opponent_team_id BIGINT REFERENCES teams(id),-- 상대팀 (회원 팀)
    ground_id       BIGINT REFERENCES grounds(id),
    ground_name     VARCHAR(100),               -- 구장 직접 입력용
    match_date      DATE NOT NULL,
    gather_time     TIME,                       -- 집합 시간
    match_time      TIME NOT NULL,              -- 경기 시간
    vote_deadline   TIMESTAMP,                  -- 출석 투표 마감
    status          VARCHAR(20) DEFAULT 'SCHEDULED',
                    -- SCHEDULED, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED
    result_home     INTEGER,
    result_away     INTEGER,
    memo            TEXT,
    is_recurring    BOOLEAN DEFAULT FALSE,
    recurring_rule  JSONB,                      -- 반복 규칙
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================
-- 출석
-- ============================================
CREATE TABLE attendances (
    id              BIGSERIAL PRIMARY KEY,
    match_id        BIGINT NOT NULL REFERENCES matches(id),
    user_id         BIGINT NOT NULL REFERENCES users(id),
    status          VARCHAR(20) NOT NULL,
                    -- ATTEND, ABSENT, MAYBE, LATE, EARLY_LEAVE
    reason          VARCHAR(200),
    responded_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    -- AI 예측 필드
    ai_predicted    VARCHAR(20),
    ai_confidence   DECIMAL(3,2),               -- 0.00 ~ 1.00
    UNIQUE (match_id, user_id)
);

-- 출석 패턴 (AI 피처용 집계 테이블)
CREATE TABLE attendance_patterns (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id),
    team_id         BIGINT NOT NULL REFERENCES teams(id),
    day_of_week     INTEGER,                    -- 0=월 ~ 6=일
    total_matches   INTEGER DEFAULT 0,
    attended        INTEGER DEFAULT 0,
    avg_response_hours DECIMAL(5,2),            -- 평균 응답 시간
    last_updated    TIMESTAMP DEFAULT NOW(),
    UNIQUE (user_id, team_id, day_of_week)
);

-- ============================================
-- 라인업
-- ============================================
CREATE TABLE lineups (
    id              BIGSERIAL PRIMARY KEY,
    match_id        BIGINT NOT NULL REFERENCES matches(id),
    team_id         BIGINT NOT NULL REFERENCES teams(id),
    is_ai_generated BOOLEAN DEFAULT FALSE,
    is_confirmed    BOOLEAN DEFAULT FALSE,
    confirmed_at    TIMESTAMP,
    created_by      BIGINT REFERENCES users(id),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (match_id, team_id)
);

CREATE TABLE lineup_entries (
    id              BIGSERIAL PRIMARY KEY,
    lineup_id       BIGINT NOT NULL REFERENCES lineups(id),
    user_id         BIGINT NOT NULL REFERENCES users(id),
    position        VARCHAR(10) NOT NULL,       -- P,C,1B,2B,3B,SS,LF,CF,RF,DH
    batting_order   INTEGER,                    -- 1~9, NULL=벤치
    is_bench        BOOLEAN DEFAULT FALSE,
    UNIQUE (lineup_id, user_id)
);

-- ============================================
-- 회비
-- ============================================
CREATE TABLE fees (
    id              BIGSERIAL PRIMARY KEY,
    team_id         BIGINT NOT NULL REFERENCES teams(id),
    title           VARCHAR(100) NOT NULL,
    amount          INTEGER NOT NULL,
    fee_type        VARCHAR(20) NOT NULL,       -- MONTHLY, QUARTERLY, SEASONAL, MATCH
    match_id        BIGINT REFERENCES matches(id),
    due_date        DATE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE fee_payments (
    id              BIGSERIAL PRIMARY KEY,
    fee_id          BIGINT NOT NULL REFERENCES fees(id),
    user_id         BIGINT NOT NULL REFERENCES users(id),
    amount          INTEGER NOT NULL,
    status          VARCHAR(20) DEFAULT 'UNPAID', -- UNPAID, PAID, PARTIAL
    paid_at         TIMESTAMP,
    confirmed_by    BIGINT REFERENCES users(id),
    memo            VARCHAR(200),
    UNIQUE (fee_id, user_id)
);

-- ============================================
-- 매칭
-- ============================================
CREATE TABLE team_ratings (
    id              BIGSERIAL PRIMARY KEY,
    team_id         BIGINT NOT NULL REFERENCES teams(id) UNIQUE,
    elo_rating      DECIMAL(7,2) DEFAULT 1200.00,
    wins            INTEGER DEFAULT 0,
    losses          INTEGER DEFAULT 0,
    draws           INTEGER DEFAULT 0,
    manner_score    DECIMAL(3,2) DEFAULT 3.00,  -- 1.00~5.00
    manner_count    INTEGER DEFAULT 0,
    updated_at      TIMESTAMP DEFAULT NOW()
);

CREATE TABLE match_requests (
    id              BIGSERIAL PRIMARY KEY,
    team_id         BIGINT NOT NULL REFERENCES teams(id),
    preferred_dates DATE[] NOT NULL,
    preferred_time  VARCHAR(20),
    preferred_ground_id BIGINT REFERENCES grounds(id),
    preferred_region VARCHAR(50),
    rating_min      DECIMAL(7,2),
    rating_max      DECIMAL(7,2),
    home_away       VARCHAR(10) DEFAULT 'ANY', -- HOME, AWAY, ANY
    status          VARCHAR(20) DEFAULT 'OPEN',
                    -- OPEN, MATCHED, EXPIRED, CANCELLED
    memo            TEXT,
    expires_at      TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE match_proposals (
    id              BIGSERIAL PRIMARY KEY,
    request_id      BIGINT NOT NULL REFERENCES match_requests(id),
    proposed_team_id BIGINT NOT NULL REFERENCES teams(id),
    proposed_by     BIGINT NOT NULL REFERENCES users(id),
    match_score     DECIMAL(5,2),               -- AI 매칭 점수
    status          VARCHAR(20) DEFAULT 'PENDING',
                    -- PENDING, ACCEPTED, REJECTED
    message         TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE match_reviews (
    id              BIGSERIAL PRIMARY KEY,
    match_request_id BIGINT NOT NULL REFERENCES match_requests(id),
    reviewer_team_id BIGINT NOT NULL REFERENCES teams(id),
    target_team_id  BIGINT NOT NULL REFERENCES teams(id),
    manner_score    INTEGER NOT NULL CHECK (manner_score BETWEEN 1 AND 5),
    tags            VARCHAR(50)[],              -- {시간엄수, 페어플레이, 친절}
    comment         TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (match_request_id, reviewer_team_id)
);

-- ============================================
-- 용병
-- ============================================
CREATE TABLE mercenary_profiles (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) UNIQUE,
    is_active       BOOLEAN DEFAULT TRUE,
    regions         VARCHAR(50)[] NOT NULL,
    available_days  VARCHAR(10)[],
    available_times VARCHAR(20),
    desired_fee     INTEGER,                    -- 희망 참가비 (0=무관)
    rating          DECIMAL(3,2) DEFAULT 3.00,
    rating_count    INTEGER DEFAULT 0,
    total_games     INTEGER DEFAULT 0
);

CREATE TABLE mercenary_requests (
    id              BIGSERIAL PRIMARY KEY,
    team_id         BIGINT NOT NULL REFERENCES teams(id),
    match_id        BIGINT NOT NULL REFERENCES matches(id),
    needed_positions VARCHAR(10)[] NOT NULL,
    needed_count    INTEGER DEFAULT 1,
    skill_min       INTEGER DEFAULT 1,
    skill_max       INTEGER DEFAULT 4,
    fee             INTEGER DEFAULT 0,          -- 참가비
    memo            TEXT,
    status          VARCHAR(20) DEFAULT 'OPEN',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE mercenary_applications (
    id              BIGSERIAL PRIMARY KEY,
    request_id      BIGINT NOT NULL REFERENCES mercenary_requests(id),
    user_id         BIGINT NOT NULL REFERENCES users(id),
    position        VARCHAR(10) NOT NULL,
    message         TEXT,
    status          VARCHAR(20) DEFAULT 'PENDING',
                    -- PENDING, ACCEPTED, REJECTED
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (request_id, user_id)
);

CREATE TABLE mercenary_reviews (
    id              BIGSERIAL PRIMARY KEY,
    application_id  BIGINT NOT NULL REFERENCES mercenary_applications(id),
    reviewer_id     BIGINT NOT NULL REFERENCES users(id),
    target_id       BIGINT NOT NULL REFERENCES users(id),
    reviewer_type   VARCHAR(10) NOT NULL,       -- TEAM, MERCENARY
    skill_score     INTEGER CHECK (skill_score BETWEEN 1 AND 5),
    manner_score    INTEGER CHECK (manner_score BETWEEN 1 AND 5),
    punctuality     INTEGER CHECK (punctuality BETWEEN 1 AND 5),
    comment         TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================
-- 구장
-- ============================================
CREATE TABLE grounds (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    address         VARCHAR(300) NOT NULL,
    location        GEOGRAPHY(POINT, 4326),     -- PostGIS
    phone           VARCHAR(20),
    field_type      VARCHAR(20),                -- ARTIFICIAL, NATURAL, DIRT
    has_lights      BOOLEAN DEFAULT FALSE,
    has_scoreboard  BOOLEAN DEFAULT FALSE,
    has_dugout      BOOLEAN DEFAULT FALSE,
    capacity        VARCHAR(50),
    price_info      TEXT,
    photos          VARCHAR(500)[],
    avg_rating      DECIMAL(3,2) DEFAULT 0,
    review_count    INTEGER DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE ground_reviews (
    id              BIGSERIAL PRIMARY KEY,
    ground_id       BIGINT NOT NULL REFERENCES grounds(id),
    user_id         BIGINT NOT NULL REFERENCES users(id),
    rating          INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    content         TEXT,
    photos          VARCHAR(500)[],
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================
-- 알림 로그
-- ============================================
CREATE TABLE notification_logs (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id),
    type            VARCHAR(50) NOT NULL,
    channel         VARCHAR(20) NOT NULL,       -- PUSH, ALIMTALK, EMAIL
    title           VARCHAR(200),
    body            TEXT,
    data            JSONB,
    status          VARCHAR(20) DEFAULT 'SENT', -- SENT, DELIVERED, FAILED
    sent_at         TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================
-- 인덱스
-- ============================================
CREATE INDEX idx_team_members_team ON team_members(team_id);
CREATE INDEX idx_team_members_user ON team_members(user_id);
CREATE INDEX idx_matches_team_date ON matches(team_id, match_date);
CREATE INDEX idx_attendances_match ON attendances(match_id);
CREATE INDEX idx_attendances_user ON attendances(user_id);
CREATE INDEX idx_match_requests_status ON match_requests(status, created_at);
CREATE INDEX idx_mercenary_requests_status ON mercenary_requests(status);
CREATE INDEX idx_grounds_location ON grounds USING GIST(location);
CREATE INDEX idx_notification_logs_user ON notification_logs(user_id, sent_at);
```

---

## 3. AI 서비스 설계

### 3-1. 서비스 구조

```
dugout-ai/
├── main.py                    # FastAPI 앱
├── requirements.txt
├── app/
│   ├── api/
│   │   ├── attendance.py      # 출석 예측 API
│   │   ├── lineup.py          # 라인업 추천 API
│   │   ├── matching.py        # 매칭 API
│   │   └── rating.py          # 레이팅 API
│   ├── models/
│   │   ├── attendance_predictor.py
│   │   ├── lineup_optimizer.py
│   │   ├── matching_engine.py
│   │   └── elo_rating.py
│   ├── services/
│   │   ├── weather_service.py
│   │   └── llm_service.py
│   └── config.py
```

### 3-2. 출석 예측 모델 상세

#### Phase 1: 규칙 기반 (데이터 부족 시)

```python
def predict_attendance_rule_based(user_id, match) -> dict:
    """
    규칙 기반 출석 예측
    Return: { "prediction": "ATTEND"|"ABSENT", "confidence": 0.0~1.0, "reasons": [] }
    """
    score = 0.5  # 기본 확률
    reasons = []

    # 1. 해당 요일 출석률 (가중치: 0.3)
    day_rate = get_day_attendance_rate(user_id, match.day_of_week)
    if day_rate is not None:
        score += (day_rate - 0.5) * 0.3
        reasons.append(f"{DAY_NAMES[match.day_of_week]} 출석률 {day_rate:.0%}")

    # 2. 최근 5경기 출석률 (가중치: 0.25)
    recent_rate = get_recent_attendance_rate(user_id, last_n=5)
    if recent_rate is not None:
        score += (recent_rate - 0.5) * 0.25
        reasons.append(f"최근 5경기 출석률 {recent_rate:.0%}")

    # 3. 날씨 영향 (가중치: 0.15)
    weather = get_weather_forecast(match.date, match.ground)
    if weather.rain_prob > 0.7:
        score -= 0.15
        reasons.append(f"강수확률 {weather.rain_prob:.0%}")
    elif weather.temp < 0 or weather.temp > 35:
        score -= 0.1
        reasons.append(f"기온 {weather.temp}°C")

    # 4. 구장 거리 (가중치: 0.1)
    distance = get_distance(user_home, match.ground)
    if distance > 30:  # 30km 이상
        score -= 0.1
        reasons.append(f"구장 거리 {distance:.0f}km")

    # 5. 연속 결석 패턴 (가중치: 0.1)
    consecutive_absent = get_consecutive_absent(user_id)
    if consecutive_absent >= 3:
        score -= 0.1
        reasons.append(f"연속 {consecutive_absent}회 불참 중")

    # 6. 응답 속도 패턴 (가중치: 0.1)
    # 빠르게 응답하는 사람 = 참가 의지 높음
    avg_response_hours = get_avg_response_hours(user_id)
    hours_since_created = get_hours_since_match_created(match)
    if hours_since_created > avg_response_hours * 2:
        score -= 0.05
        reasons.append("평소보다 응답이 늦음")

    confidence = min(abs(score - 0.5) * 2, 0.95)
    prediction = "ATTEND" if score >= 0.5 else "ABSENT"

    return {
        "prediction": prediction,
        "confidence": round(confidence, 2),
        "probability": round(max(0, min(1, score)), 2),
        "reasons": reasons
    }
```

#### Phase 2: ML 모델 (데이터 축적 후, 팀당 50경기+)

```python
# Features
features = [
    "day_of_week",                # 요일 (one-hot)
    "hour",                       # 경기 시간
    "day_attendance_rate",        # 해당 요일 출석률
    "recent_5_rate",              # 최근 5경기 출석률
    "recent_10_rate",             # 최근 10경기 출석률
    "overall_rate",               # 전체 출석률
    "rain_probability",           # 강수 확률
    "temperature",                # 기온
    "distance_km",                # 구장 거리
    "consecutive_attend",         # 연속 참석 횟수
    "consecutive_absent",         # 연속 불참 횟수
    "avg_response_hours",         # 평균 응답 시간
    "days_since_last_attend",     # 마지막 참석 이후 일수
    "is_holiday",                 # 공휴일 여부
    "team_avg_attendance",        # 팀 평균 출석률
]

# Model: LightGBM (팀별 모델)
# 주 1회 재학습 (일요일 밤 배치)
# 팀별 데이터 50경기 미만 → 규칙 기반 유지
```

### 3-3. 라인업 추천 알고리즘 상세

```python
def recommend_lineup(attendees, team_settings) -> dict:
    """
    출석 확정 인원 기반 라인업 추천

    Step 1: 수비 포지션 배치 (헝가리안 알고리즘)
    Step 2: 타순 배치
    Step 3: 공정성 보정
    """

    # ---- Step 1: 포지션 배치 ----
    positions = ["P", "C", "1B", "2B", "3B", "SS", "LF", "CF", "RF"]

    # 적합도 매트릭스 생성 (선수 x 포지션)
    # 주 포지션: 1.0, 서브 포지션: 0.7, 기타: 0.2
    cost_matrix = build_position_cost_matrix(attendees, positions)

    # 공정성 보정: 최근 N경기 벤치 비율이 높은 선수에게 가산점
    if team_settings.lineup_mode == "BALANCED":
        for player in attendees:
            bench_ratio = get_recent_bench_ratio(player, last_n=5)
            if bench_ratio > 0.4:
                cost_matrix[player] *= 1.2  # 출전 우선권 부여

    # 헝가리안 알고리즘으로 최적 배치
    from scipy.optimize import linear_sum_assignment
    row_idx, col_idx = linear_sum_assignment(cost_matrix, maximize=True)

    starters = assign_positions(row_idx, col_idx, attendees, positions)
    bench = [p for p in attendees if p not in starters]

    # ---- Step 2: 타순 배치 ----
    batting_order = optimize_batting_order(starters, team_settings)
    """
    타순 로직:
    1번: 출루율(OBP) 최고 or 가장 빠른 선수
    2번: 컨택 능력 좋은 선수
    3번: 최고 타자 (종합)
    4번: 장타력 최고
    5~6번: 중거리 타자
    7~9번: 나머지 (투수 보통 9번)

    Phase 1에서는 실제 기록이 없으므로:
    - 자기 신고 타격 스타일 활용 (장타형/출루형/균형형)
    - 좌우 타석 교차 배치 우선
    Phase 2에서 실제 기록 반영
    """

    # ---- Step 3: DH 처리 ----
    if team_settings.use_dh and len(attendees) >= 10:
        # DH 추가 배치
        pass

    return {
        "lineup": [
            {"user_id": p.id, "position": p.assigned_pos,
             "batting_order": p.order, "is_bench": False}
            for p in starters
        ] + [
            {"user_id": p.id, "position": None,
             "batting_order": None, "is_bench": True}
            for p in bench
        ],
        "is_ai_generated": True,
        "fairness_note": generate_fairness_note(starters, bench)
    }
```

### 3-4. 매칭 엔진 상세

```python
def calculate_match_score(team_a, team_b, request) -> dict:
    """
    두 팀 간 매칭 적합도 점수 계산 (0~100)
    """
    weights = {
        "skill": 0.40,    # 실력 유사도
        "distance": 0.25, # 거리
        "time": 0.20,     # 시간대 호환
        "manner": 0.15    # 매너
    }

    # 1. 실력 유사도 (ELO 차이 기반)
    elo_diff = abs(team_a.elo_rating - team_b.elo_rating)
    # 0~50 차이: 100점, 50~100: 80점, 100~200: 60점, 200+: 선형 감소
    if elo_diff <= 50:
        skill_score = 100
    elif elo_diff <= 100:
        skill_score = 100 - (elo_diff - 50) * 0.4
    elif elo_diff <= 200:
        skill_score = 80 - (elo_diff - 100) * 0.4
    else:
        skill_score = max(0, 40 - (elo_diff - 200) * 0.2)

    # 2. 거리 점수 (두 팀 홈구장 중간 지점 기준)
    distance = calculate_distance(team_a.home_ground, team_b.home_ground)
    # 0~5km: 100점, 5~15km: 80점, 15~30km: 50점, 30km+: 선형 감소
    if distance <= 5:
        distance_score = 100
    elif distance <= 15:
        distance_score = 100 - (distance - 5) * 2
    elif distance <= 30:
        distance_score = 80 - (distance - 15) * 2
    else:
        distance_score = max(0, 50 - (distance - 30))

    # 3. 시간대 호환 점수
    time_overlap = calculate_time_overlap(
        request.preferred_dates, request.preferred_time,
        team_b.activity_days, team_b.activity_time
    )
    time_score = time_overlap * 100  # 0.0 ~ 1.0

    # 4. 매너 점수
    manner_score = team_b.manner_score / 5.0 * 100

    # 가중 합산
    total = (
        weights["skill"] * skill_score +
        weights["distance"] * distance_score +
        weights["time"] * time_score +
        weights["manner"] * manner_score
    )

    return {
        "total_score": round(total, 1),
        "breakdown": {
            "skill": {"score": round(skill_score, 1),
                      "detail": f"ELO 차이 {elo_diff:.0f}"},
            "distance": {"score": round(distance_score, 1),
                         "detail": f"{distance:.1f}km"},
            "time": {"score": round(time_score, 1),
                     "detail": f"시간 호환 {time_overlap:.0%}"},
            "manner": {"score": round(manner_score, 1),
                       "detail": f"매너 {team_b.manner_score:.1f}/5.0"}
        }
    }
```

### 3-5. ELO 레이팅 시스템

```python
K_FACTOR = 32  # 사회인 야구 특성상 변동폭 크게

def update_elo(winner_elo, loser_elo, is_draw=False):
    """
    경기 결과에 따른 ELO 업데이트
    초기 레이팅: 부수 기반 (1부=1600, 2부=1400, 3부=1200, 4부=1000)
    """
    expected_winner = 1 / (1 + 10 ** ((loser_elo - winner_elo) / 400))
    expected_loser = 1 - expected_winner

    if is_draw:
        actual_winner = 0.5
        actual_loser = 0.5
    else:
        actual_winner = 1.0
        actual_loser = 0.0

    new_winner_elo = winner_elo + K_FACTOR * (actual_winner - expected_winner)
    new_loser_elo = loser_elo + K_FACTOR * (actual_loser - expected_loser)

    return round(new_winner_elo, 2), round(new_loser_elo, 2)
```

---

## 4. 알림 시스템 설계

### 4-1. 카카오 알림톡 연동

```kotlin
// 카카오 비즈메시지 알림톡 발송
class KakaoAlimtalkClient(
    private val apiKey: String,
    private val senderKey: String
) {
    // 템플릿 코드 (사전 등록 필요)
    object Templates {
        const val MATCH_CREATED = "TPL_MATCH_01"      // 새 경기 일정
        const val ATTENDANCE_REMIND = "TPL_ATTEND_01"  // 출석 리마인드
        const val LINEUP_CONFIRMED = "TPL_LINEUP_01"   // 라인업 확정
        const val FEE_REQUEST = "TPL_FEE_01"           // 회비 청구
        const val MATCHING_RECEIVED = "TPL_MATCH_02"   // 매칭 요청 수신
    }

    // 알림톡 메시지 예시 (사전 심사 필요)
    /*
    [더그아웃] 출석 투표 안내

    #{팀명} 경기 일정이 등록되었습니다.

    📅 #{날짜} #{시간}
    🏟 #{구장명}
    ⚾ vs #{상대팀}

    출석 투표를 해주세요!
    #{출석투표URL}

    현재 참가 #{참가수}명 / 불참 #{불참수}명 / 미응답 #{미응답수}명
    */
}
```

### 4-2. 알림 발송 플로우

```
경기 일정 등록
  → NotificationService.sendMatchCreated(match)
    → 전체 멤버에게 푸시 + 알림톡 수신 동의자에게 알림톡

경기 48시간 전 (스케줄러)
  → AttendanceReminderScheduler.sendFirstReminder()
    → 미응답자에게 푸시
    → 주장에게 현황 요약 푸시

경기 24시간 전 (스케줄러)
  → AttendanceReminderScheduler.sendSecondReminder()
    → 미응답자에게 푸시 + 알림톡
    → AI 출석 예측 실행 → 주장에게 예측 결과 알림
    → 예상 인원 부족 시 용병 모집 추천 알림

라인업 확정
  → NotificationService.sendLineupConfirmed(lineup)
    → 전체 멤버에게 푸시 + 알림톡
    → 라인업 카드 이미지 생성 → 카카오톡 공유 링크 포함
```

---

## 5. iOS 앱 설계

### 5-1. 아키텍처: MVVM + Clean Architecture

```
DugoutApp/
├── App/
│   ├── DugoutApp.swift
│   └── AppDelegate.swift          # FCM, 딥링크 처리
├── Core/
│   ├── Network/
│   │   ├── APIClient.swift        # URLSession async/await
│   │   ├── APIEndpoint.swift
│   │   ├── APIError.swift
│   │   ├── AuthInterceptor.swift  # JWT 자동 갱신
│   │   └── TokenStore.swift       # Keychain 저장
│   ├── Models/                    # 공유 모델 (Sendable)
│   │   ├── User.swift
│   │   ├── Team.swift
│   │   ├── Match.swift
│   │   ├── Attendance.swift
│   │   ├── Lineup.swift
│   │   └── ...
│   ├── Cache/
│   │   └── LocalStore.swift       # SwiftData (오프라인 캐시)
│   ├── Notification/
│   │   ├── PushHandler.swift
│   │   └── DeepLinkRouter.swift
│   └── DesignSystem/
│       ├── Theme.swift
│       ├── Components/
│       │   ├── DGButton.swift
│       │   ├── DGCard.swift
│       │   ├── DGBadge.swift
│       │   └── DiamondView.swift  # 야구장 다이아몬드 뷰
│       └── Extensions/
│
├── Features/
│   ├── Auth/
│   │   ├── AuthView.swift
│   │   ├── AuthViewModel.swift
│   │   ├── OnboardingView.swift
│   │   └── AuthRepository.swift
│   │
│   ├── Home/
│   │   ├── HomeView.swift         # 메인 대시보드
│   │   ├── HomeViewModel.swift
│   │   ├── NextMatchCard.swift    # 다음 경기 카드
│   │   ├── AIInsightCard.swift    # AI 인사이트
│   │   └── TeamNoticeCard.swift
│   │
│   ├── Schedule/
│   │   ├── ScheduleView.swift     # 캘린더 뷰
│   │   ├── MatchDetailView.swift
│   │   ├── CreateMatchView.swift
│   │   ├── ScheduleViewModel.swift
│   │   └── ScheduleRepository.swift
│   │
│   ├── Attendance/
│   │   ├── AttendanceView.swift   # 출석 투표 & 현황
│   │   ├── AttendancePredictionView.swift  # AI 예측
│   │   ├── AttendanceViewModel.swift
│   │   └── AttendanceRepository.swift
│   │
│   ├── Lineup/
│   │   ├── LineupView.swift       # 라인업 뷰
│   │   ├── LineupEditorView.swift # 드래그&드롭 편집
│   │   ├── DiamondLineupView.swift # 다이아몬드 시각화
│   │   ├── BattingOrderView.swift # 타순 뷰
│   │   ├── LineupViewModel.swift
│   │   └── LineupRepository.swift
│   │
│   ├── Finance/
│   │   ├── FinanceDashboardView.swift
│   │   ├── FeeDetailView.swift
│   │   ├── CreateFeeView.swift
│   │   ├── FinanceViewModel.swift
│   │   └── FinanceRepository.swift
│   │
│   ├── Matching/
│   │   ├── MatchingHomeView.swift
│   │   ├── CreateMatchRequestView.swift
│   │   ├── MatchRecommendView.swift  # AI 추천 리스트
│   │   ├── MatchingViewModel.swift
│   │   └── MatchingRepository.swift
│   │
│   ├── Mercenary/
│   │   ├── MercenaryHomeView.swift
│   │   ├── MercenaryProfileView.swift
│   │   ├── MercenaryRequestView.swift
│   │   ├── MercenaryViewModel.swift
│   │   └── MercenaryRepository.swift
│   │
│   ├── Ground/
│   │   ├── GroundMapView.swift
│   │   ├── GroundDetailView.swift
│   │   ├── GroundViewModel.swift
│   │   └── GroundRepository.swift
│   │
│   ├── Team/
│   │   ├── TeamHomeView.swift
│   │   ├── MemberListView.swift
│   │   ├── TeamSettingsView.swift
│   │   ├── InviteView.swift
│   │   ├── TeamViewModel.swift
│   │   └── TeamRepository.swift
│   │
│   └── Settings/
│       ├── SettingsView.swift
│       ├── NotificationSettingsView.swift
│       └── ProfileEditView.swift
│
└── Resources/
    ├── Assets.xcassets
    └── Localizable.strings
```

### 5-2. 네트워크 레이어 (Swift 6 Concurrency)

```swift
// APIClient.swift — Swift 6 Strict Concurrency 준수
final class APIClient: Sendable {
    static let shared = APIClient()

    private let session: URLSession
    private let baseURL: URL
    private let tokenStore: TokenStore

    func request<T: Decodable & Sendable>(
        _ endpoint: APIEndpoint
    ) async throws -> T {
        var req = URLRequest(url: baseURL.appending(path: endpoint.path))
        req.httpMethod = endpoint.method.rawValue
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")

        // JWT 토큰 삽입
        if let token = await tokenStore.accessToken {
            req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        if let body = endpoint.body {
            req.httpBody = try JSONEncoder().encode(body)
        }

        let (data, response) = try await session.data(for: req)
        let httpResponse = response as! HTTPURLResponse

        // 401 → 토큰 갱신 후 재시도
        if httpResponse.statusCode == 401 {
            try await refreshToken()
            return try await request(endpoint)
        }

        guard (200...299).contains(httpResponse.statusCode) else {
            let error = try JSONDecoder().decode(APIErrorResponse.self, from: data)
            throw APIError.server(error)
        }

        return try JSONDecoder().decode(T.self, from: data)
    }
}
```

### 5-3. 핵심 UI 컴포넌트

```swift
// DiamondView.swift — 야구장 다이아몬드 라인업 뷰
struct DiamondLineupView: View {
    let lineup: [LineupEntry]
    let onPlayerTap: (LineupEntry) -> Void

    var body: some View {
        GeometryReader { geo in
            let w = geo.size.width
            let h = geo.size.height

            ZStack {
                // 다이아몬드 배경
                DiamondShape()
                    .stroke(.green.opacity(0.5), lineWidth: 2)
                    .fill(.green.opacity(0.1))

                // 각 포지션에 선수 배치
                ForEach(lineup.filter { !$0.isBench }) { entry in
                    PlayerBadge(entry: entry)
                        .position(
                            positionCoordinate(
                                entry.position,
                                in: CGSize(width: w, height: h)
                            )
                        )
                        .onTapGesture { onPlayerTap(entry) }
                }
            }
        }
        .aspectRatio(1.2, contentMode: .fit)
    }

    func positionCoordinate(
        _ pos: FieldPosition,
        in size: CGSize
    ) -> CGPoint {
        let cx = size.width / 2
        switch pos {
        case .P:  return CGPoint(x: cx, y: size.height * 0.58)
        case .C:  return CGPoint(x: cx, y: size.height * 0.88)
        case .B1: return CGPoint(x: cx + size.width * 0.28, y: size.height * 0.55)
        case .B2: return CGPoint(x: cx + size.width * 0.15, y: size.height * 0.38)
        case .B3: return CGPoint(x: cx - size.width * 0.28, y: size.height * 0.55)
        case .SS: return CGPoint(x: cx - size.width * 0.15, y: size.height * 0.38)
        case .LF: return CGPoint(x: cx - size.width * 0.35, y: size.height * 0.15)
        case .CF: return CGPoint(x: cx, y: size.height * 0.08)
        case .RF: return CGPoint(x: cx + size.width * 0.35, y: size.height * 0.15)
        default:  return CGPoint(x: cx, y: size.height * 0.5)
        }
    }
}
```

---

## 6. 인프라 & 배포

### 6-1. AWS 인프라 구성

```
┌─ AWS ──────────────────────────────────────────────┐
│                                                     │
│  Route 53 (DNS)                                     │
│      │                                              │
│  ALB (Application Load Balancer)                    │
│      ├── /api/v1/ai/*  → AI Service (ECS Fargate)  │
│      └── /api/v1/*     → Core API (ECS Fargate)    │
│                                                     │
│  ECS Fargate                                        │
│      ├── core-api (Kotlin) × 2 tasks              │
│      ├── ai-service (Python) × 1 task             │
│      └── scheduler (출석 리마인드 배치)              │
│                                                     │
│  RDS PostgreSQL (db.t3.medium)                      │
│      ├── Primary                                    │
│      └── Read Replica (Phase 2)                     │
│                                                     │
│  ElastiCache Redis (cache.t3.micro)                 │
│                                                     │
│  S3 (이미지, 라인업 카드)                            │
│      └── CloudFront CDN                             │
│                                                     │
│  ECR (Docker Registry)                              │
│                                                     │
└─────────────────────────────────────────────────────┘
```

### 6-2. 예상 월 비용 (초기)

| 서비스 | 사양 | 월 비용 |
|--------|------|---------|
| ECS Fargate (Core API) | 0.5 vCPU, 1GB × 2 | ~$30 |
| ECS Fargate (AI) | 1 vCPU, 2GB × 1 | ~$25 |
| RDS PostgreSQL | db.t3.medium | ~$50 |
| ElastiCache Redis | cache.t3.micro | ~$15 |
| ALB | — | ~$20 |
| S3 + CloudFront | — | ~$5 |
| 카카오 알림톡 | 건당 ~8원 | ~$30 (5,000건) |
| **합계** | | **~$175/월 (약 23만원)** |

### 6-3. CI/CD 파이프라인

```yaml
# .github/workflows/deploy.yml
name: Deploy

on:
  push:
    branches: [main]

jobs:
  deploy-api:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Build & Push Docker Image
        run: |
          docker build -t dugout-api ./dugout-api
          docker push $ECR_REGISTRY/dugout-api:$GITHUB_SHA
      - name: Deploy to ECS
        run: |
          aws ecs update-service --cluster dugout \
            --service core-api \
            --force-new-deployment

  deploy-ai:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Build & Push
        run: |
          docker build -t dugout-ai ./dugout-ai
          docker push $ECR_REGISTRY/dugout-ai:$GITHUB_SHA
      - name: Deploy to ECS
        run: |
          aws ecs update-service --cluster dugout \
            --service ai-service \
            --force-new-deployment
```

---

## 7. 보안 설계

### 7-1. 인증 플로우

클라이언트(iOS/Android)가 각 OAuth SDK로 토큰을 받아 백엔드에 전달하면,
백엔드는 제공자별 방식으로 토큰을 검증하고 자체 JWT를 발급한다.

#### Kakao / Naver / Google (Access Token 방식)

```
Client → 각 SDK로 로그인 → Access Token 수신
  → POST /api/v1/auth/{provider} { access_token }
    → 서버: 제공자 사용자 정보 API 호출 (userinfo endpoint)
    → 신규: 회원 생성 / 기존: 조회 후 프로필 동기화
    → JWT Access Token (15분) + Refresh Token (30일) 발급
    → Refresh Token은 Redis에 저장 (key: refresh:{userId})
  ← { access_token, refresh_token, user }
```

#### Apple (Identity Token 방식)

```
Client → ASAuthorization으로 로그인 → Identity Token (JWT) 수신
  → POST /api/v1/auth/apple { access_token: <identityToken> }
    → 서버: Apple JWKS (https://appleid.apple.com/auth/keys) 조회
    → Identity Token의 서명/iss/aud/exp 검증
    → 클레임에서 sub(userId), email 추출
    → 신규: 회원 생성 / 기존: 조회
    → JWT 발급 (Kakao/Naver/Google과 동일)
  ← { access_token, refresh_token, user }
```

#### 토큰 갱신 (Refresh Token Rotation)

```
Client → POST /api/v1/auth/refresh { refresh_token }
  → 서버: JWT 서명/만료 검증 → Redis의 저장된 토큰과 일치 확인
  → 새 Access Token + 새 Refresh Token 발급
  → Redis에 새 Refresh Token 저장 (이전 토큰 무효화)
  ← { access_token, refresh_token, user }
```

#### OAuth 클라이언트 구조 (전략 패턴)

```
AuthService.oauthLogin(provider, request)
  → OAuthClientFactory.getClient(provider)
    → KakaoOAuthClient / NaverOAuthClient / GoogleOAuthClient / AppleOAuthClient
      (각 구현체가 OAuthUserInfo로 정규화하여 반환)
```

### 7-2. 데이터 보호

| 데이터 | 보호 방식 |
|--------|-----------|
| 비밀번호 | OAuth 전용, 비밀번호 미저장 |
| 전화번호 | AES-256 암호화 저장 (Phase 2) |
| JWT (자체 발급) | HS256 서명 (Phase 1) → RS256 전환 예정 |
| JWT (Apple Identity) | RS256 서명, Apple JWKS로 공개키 조회 후 검증 |
| Refresh Token | Redis 저장 (key: `refresh:{userId}`), 사용자당 1개. Phase 2에서 디바이스별 확장 |
| API 통신 | HTTPS TLS 1.3 |
| 민감 설정값 (JWT secret, OAuth client id) | 환경 변수 → AWS Secrets Manager (운영) |

---

## 8. 모니터링 & 운영

### 8-1. 핵심 모니터링 지표

| 지표 | 임계값 | 알림 |
|------|--------|------|
| API 응답시간 (p95) | > 500ms | Slack 알림 |
| API 에러율 | > 1% | Slack 알림 |
| AI 서비스 응답시간 | > 5s | Slack 알림 |
| 알림톡 발송 실패율 | > 5% | Slack 알림 |
| DB 커넥션 | > 80% | Slack 알림 |
| 푸시 발송 실패율 | > 10% | Slack 알림 |

### 8-2. 로깅

- **Application Log**: ECS → CloudWatch Logs
- **Error Tracking**: Sentry (iOS + 백엔드)
- **API 호출 로그**: 요청/응답 메타 (개인정보 제외)
- **알림 발송 로그**: notification_logs 테이블

---

## 9. 개발 일정

| 주차 | Backend | AI Service | iOS App |
|------|---------|------------|---------|
| W1-2 | 프로젝트 셋업, DB, Auth API | 프로젝트 셋업 | 프로젝트 셋업, 디자인 시스템 |
| W3-4 | Team, Member API | — | Auth, 온보딩 UI |
| W5-6 | Match, Attendance API | 규칙 기반 출석 예측 | 팀 홈, 일정 캘린더 |
| W7-8 | Lineup API | 라인업 추천 알고리즘 | 출석 투표, 현황 UI |
| W9-10 | Finance API, 알림 시스템 | — | 라인업 뷰 (다이아몬드 + 편집) |
| W11-12 | 카카오 알림톡 연동 | — | 회비 관리, 알림 설정 |
| W13-14 | Matching API | 매칭 엔진, ELO | 매칭 홈, 요청 등록 |
| W15-16 | Mercenary API | 용병 매칭 | 용병, 구장 |
| W17-18 | Ground API, 전체 통합 테스트 | 통합 테스트 | 통합 테스트, 버그 수정 |
| W19-20 | 성능 최적화, 배포 | 모델 튜닝 | TestFlight 베타 |
| W21-24 | 베타 운영, 피드백 반영 | 피드백 반영 | App Store 출시 |
