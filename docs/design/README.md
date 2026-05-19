# Handoff: Dugout — 사회인 야구 AI 플랫폼

## Overview

**Dugout**은 사회인 야구팀의 운영을 자동화하는 모바일 앱입니다. 네이버 밴드·카카오톡에 흩어진 출석 투표·라인업·회비 관리를 하나의 앱으로 통합하고, AI로 자동화합니다. 타겟은 팀 주장/매니저(출석·라인업 자동화), 일반 선수(빠른 응답), 용병(매칭 추천) 세 페르소나입니다.

---

## About the Design Files

이 번들의 HTML 파일은 **디자인 참조용 HTML 프로토타입**입니다. 실제 프로덕션에 그대로 쓰는 코드가 아니라, 의도된 UI 모양·인터랙션·복사본을 보여주는 시안입니다.

**개발자 과제:** 이 HTML 디자인을 그대로 출력하는 게 아니라, 팀의 기존 코드베이스(React Native / SwiftUI / Flutter 등)에서 동일한 UI를 재현하는 것입니다. 기존 환경이 없다면 **React Native (Expo)** 를 권장합니다.

### 파일 구조 (flows/ 폴더)

| 파일 | 내용 |
|------|------|
| `index.html` | 6개 플로우 전체 인덱스 |
| `auth.html` | ① 인증 플로우 (5 screens) |
| `home.html` | ② 홈 플로우 (2 screens) |
| `team.html` | ③ 팀 플로우 (5 screens) |
| `match.html` | ④ 경기 플로우 (5 screens) |
| `my.html` | ⑤ 마이·알림 플로우 (2 screens) |
| `states.html` | ⑥ 전역 상태 표준 (Empty·Loading·Error·Skeleton) |
| `dugout-screens.jsx` | 모든 화면 컴포넌트 소스 |
| `storyboard-kit.jsx` | 스토리보드 레이아웃 유틸 |

각 HTML 파일을 브라우저에서 열면 **320×692px 폰 프레임** + **어노테이션 카드** (역할·액션·전환·카피·API·엣지케이스)를 함께 볼 수 있습니다.

---

## Fidelity

**High-fidelity.** 최종 색상·타이포그래피·간격·인터랙션 상태가 모두 반영된 픽셀 수준 시안입니다. 개발자는 이 디자인을 기존 코드베이스의 라이브러리·패턴을 써서 **픽셀 수준으로** 재현해야 합니다.

---

## Design Tokens (Single Source of Truth)

> 모든 토큰의 단일 소스는 `Dugout Design Spec.html`입니다.

### 색상 팔레트

#### Primary — Green
| 토큰 | Hex | 용도 |
|------|-----|------|
| `p50` | `#E8F5EE` | AI 카드 배경, 브랜드 서피스 |
| `p100` | `#C8E6D2` | 포지션 칩 배경, 선택 서피스 |
| `p200` | `#9FD3B0` | 아웃라인 강조 |
| `p300` | `#73BE8B` | 보조 그린 |
| `p500` | `#2D8A4E` | **Primary** — 버튼, 탭 active, 배지 |
| `p600` | `#236F40` | 호버·포커스 상태 |
| `p700` | `#1A5532` | 눌림 상태 |
| `p900` | `#0B2917` | 다크 텍스트 |

#### Secondary — Amber/Orange
| 토큰 | Hex | 용도 |
|------|-----|------|
| `s50` | `#FDF1E8` | |
| `s100` | `#FADBC4` | 주장 역할 배지 배경 |
| `s300` | `#F0B083` | |
| `s500` | `#D27640` | Secondary accent |
| `s700` | `#974D24` | Secondary 텍스트 |

#### Neutral — Cream
| 토큰 | Hex | 용도 |
|------|-----|------|
| `c0` | `#FFFFFF` | 카드 배경 |
| `c50` | `#FAF7F4` | 화면 배경 (스크린 내부) |
| `c100` | `#F1ECE5` | 앱 배경, divider |
| `c200` | `#E0DAD0` | 테두리, input border |
| `c300` | `#C2BBAF` | placeholder |
| `c400` | `#9A9389` | 아이콘 비활성 |
| `c500` | `#73706A` | 보조 텍스트 |
| `c700` | `#46443F` | 일반 텍스트 |
| `c900` | `#1F1E1B` | 메인 텍스트 |

