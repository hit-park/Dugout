# ⚾ Dugout

> 사회인 야구 AI 플랫폼 — AI 팀 매니저 + 스마트 매칭

밴드와 카카오톡에 흩어진 팀 운영을 하나로 모으고, AI로 자동화하는 사회인 야구 전용 서비스입니다.

## 해결하는 문제

| 현재 (밴드 + 카톡) | Dugout |
|---------------------|--------|
| 밴드에 투표 올리고, 미응답자에게 카톡 개별 연락 | 자동 출석 알림 + AI 출석 예측 |
| 참가자 보고 메모장에 라인업 수기 작성 | AI가 포지션·타순 자동 추천 |
| 엑셀로 회비 관리, 미납자 개별 독촉 | 인앱 회비 관리 + 자동 미납 알림 |
| 밴드/카페 게시판에서 용병·상대팀 찾기 | AI 기반 실력·거리·시간 매칭 |

## 주요 기능

- 🤖 **AI 출석 예측** — 과거 패턴, 날씨, 거리 학습으로 참가 인원 예측
- 📋 **AI 라인업 추천** — 포지션 적합도 + 공정성 기반 자동 라인업
- ⚾ **스마트 매칭** — ELO 레이팅 기반 팀/용병 매칭
- 💰 **회비 관리** — 자동 집계, 미납 알림
- 📲 **카카오 알림톡** — 앱 미설치자에게도 도달하는 알림

## 기술 스택

| 레이어 | 기술 |
|--------|------|
| iOS | Swift 6 + SwiftUI |
| Android | Kotlin + Jetpack Compose |
| Backend | Kotlin + Spring Boot 3 |
| AI | Python + FastAPI |
| Database | PostgreSQL + PostGIS |
| Cache | Redis |
| Infra | AWS (ECS Fargate + RDS) |

## 프로젝트 구조

```
dugout/
├── docs/              # PRD, TDD, 분석 문서
├── dugout-api/        # Backend (Kotlin + Spring Boot)
├── dugout-ai/         # AI Service (Python + FastAPI)
├── dugout-ios/        # iOS App (Swift 6 + SwiftUI)
├── dugout-android/    # Android App (Phase 2)
└── infra/             # Docker, AWS 설정
```

## 문서

- [PRD (제품 요구사항)](docs/PRD.md)
- [TDD (기술 설계서)](docs/TDD.md)
- [경쟁 앱 분석](docs/competitor-analysis.md)

## 개발 환경 설정

### 사전 요구사항

- macOS (Apple Silicon 권장 — iOS 빌드 필수)
- [mise](https://mise.jdx.dev) — Python / Tuist 도구 버전 관리
- [Docker Desktop](https://docs.docker.com/desktop/install/mac-install/) — postgres + redis
- Xcode 16+ — iOS 빌드
- Java 21 (`./gradlew` 가 toolchain 자동 다운로드)

### 첫 셋업 (1회)

```bash
# 1) 도구 설치 (mise 로 Python 3.12 + tuist 자동 설치)
mise install

# 2) iOS 의존성
cd dugout-ios && tuist install && tuist generate && cd ..

# 3) AI 서비스 의존성
cd dugout-ai && python -m venv .venv && .venv/bin/pip install -r requirements.txt && cd ..
```

### 일일 워크플로우 (3 터미널)

```bash
# T1 — 인프라(postgres + redis) + 백엔드
make stack
make api

# T2 — AI 서비스 (헝가리안 라인업·매칭 스코어 등)
make ai

# T3 — iOS Xcode
make ios
```

`make help` 로 전체 명령 확인. 자주 쓰는 것:

```bash
make seed-check     # 4개 서비스 상태 (api/ai/postgres/redis)
make ios-build      # iOS 빌드 점검 (warnings 0 검증)
make api-test       # 백엔드 컴파일 점검
make ai-test        # AI 서비스 pytest
make clean          # gradle / DerivedData / __pycache__ 일괄 정리
make down           # postgres + redis 중지
```

### 검증

```bash
make seed-check
# Backend  (8080): HTTP 200
# AI       (8001): HTTP 200
# Postgres (5432): UP
# Redis    (6379): UP
```

iOS 빌드는 `make ios-build` 가 warnings 0 으로 통과해야 정상.

### 트러블슈팅

| 증상 | 원인 / 해결 |
|---|---|
| iOS link 단계 에러 (`_TaskModifier2`, `SwiftUICore.tbd`) | DerivedData stale. `make clean` 후 재빌드 |
| dugout-ai 가 `pyexpat` import 단계에서 dylib mismatch 로 실패 | Homebrew `python@3.12` 의 expat ABI mismatch. `mise install` 로 cpython 자체 빌드 사용 (`.mise.toml` 가 자동 활성화) |
| AI 추천 시 백엔드가 `AI_REQUEST_FAILED` 던짐 (422) | `RestClient` 가 글로벌 ObjectMapper 적용 못 받는 회귀. `AiClientConfig` 의 `RestClient.Builder` 빈 의존 확인 |
| `make ai` 실행 시 `.venv/bin/uvicorn` 없다고 함 | "첫 셋업 (1회)" 의 `.venv` + `pip install` 단계 누락 |
| 새 iOS 파일이 `.xcodeproj` 에 미등록 | `cd dugout-ios && tuist generate --no-open` |
| dev DB 가 매번 비어있음 | `application-local.yml` 의 `ddl-auto: create-drop` 의도된 동작. `LocalSeedRunner` 가 부팅 시 시드 INSERT |

## License

MIT License
