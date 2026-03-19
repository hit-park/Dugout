# CLAUDE.md — Dugout 프로젝트 컨텍스트

## 프로젝트 개요

**Dugout**은 사회인 야구 AI 플랫폼이다.
"밴드와 카카오톡에 흩어진 팀 운영을 하나로 모으고, AI로 자동화하는 사회인 야구 전용 서비스"

### 핵심 가치
- 밴드 투표 → 스마트 출석 관리 + AI 출석 예측
- 카톡 단톡방 라인업 공유 → AI 라인업 추천 + 자동 알림
- 엑셀 회비 관리 → 인앱 회비 관리 + 자동 리마인드
- 게시판 용병 구인 → AI 기반 팀/용병 매칭

### 타겟 사용자
1. 팀 주장/매니저 — 출석 확인, 라인업, 회비 관리 자동화
2. 일반 선수 — 빠른 출석 응답, 라인업 확인
3. 용병 — 내 조건에 맞는 경기 자동 추천
4. 리그 운영자 — 팀 간 매칭, 일정 조율

---

## 기술 스택

| 레이어 | 기술 | 비고 |
|--------|------|------|
| iOS | Swift 6 + SwiftUI | Strict Concurrency 준수 필수 |
| Android | Kotlin + Jetpack Compose | Phase 2 |
| Backend | Kotlin + Spring Boot 3 | 코루틴 사용 |
| AI Service | Python 3.12+ + FastAPI | ML 모델 + LLM 연동 |
| Database | PostgreSQL 16 + PostGIS | 지리 쿼리 |
| Cache | Redis 7 | 세션, 실시간 상태 |
| Push | FCM + 카카오 알림톡 | 알림톡이 핵심 전환 채널 |
| Infra | AWS (ECS Fargate + RDS) | |
| CI/CD | GitHub Actions | |

---

## 모노레포 구조

```
dugout/
├── CLAUDE.md              # 이 파일
├── README.md
├── docs/
│   ├── PRD.md             # Product Requirements Document
│   ├── TDD.md             # Technical Design Document
│   └── competitor-analysis.md
├── dugout-api/            # Kotlin + Spring Boot 백엔드
│   ├── build.gradle.kts
│   └── src/
├── dugout-ai/             # Python + FastAPI AI 서비스
│   ├── requirements.txt
│   └── app/
├── dugout-ios/            # Swift 6 + SwiftUI iOS 앱
│   └── Dugout.xcodeproj
├── dugout-android/        # Kotlin + Compose (Phase 2)
└── infra/                 # Docker, AWS 설정
    ├── docker-compose.yml
    └── aws/
```

---

## 개발 규칙

### 공통
- 커밋 메시지: Conventional Commits (feat:, fix:, docs:, refactor:, test:)
- 브랜치: main, develop, feature/*, fix/*
- PR 단위로 개발, main 직접 푸시 금지

### Kotlin (Backend)
- 패키지: `com.dugout.api`
- Spring Boot 3.x + Kotlin 코루틴
- JPA + QueryDSL
- 도메인 주도 패키지 구조 (domain/{기능}/entity, service, controller, dto, repository)
- API 버저닝: /api/v1/*
- 에러 응답: ErrorCode enum 기반 통일

### Python (AI Service)
- 패키지: `app/`
- FastAPI + Pydantic v2
- AI 모델: 초기 규칙 기반 → 데이터 축적 후 LightGBM
- API 문서: /docs (Swagger 자동 생성)

### Swift (iOS)
- **Swift 6 Strict Concurrency 필수** — 모든 타입은 Sendable 준수
- SwiftUI + MVVM + Clean Architecture
- async/await 기반 네트워크 레이어
- SwiftData로 오프라인 캐시
- 디자인 시스템: PB 접두어 (PBButton, PBCard 등)

---

## 핵심 도메인 용어

| 한국어 | 영어 (코드) | 설명 |
|--------|-------------|------|
| 팀 | Team | 사회인 야구팀 |
| 경기 | Match | 경기 일정 |
| 출석 | Attendance | 경기 참가/불참 투표 |
| 라인업 | Lineup | 수비 포지션 + 타순 배치 |
| 회비 | Fee | 팀 운영 비용 |
| 매칭 | Matching | 팀 간 연습경기 매칭 |
| 용병 | Mercenary | 고정 팀 없이 경기 참가하는 선수 |
| 구장 | Ground | 야구장 |
| 부수 | Division | 실력 등급 (1부~4부) |
| 레이팅 | Rating | ELO 기반 팀 실력 점수 |

## 포지션 코드

P(투수), C(포수), 1B(1루수), 2B(2루수), 3B(3루수), SS(유격수), LF(좌익수), CF(중견수), RF(우익수), DH(지명타자)

---

## AI 모델 참고

### 출석 예측
- Phase 1: 규칙 기반 (요일 패턴, 날씨, 거리, 응답 속도)
- Phase 2: LightGBM (팀별 모델, 팀당 50경기+ 후 전환)

### 라인업 추천
- 헝가리안 알고리즘으로 포지션 최적 배치
- 공정성 보정 (연속 벤치 방지)
- 팀 설정: BALANCED(균등 출전) / COMPETITIVE(실력 우선)

### 매칭 엔진
- 매칭 스코어 = 실력(40%) + 거리(25%) + 시간(20%) + 매너(15%)
- ELO 레이팅 (K=32, 초기: 1부=1600, 2부=1400, 3부=1200, 4부=1000)

---

## 주요 참고 문서

- `docs/PRD.md` — 제품 요구사항 (기능 상세, 사용자 여정, 수익 모델)
- `docs/TDD.md` — 기술 설계 (DB 스키마, API 설계, AI 알고리즘, 인프라)
- `docs/competitor-analysis.md` — 경쟁 앱 분석 (게임원, 스포비, 사야인)

---

## 응답 규칙

- 한국어로 답변
- iOS 코드는 Swift 6 Strict Concurrency를 반드시 만족
- 코드 생성 시 해당 모듈의 패키지 구조와 네이밍 규칙을 따를 것
- DB 변경 시 docs/TDD.md의 스키마 섹션도 함께 업데이트
- 새 API 추가 시 docs/TDD.md의 API 설계 섹션도 함께 업데이트