#### 시맨틱
| 토큰 | Hex | 용도 |
|------|-----|------|
| `success` | `#16A34A` | 참가 응답, 성공 상태 |
| `warning` | `#D97706` | AI 예측, 주의 |
| `danger` | `#DC2626` | 불참, 오류, 삭제 |
| `info` | `#2563EB` | 알림 배지 |

---

### 타이포그래피

**폰트:** `Pretendard` (본문·레이블·버튼 전체). CDN: `https://cdn.jsdelivr.net/npm/pretendard@1.3.9/dist/web/static/pretendard.min.css`

| 용도 | size | weight | letter-spacing |
|------|------|--------|----------------|
| 화면 제목 (대형) | 26px | 700 | -0.5px |
| 섹션 제목 | 18px | 700 | -0.3px |
| 카드 제목 | 16px | 700 | -0.2px |
| 본문 | 14px | 400 | 0 |
| 보조 텍스트 | 13px | 400 | 0 |
| 레이블/배지 | 11–12px | 600 | 0 |
| 모노 (코드·상태) | `JetBrains Mono` 10–13px | 400–700 | varies |

---

### 간격 & 형태

- **기본 단위:** 4px
- **화면 수평 패딩:** 16px
- **카드 내부 패딩:** 12–16px
- **카드 radius:** 14px
- **버튼 radius:** 10px (대형 12px)
- **칩/배지 radius:** 9999px (pill)
- **기기 프레임:** 320×692px, radius 36px

---

### 인터랙션 모션

| 트리거 | 효과 |
|--------|------|
| 버튼 탭 | `scale(0.97)` + 120ms `cubic-bezier(0.34,1.56,0.64,1)` |
| 포지션 카드 탭 | `scale(0.95)` + spring 180ms 오버슛 |
| 카드 호버 | `translateY(-3px)` + shadow 증가 |
| 팀 칩 탭 | 데이터 refetch + skeleton 280ms |
| 화면 전환 | slide-left 420ms (forward) / slide-right (back) |
| Splash → Login | fade-out 280ms after 2.4s |

---

## Screens / Views — 상세 스펙

### ① AUTH 플로우 (5 screens)

---

#### AUTH-1 · Splash

**역할:** 브랜드 노출 + 자동 라우팅  
**레이아웃:** 전체 화면 `p500(#2D8A4E)` 배경. 중앙 수직 정렬. ⚾ 아이콘 72px → "Dugout" 30px/700/white → "야구팀 운영의 모든 것" 13px/white 78% opacity  
**전환:** 2.4초 후 fade-out 280ms → AUTH-2. 기존 토큰 있으면 HOME-2 직행. 딥링크 진입 시 splash 스킵  
**엣지:** 딥링크 진입 → 로그인 후 원래 URL로 복귀

---

#### AUTH-2 · Login

**역할:** OAuth 4종 + 비로그인 둘러보기  
**레이아웃:**
- 상단 로고 영역: ⚾ + "Dugout" 타이틀
- 소셜 버튼 4개 (세로 스택, gap 10px): 카카오·네이버·Apple·Google
- 하단 ghost CTA: "둘러보기"

**소셜 버튼 스펙 (height 52px, radius 12px, font 15px/700):**

| 버튼 | bg | color |
|------|-----|-------|
| 카카오로 시작하기 | `#FEE500` | `#3C1E1E` |
| 네이버로 시작하기 | `#03C75A` | `#FFFFFF` |
| Apple로 시작하기 | `#000000` | `#FFFFFF` |
| Google로 시작하기 | `#FFFFFF` | `#1F1E1B` (border `c200`) |

