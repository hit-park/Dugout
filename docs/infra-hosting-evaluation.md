# 인프라 호스팅 평가 — Dugout

작성일: 2026-06-03
컨텍스트: 1인 개발, 데모 단계, 월 예산 $10~30, 추후 정식 런칭 예정
대상 서비스: Spring Boot 백엔드 + FastAPI AI 서비스 + PostgreSQL/PostGIS + Redis + iOS 푸시(FCM)

---

## 결론

- **데모/베타 (Phase 1)**: Fly.io + Supabase + Cloudflare R2 + Firebase 가 단순함 우선의 정답
- **한국 latency · Firebase 통합 우선**: Cloud Run + Supabase + Firebase 하이브리드
- **AWS ECS Fargate + RDS**: Phase 3 (수만 명, 수익화 이후)로 미룰 것. 데모 단계 예산($10~30)에는 부적합

CLAUDE.md에 적힌 "AWS ECS Fargate + RDS"는 데모 단계 예산으로는 무리. ECS Fargate 최소 인스턴스 + RDS db.t4g.micro Single-AZ + NAT Gateway + ALB만으로 월 $80+ 기본. 백엔드(Spring Boot) + AI(FastAPI) 두 컨테이너라 더 듦.

---

## Phase 1 권장 구성 (지금~데모, 월 $5~15)

### Option A — 단순함 우선 (Fly.io)

- Backend(Spring Boot) + AI(FastAPI): Fly.io 두 앱 (shared-cpu-1x, 256~512MB)
  - Dockerfile 그대로 배포, Tokyo NRT 리전, scale-to-zero
  - JVM 콜드 스타트 5~10초 → min 1 인스턴스 유지 권장 (~$2~5/월)
- DB: Supabase Free (500MB, PostGIS 공식 지원)
- Redis: Upstash Free (10k req/day)
- File Storage: Cloudflare R2 (egress 무료, 10GB free)
- CDN: Cloudflare (무료)
- Push: FCM (무료) — 카카오 알림톡은 PRD F9 기준 영구 제외
- Monitoring: Sentry Free + Better Stack/Axiom Free
- CI: GitHub Actions + Xcode Cloud 25h/월 free
- iOS: Apple Developer $99/년 (인프라 비용 외)

예상 비용: 인프라 $5~10 + Apple $8 = 월 $13~18

### Option B — 한국 latency · Firebase 통합 우선 (Cloud Run)

- Backend(Spring Boot) + AI(FastAPI): Cloud Run 두 서비스 (asia-northeast3 Seoul)
- DB: Supabase Free (Cloud SQL 아님 — 이유는 아래 "GCP 약점" 참고)
- Redis: Upstash Free
- File Storage: Cloudflare R2
- Push/크래시/분석: Firebase (FCM + Crashlytics + Analytics)
- Auth (선택): Firebase Auth (카카오/애플 OAuth 통합)
- 로그: Cloud Logging 50GB/월 무료
- CI: GitHub Actions + Cloud Build, iOS는 Xcode Cloud

예상 비용: $300 크레딧 90일은 거의 $0, 이후 월 $5~15

---

## 호스팅 플랫폼 비교

### Fly.io
강점:
- CLI 한 줄 배포 (`fly deploy`), 학습 부담 낮음
- Tokyo NRT 리전 — 한국 latency ~30ms
- Docker 친화적
- 1인 개발에 가장 부담 적음

약점:
- 미국 카드 결제만 (한국 사업자 등록·세금계산서 불가)
- 부가 서비스 빈약 (Postgres, Redis 정도)
- 한국 latency가 Cloud Run보다 10~20ms 느림

### Cloud Run (GCP)
강점:
- 서울 리전(asia-northeast3) — 한국 latency ~10ms
- always-free 후함 (200만 req/월 + 360,000 GB-s)
- $300 크레딧 / 90일 (신규 가입)
- Firebase 생태계 통합 (FCM/Crashlytics/Auth/Analytics 한 콘솔)
- Cloud Logging 50GB/월 무료 — 로그 인프라 따로 필요 없음
- Pub/Sub, Cloud Tasks, Cloud Scheduler 등 비동기 인프라 풍부
- 한국 사업자 등록·세금계산서 가능

약점:
- IAM·Service Account 학습 부담
- JVM 콜드 스타트가 부각됨 → min-instances=1 회피 시 월 $5~10 고정 발생
- Cloud SQL 비쌈 (db-f1-micro 24/7 = 월 $9~12 + storage)
- Cloud Run ↔ Cloud SQL private IP 연결 시 Serverless VPC Connector idle 비용 ~$10

### AWS ECS Fargate + RDS
- Phase 1 부적합: 최소 구성도 월 $80+
- Phase 3 (수만 명, SLA/VPC/규제 요구) 이후 검토
- ECS Fargate 최소 인스턴스 + RDS Multi-AZ + ElastiCache + NAT + ALB 조합으로 본격 확장 시 고려

---

## DB 선택 — Supabase가 데모 단계 정답인 이유

- PostGIS 기본 지원 (구장 지리 쿼리에 필수)
- Free tier 500MB, 사용 안 해도 비용 0
- Auth, Storage, Realtime 번들 (카카오 OAuth provider 직접 지원)
- 추후 Pro $25로 자연스러운 업그레이드 경로

