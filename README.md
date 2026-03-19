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

> 준비 중

## License

MIT License