**버튼 상태:** default → hover (brightness 0.96) → pressed (scale 0.97) → loading (spinner 교체) → disabled (opacity 0.5)  
**전환:** 버튼 tap 120ms → spinner overlay → OAuth 콜백(1~3s) → 신규: AUTH-3 / 기존: HOME-2  
**API:** `POST /auth/oauth/{provider}` → `{ accessToken, isNew }`  
**엣지:** 카카오톡 미설치 시 웹뷰 fallback / 3회 실패 시 "문의하기" 노출

---

#### AUTH-3 · 온보딩 1/3 — 닉네임

**역할:** 유저 식별자 확보  
**레이아웃:**
- 상단 progress bar (1/3): `p500` 33% fill, 높이 4px, radius 9999px
- 헤드라인: "팀원들에게 보일 이름을 알려주세요" 20px/700
- 닉네임 input (필수): label "닉네임", placeholder "닉네임을 입력하세요", 600ms debounce 후 서버 검증
- 등번호 input (선택): label "등번호 (선택)", placeholder "등번호"
- 하단 고정 Primary 버튼: "다음"

**Input 상태:**

| 상태 | border | 힌트 |
|------|--------|------|
| empty | `c200` | — |
| focus | `p500` 1.5px + `p500`22 ring 3px | — |
| valid | `p500` | "✓ 사용 가능한 닉네임이에요" (p500) |
| taken | `danger` | "이미 사용 중이에요" (danger) |
| invalid | `danger` | "사용할 수 없는 단어예요" (danger) |

**API:** `GET /users/check-nickname?q={text}` / `PATCH /users/me`  
**엣지:** 중복 닉네임 → 추천 칩 3개 표시 / 부적절 단어 → 로컬 사전으로 즉시 차단

---

#### AUTH-4 · 온보딩 2/3 — 포지션

**역할:** AI 라인업 핵심 입력  
**레이아웃:**
- progress bar (2/3): `p500` 66%
- 헤드라인: "주로 어떤 포지션을 맡나요?" + "라인업 추천에 사용돼요" 13px/c500
- 3×3 그리드: 투수/포수/1루/2루/3루/유격/좌익/중견/우익 포지션 카드
  - 카드 크기: ~90px × ~56px, radius 10px
  - 선택 시: border `p500` 1.5px + bg `p50`
  - 탭 모션: spring 180ms `cubic-bezier(0.34,1.56,0.64,1)`
- 서브 포지션 칩 (다중 선택): pill 형태, 선택 시 bg `p500` / color white
- 하단: "다음" Primary 버튼

**API:** `PATCH /users/me/position { main, subs[] }`  
**엣지:** '잘 모르겠어요' ghost CTA → 주포지션 null로 진행

---

#### AUTH-5 · 온보딩 3/3 — 시작 모드

**역할:** 플로우 분기  
**레이아웃:**
- progress bar (3/3): 100%
- 헤드라인: "어떻게 시작하시겠어요?" 20px/700
- 3개 대형 선택 카드 (full-width, 세로 스택):
  - "팀 만들기" (주장 역할)
  - "팀 참가하기" (초대 코드/QR)
  - "용병으로 시작하기"
  - 각 카드: 아이콘 + 제목 + 설명, radius 14px, border `c200`, 선택 시 border `p500`
- 카드 탭 즉시 다음 화면 (별도 "다음" 버튼 없음)
- 하단 ghost CTA: "나중에 결정하기"

**전환:** 팀 만들기 → TEAM-2 / 팀 참가 → TEAM-3 / 용병 → HOME-1 (empty) / 나중에 → HOME-1  
**API:** `PATCH /users/me/onboarding { step: 3, startMode }`

---

### ② HOME 플로우 (2 screens)

---

#### HOME-1 · 홈 (Empty)

**역할:** 팀 없는 사용자에게 팀 합류 유도  
**레이아웃:**
- status bar (9:41 / 배터리·신호)
- nav bar: "홈" 타이틀 + 우측 🔔 아이콘
- 중앙 empty state: ⚾ 일러스트 72px + "팀과 함께 시작해요" 20px/700 + 설명 14px/c500
- Primary 버튼: "팀 만들기"
- Secondary 버튼: "초대 코드로 참여"
- Ghost 텍스트: "용병으로 먼저 둘러보기"
- 하단 tab bar (홈 active, 인덱스 0)