대안 검토:
- Neon: PostgreSQL만 + branching 강력. PostGIS는 지원하지만 일부 확장 제약
- Planetscale: MySQL 기반 — PostGIS 없음, 탈락
- Cloud SQL: 비싸고 서버리스 아님
- RDS: 비싸고 운영 부담

리전 주의: Supabase Free는 region 제약 있을 수 있음. 개인정보 들어가면 반드시 Tokyo 리전. 데이터 국외 이전 동의 처리 안 하면 개인정보보호법 이슈.

---

## File Storage — R2가 S3보다 우위인 이유

Cloudflare R2:
- **egress 무료** — S3는 GB당 $0.09
- S3 호환 API (Spring Boot AWS SDK 그대로 endpoint URL만 교체)
- 10GB까지 storage free
- 라이프사이클·glacier 같은 cold tier 기능은 약함 (Dugout엔 불필요)

라인업 공유 이미지·프로필 사진 다운로드가 많은 도메인이라 egress 비용 차이가 큼.

---

## 단계별 마이그레이션 경로

### Phase 1 (지금~데모)
1. Spring Boot Dockerfile 작성 → 호스팅 플랫폼에 배포 (`fly launch` 또는 Cloud Run deploy)
2. Supabase 프로젝트 생성 → PostGIS enable → datasource URL 교체
3. R2 bucket 생성, S3 SDK endpoint URL을 R2로
4. Sentry SDK 추가 (Spring Boot/FastAPI/iOS)
5. Xcode Cloud 연결 (TestFlight 자동 배포)

### Phase 2 (정식 런칭, 수백~수천 명, 월 $30~80)
- 호스팅 인스턴스 스펙업 (shared-cpu-2x ~ performance-1x)
- DB: Supabase Pro $25 또는 Cloud SQL로 이전 (서울 리전 latency 병목 시)
- AI: 호출량 증가 시 Modal로 분리 (호출당 과금, GPU 옵션)
- Cloud Tasks/Scheduler로 출석 알림·회비 리마인드 비동기화
- 로그: Better Stack $10 또는 Loki + R2 자체 운영

### Phase 3 (확장, 수만 명)
- GCP 풀스택 또는 AWS 풀스택 결정
- 한국 시장 위주면 GCP가 적합도 높음
- SLA·VPC·CloudTrail·KISA 규제 대응 시점

---

## 보안·법적 가드 (CLAUDE.md 연동)

- Supabase/R2 region은 반드시 APAC (Tokyo 우선)
- JWT secret, FCM key, Supabase service_role key, R2 access key는 플랫폼 secret manager로만 (`fly secrets set` 또는 Cloud Run Secret Manager)
- application.yml에 직접 박지 말 것
- 개인정보(이름·연락처·카카오ID·회비 내역)는 로그·에러·테스트 픽스처에 노출 금지 (마스킹 필수)

---

## 결정 포인트 요약

- Firebase Auth로 로그인 처리 → Cloud Run 강하게 추천
- 자체 Auth 백엔드 운영 → Fly.io의 단순함이 더 매력적
- iOS Crashlytics·Analytics 어차피 도입 → GCP +1
- 한국 사업자 등록·세금계산서 필요 → GCP +1
- 1인 개발 운영 부담 최소화 → Fly.io +1

---

## 부록 — Claude Code CLI 한글 표 중복 렌더링 이슈

작성 배경: 위 평가를 정리하던 중 마크다운 표가 한글과 함께 그려질 때 중복돼 출력되는 현상 발생.

### 원인
- CJK(한글) 문자는 터미널에서 2칸 폭인데 Claude Code CLI는 1칸으로 계산 → 표 정렬·줄바꿈 위치가 어긋남
- 스트리밍 중 wide 표가 재렌더링되면서 이전 행이 stale render로 남음
- 일부 버전(v2.1.109)에서는 이전 콘텐츠가 통째로 다시 그려지는 회귀 발생
- Windows에서 `CLAUDE_CODE_NO_FLICKER=1` 환경변수가 중복 출력 유발

### 해결책 (효과 큰 순)
1. Claude Code 최신 업데이트 (`claude update` 또는 `npm i -g @anthropic-ai/claude-code@latest`)
2. 답변 요청 시 "표 대신 리스트로" 명시 — 가장 확실한 회피책
3. 터미널 폭 120 columns 이상으로 확장
4. `~/.claude/settings.json`에서 `CLAUDE_CODE_NO_FLICKER` 제거 (Windows 한정)
5. 한글 고정폭 폰트 사용 (D2Coding, Sarasa Mono K)
6. 재현되면 `claude --debug` 로그 첨부해서 GitHub Issues에 보고

### 참고 GitHub Issues
- #11274 Markdown tables render incorrectly with CJK characters
- #13438 Tables with CJK characters are misaligned
- #14686 Terminal output truncation and Unicode rendering with Korean
- #23534 CJK full-width characters clipped during text wrapping
- #48318 v2.1.109 duplicates previously rendered content
- #42703 CLAUDE_CODE_NO_FLICKER=1 causes duplicated output (Windows)