**API:** `GET /teams/mine → []` 이면 이 화면  
**엣지:** 용병 모드 → 매칭 탭(인덱스 2)으로 우회 안내 토스트 1회

---

#### HOME-2 · 홈 (Dashboard)

**역할:** 앱의 메인 대시보드  
**레이아웃 (스크롤 가능):**

1. **Nav bar (large):**
   - 좌측: 홈 아이콘
   - 우측: 🔔 (알림 있으면 빨간 점)
   - 하단: "안녕하세요, 김주장 님" 26px/700

2. **팀 전환 칩 (가로 스크롤):**
   - 현재 팀: bg `p500`, color white (solid pill)
   - 다른 팀: bg white, border `c200` (outline pill)
   - "+ 팀 추가": border dashed `c200`, color `c400`

3. **다음 경기 카드:**
   - bg white, radius 14px, padding 12px
   - 상단 행: D-N 배지 + 날짜·시간
   - "vs [상대팀]" 18px/700
   - 📍 구장명 11px/c500
   - 구분선 후: 내 응답 상태 배지 (참가: `success`12% bg / 미응답: `c100`)

4. **AI 예측 카드:**
   - bg `p50`, border `p200`, radius 14px
   - "🤖 AI 출석 예측" 레이블
   - "예상 참가 14~16명" 굵게
   - 개별 멤버 예측 행 (참가 확률 %)

5. **팀 공지 카드:**
   - 공지 제목 + 날짜

**팀 칩 탭 시:** skeleton 280ms → 데이터 갱신  
**경기 카드 탭:** MATCH-3 slide-left 420ms  
**API:** `GET /home/dashboard?teamId=… → { nextMatch, aiPrediction, notices[] }`  
**엣지:** 다음 경기 없음 → "예정 경기 없음" secondary card / 알림 없음 → 빨간 점 숨김

---

### ③ TEAM 플로우 (5 screens)

**역할 권한 체계:**

| 역할 | 권한 | 배지 색상 |
|------|------|-----------|
| 주장 | 전체 권한 (생성·삭제·권한 변경) | `s100` bg / `s700` color |
| 매니저 | 경기·공지·멤버 관리 | `p50` bg / `p600` color |
| 회계 | 회비 관리만 | `warning`14% bg |
| 일반 | 출석 응답·열람만 | `c100` bg / `c700` color |

---

#### TEAM-1 · 내 팀 목록

**레이아웃:**
- nav bar: "내 팀" + 우측 "+" 버튼
- 팀 카드 리스트 (각 카드: 팀명 + 역할 배지 + 멤버 수 + chevron)
- 하단 secondary 버튼: "초대 코드로 팀 참가"

---

#### TEAM-2 · 팀 만들기

**레이아웃:**
- back 버튼 네브바: "팀 만들기"
- 섹션별 입력 폼:
  - 팀 로고 (원형 이미지 업로드, 72px)
  - 팀명 (필수)
  - 지역 (선택 picker)
  - 부수 (1~4부, segmented control)
  - 홈구장 (텍스트 or DB 검색)
  - 활동 요일/시간 (다중 선택 칩)
- 하단 고정: "팀 만들기" Primary 버튼

**API:** `POST /teams { name, region, division, homeGround, activeDays }`

---

#### TEAM-3 · 팀 참가 (초대 코드/QR)

**레이아웃:**
- nav bar: "팀 참가하기"
- 탭 전환: "초대 코드" | "QR 코드"
- 초대 코드 탭: 6자리 코드 입력 (각 1자리씩 6개 셀, OTP 스타일)
  - 셀 크기: 38×48px, radius 10px, `JetBrains Mono` 18px/700
  - focus 셀: border `p500` 2px
  - 전체 입력 완료 → 자동 제출
  - 오류 시: 전체 셀 border `danger` 2px + bg `danger`6%
- QR 탭: 카메라 뷰파인더 + "QR 코드를 스캔하세요" 안내

**API:** `POST /teams/join { code }`  
**엣지:** 만료 코드 → "초대 코드가 만료됐어요" / 정원 초과 → "팀 정원이 꽉 찼어요"

---

#### TEAM-4 · 팀 상세

**레이아웃:**
- nav bar: [팀명] + (주장 시) "설정" 버튼
- 팀 정보 섹션: 로고 + 팀명 + 지역·부수 배지 + 멤버 수
- 탭 바: "경기" | "멤버" | "공지"
- 멤버 탭: 역할별 멤버 리스트 (아바타 + 닉네임 + 역할 배지)
- 주장 시 멤버 행 탭 → TEAM-5 (권한 변경 다이얼로그)

---

#### TEAM-5 · 멤버 권한 다이얼로그

**레이아웃:** 바텀 시트
- 멤버 아바타 + 닉네임
- 역할 선택 라디오 리스트 (4가지)
- 하단: "저장" Primary + "추방" Destructive (빨간 텍스트 버튼)
- 추방 탭 → 확인 다이얼로그 (위험 액션)

---

### ④ MATCH 플로우 (5 screens)

---

#### MATCH-1 · 일정 (캘린더/리스트)

**레이아웃:**
- nav bar: "일정" + 뷰 전환 아이콘
- 월 캘린더: 경기 있는 날 `p500` 점 표시
- 하단 스크롤 리스트: 예정 경기 카드들
- (주장 시) 우측 하단 FAB "+" : bg `p500`, 56px circle

---

#### MATCH-2 · 경기 등록 (주장 전용)

**레이아웃:**
- nav bar: "경기 등록" + "저장" 텍스트 버튼
- 입력 폼:
  - 날짜/시간 (date picker)
  - 집합 시간 (time picker, 경기 시간과 분리)
  - 구장 (검색 or 직접 입력)
  - 상대팀 (선택)
  - 반복 설정 (없음/매주/격주)
  - 출석 마감 시간

**API:** `POST /matches { teamId, date, time, gatherTime, venue, opponent, repeat, deadline }`

---

#### MATCH-3 · 경기 상세

**레이아웃:**
- nav bar: back + [상대팀] 제목
- 경기 정보 헤더: D-N 배지 + 날짜·시간 + 구장 + 주소
- 출석 현황 바:
  - 참가 N명 (p500) / 불참 N명 (danger) / 미응답 N명 (c400)
  - 진행 바 (컬러 분할)
- (AI 활성 시) AI 예측 카드: "예상 참가 14~16명" + 멤버별 확률
- 출석 응답 버튼 (현재 상태 표시 + 탭으로 MATCH-4 진입)
- 멤버별 응답 리스트

---

#### MATCH-4 · 출석 응답

**레이아웃:** 바텀 시트 or 풀스크린
- 헤드라인: "[경기명] 출석 응답"
- 응답 선택 카드 5종 (full-width, 세로 스택):

| 옵션 | 아이콘 | 색상 |
|------|--------|------|
| 참가 | ✓ | `p500` |
| 불참 | ✕ | `danger` |
| 미정 | ? | `c500` |
| 늦참 | ⏰ | `warning` |
| 조퇴 | 🚪 | `warning` |

- 선택 시: bg `{color}1A` + border `{color}` 2px + spring 180ms
- 선택 후 사유 입력 (선택값, textarea)
- "확인" Primary 버튼

**API:** `POST /matches/{id}/attendance { status, reason }`

---

#### MATCH-5 · 출석 요약 (주장 전용)

**레이아웃:**
- nav bar: back + "출석 요약"
- 통계 그리드: 참가/불참/미정/늦참/조퇴 카운트
- 미응답자 리스트 (리마인드 버튼 제공)
- "라인업 작성" Primary 버튼
- "카카오톡으로 공유" Secondary 버튼

---

### ⑤ MY 플로우 (2 screens)

---

#### MY-1 · 마이페이지

**레이아웃:**
- nav bar: "마이" large 타이틀
- 프로필 카드: 아바타 72px + 닉네임 18px/700 + 포지션 배지
- 설정 메뉴 리스트 (iOS Settings 스타일):
  - "소속 팀" → TEAM-1
  - "알림 설정" → 채널별 토글
  - "포지션/프로필 편집"
  - "로그아웃" (color: `danger`)
- 메뉴 행 스펙 (height ~44px):
  - default: bg transparent
  - hover: bg `c50`
  - pressed: bg `c100`
  - disabled: opacity 0.4

---

#### MY-2 · 알림 센터

**레이아웃:**
- nav bar: back + "알림"
- 팀별 그룹 헤더
- 알림 카드 리스트:

| 타입 | 아이콘 | 액션 |
|------|--------|------|
| 경기 알림 | 📅 | MATCH-3로 딥링크 |
| 회비 알림 | 💰 | 회비 화면으로 |
| 일반 알림 | 🔔 | — |

- 읽은 알림: opacity 0.6
- 탭 → 해당 화면으로 딥링크

---

### ⑥ 전역 상태 표준

모든 화면에 공통 적용되는 4가지 상태:

#### Empty State

- 중앙 정렬: 아이콘(⚾, 72px) + 제목 20px/700 + 설명 14px/c500
- Primary CTA 버튼 (행동 유도)
- 위협·경고 톤 금지

#### Loading / Skeleton

- `c200` 배경의 둥근 직사각형 shimmer 애니메이션
- 카드 형태를 모방한 skeleton 레이아웃
- 3초 초과 시: 스피너 + "오래 걸리고 있어요" 텍스트

#### Error State

- "다시 시도" Primary 버튼 필수
- "연결을 확인해 주세요" 등 비비난적 카피

#### 상태 결정 트리

```
API 요청 → 0~280ms 즉시 응답 → loading skeleton
                                     └─ 3s+ → Spinner + "오래 걸리고 있어요"

응답 처리 → data=[] → empty state
          → error   → error + 다시 시도
          → ok      → content render
```

---

## Component Anatomy

### 버튼 3종 × 6상태

| 종류 | default | hover | pressed | disabled | loading | focus-ring |
|------|---------|-------|---------|----------|---------|------------|
| Primary | bg `p500` | bg `p600` | bg `p700` + scale 0.97 | opacity 0.5 | spinner | outline `p500` 3px |
| Secondary | border `p500` / color `p500` | bg `p50` | bg `p100` | opacity 0.5 | spinner | outline `p500` 3px |
| Destructive | bg `danger` | bg `#B91C1C` | scale 0.97 | opacity 0.5 | spinner | outline `danger` 3px |

**버튼 공통 스펙:** height 50px, radius 10px, font 15px/600, padding 12px 16px

### 탭 바 (Tab Bar)

- height 64px, padding-bottom 14px (하단 세이프 에리어)
- bg `rgba(255,255,255,0.92)` + `backdrop-filter: blur(10px)`
- border-top `c200` 1px
- 5개 아이템: 홈/일정/매칭/팀/마이
- active: color `p500`, icon 컬러 / inactive: color `c400`, icon grayscale

### AI 카드

- bg `p50`, border `p200` 1px, radius 14px, padding 12px 14px
- 상단: "🤖 AI 예측" label 11px/600 color `p600`
- 본문: 예측 결과 14px/700
- 하단: 개별 멤버 예측 행 (닉네임 + 확률 바)

### 토스트 4종

| 타입 | bg | 용도 |
|------|-----|------|
| success | `success` | 저장 완료 등 |
| warning | `warning` | 주의 필요 |
| danger | `danger` | 오류 발생 |
| info | `info` | 일반 안내 |

공통: 하단 중앙 고정, radius 10px, padding 10px 16px, 3초 후 fade-out

---

## Interactions & Behavior

### 네비게이션

- Forward 전환: slide-left 420ms ease-in-out
- Back 전환: slide-right 420ms ease-in-out
- Modal/바텀시트: slide-up 320ms + dim overlay 50%
- 탭 전환: cross-fade 200ms (slide 없음)

### AI 기능 타이밍

- AI 출석 예측: D-2(경기 48시간 전)부터 활성화
- 예측 결과: 범위(min~max) 형태만 표시 (예: "14~16명")
- AI 라인업 추천: v0.5+ 기능 (MVP 이후)

### 알림 자동 발송 규칙

| 알림 | 시점 | 채널 |
|------|------|------|
| 새 경기 등록 | 즉시 | 푸시 + 알림톡 |
| 출석 리마인드 | D-2, D-1 | 푸시 + 알림톡(D-1만) |
| 라인업 확정 | 주장 확정 시 | 푸시 + 알림톡 |
| 회비 청구 | 청구일 | 푸시 + 알림톡 |
| 매칭 요청 | 요청 시 | 푸시 + 알림톡 |

---

## State Management

### 필요한 전역 상태

```typescript
// 인증
authToken: string | null
currentUser: {
  id: string
  nickname: string
  position: { main: string, subs: string[] }
  onboardingStep: 1 | 2 | 3 | null
}

// 팀 컨텍스트 (홈 칩 전환)
selectedTeamId: string
myTeams: Team[]

// 알림
unreadNotifCount: number
```

### 주요 API 엔드포인트

```
POST /auth/oauth/{provider}        → { accessToken, isNew }
GET  /users/check-nickname?q=      → { available: bool }
PATCH /users/me                    → User
PATCH /users/me/position           → User
PATCH /users/me/onboarding         → User

GET  /teams/mine                   → Team[]
POST /teams                        → Team
POST /teams/join                   → TeamMember

GET  /home/dashboard?teamId=       → DashboardData
GET  /matches?teamId=&month=       → Match[]
POST /matches                      → Match
GET  /matches/{id}                 → MatchDetail
POST /matches/{id}/attendance      → Attendance
```

---

## Assets

- **폰트:** Pretendard (`cdn.jsdelivr.net/npm/pretendard`) + JetBrains Mono (구글 폰트)
- **아이콘:** 현재 시안에서는 이모지 사용 (⚾ 🏠 📅 🤝 👤 🔔 💰). 프로덕션에서는 **Phosphor Icons** (thin/regular weight) 권장
- **일러스트:** Empty state의 ⚾ 아이콘은 브랜드 SVG 일러스트로 교체 예정

---

## Platform Target

- **iOS 16.0+** (우선)
- **Android 10.0+** (v1.5)
- 기기 프레임: 320×692 기준 디자인 (iPhone SE 크기 기준, safe area 대응 필수)

---

## Files in This Bundle

| 파일 | 설명 |
|------|------|
| `README.md` | 이 문서 |
| `flows/index.html` | 6개 플로우 인덱스 (브라우저에서 열기) |
| `flows/auth.html` | ① 인증 플로우 스토리보드 |
| `flows/home.html` | ② 홈 플로우 스토리보드 |
| `flows/team.html` | ③ 팀 플로우 스토리보드 |
| `flows/match.html` | ④ 경기 플로우 스토리보드 |
| `flows/my.html` | ⑤ 마이·알림 플로우 스토리보드 |
| `flows/states.html` | ⑥ 전역 상태 표준 스토리보드 |
| `flows/dugout-screens.jsx` | 화면 컴포넌트 소스 (토큰 포함) |
| `flows/storyboard-kit.jsx` | 스토리보드 레이아웃 유틸 |
| `flows/design-canvas.jsx` | 캔버스 레이아웃 컴포넌트 |
| `Dugout Design Spec.html` | 디자인 토큰 전체 사전 (색상·타이포·컴포넌트 anatomy) |
| `uploads/PRD.md` | 제품 요구사항 문서 |

> 📌 **시작점:** `flows/index.html`을 브라우저에서 열면 6개 플로우 전체를 탐색할 수 있습니다.
> 각 플로우 HTML 파일은 `dugout-screens.jsx`에 의존하므로 폴더 구조를 유지하세요.
