# Screens — 화면 사양

> **목적**: v1.0까지의 모든 화면 사양을 한 문서에 모은다. 각 화면은 SwiftUI(iOS) / Compose(Android)로 1:1 구현 가능한 수준의 구조 정보를 담는다 (픽셀 단위 X).
>
> **포맷**: 각 화면은 동일한 형식. `prototype/index.html`의 anchor와 동기화.
>
> **우선순위 (PRD 4-2 + 8장 릴리스)**:
> - 🟢 **P0 / v0.1 Alpha** — F1·F2·F3 (인증·팀·경기·출석)
> - 🟡 **P0 / v0.5 Beta** — F4·F9 (라인업·알림 인프라)
> - 🟠 **P1 / v0.5 Beta** — F5 (회비)
> - 🔴 **P1 / v1.0** — F6·F7 (매칭·용병)
> - ⚪ **P2 / v1.0** — F8 (구장)

## 화면 인벤토리 (목차)

### 인증 (AUTH)
- [SCR-AUTH-SPLASH](#scr-auth-splash) 🟢
- [SCR-AUTH-LOGIN](#scr-auth-login) 🟢
- [SCR-AUTH-OAUTH](#scr-auth-oauth) 🟢
- [SCR-AUTH-ONBOARDING](#scr-auth-onboarding) 🟢
- [SCR-AUTH-ONBOARDING-CHOOSE](#scr-auth-onboarding-choose) 🟢

### 홈 (HOME)
- [SCR-HOME-DASHBOARD](#scr-home-dashboard) 🟢
- [SCR-HOME-EMPTY](#scr-home-empty) 🟢

### 팀 (TEAM)
- [SCR-TEAM-LIST](#scr-team-list) 🟢
- [SCR-TEAM-CREATE](#scr-team-create) 🟢
- [SCR-TEAM-JOIN](#scr-team-join) 🟢
- [SCR-TEAM-DETAIL](#scr-team-detail) 🟢
- [SCR-TEAM-EDIT](#scr-team-edit) 🟢
- [SCR-TEAM-MEMBER-DIALOG](#scr-team-member-dialog) 🟢

### 경기 (MATCH)
- [SCR-MATCH-LIST](#scr-match-list) 🟢
- [SCR-MATCH-CREATE](#scr-match-create) 🟢
- [SCR-MATCH-DETAIL](#scr-match-detail) 🟢
- [SCR-MATCH-EDIT](#scr-match-edit) 🟢

### 출석 (ATT)
- [SCR-ATT-VOTE](#scr-att-vote) 🟢
- [SCR-ATT-SUMMARY](#scr-att-summary) 🟢

### 라인업 (LINEUP)
- [SCR-LINEUP-VIEW](#scr-lineup-view) 🟡
- [SCR-LINEUP-EDIT](#scr-lineup-edit) 🟡

### 회비 (FEE)
- [SCR-FEE-LIST](#scr-fee-list) 🟠
- [SCR-FEE-CREATE](#scr-fee-create) 🟠
- [SCR-FEE-DETAIL](#scr-fee-detail) 🟠

### 매칭 (MATCHING)
- [SCR-MATCHING-HUB](#scr-matching-hub) 🔴
- [SCR-MATCHING-CREATE](#scr-matching-create) 🔴
- [SCR-MATCHING-RECOMMEND](#scr-matching-recommend) 🔴
- [SCR-MATCHING-PROPOSAL](#scr-matching-proposal) 🔴
- [SCR-MATCHING-DETAIL](#scr-matching-detail) 🔴
- [SCR-MATCHING-RESULT](#scr-matching-result) 🔴

### 용병 (MERC)
- [SCR-MERC-PROFILE](#scr-merc-profile) 🔴
- [SCR-MERC-PROFILE-EDIT](#scr-merc-profile-edit) 🔴
- [SCR-MERC-RECRUIT](#scr-merc-recruit) 🔴
- [SCR-MERC-APPLICANTS](#scr-merc-applicants) 🔴

### 구장 (GROUND)
- [SCR-GROUND-LIST](#scr-ground-list) ⚪
- [SCR-GROUND-DETAIL](#scr-ground-detail) ⚪

### 알림 (NOTIF)
- [SCR-NOTIF-CENTER](#scr-notif-center) 🟡

### 마이 (MY)
- [SCR-MY-PROFILE](#scr-my-profile) 🟢
- [SCR-MY-NOTIF](#scr-my-notif) 🟡
- [SCR-MY-ACCOUNT](#scr-my-account) 🟢

---

## 화면별 사양 형식

```
### SCR-XXX-YYY ⚪/🟢/🟡/🟠/🔴

| 진입점     | (어디서 들어오는가) |
| 표현       | push / sheet / fullScreenCover |
| 목적       | 한 줄 |
| 요소       | 화면 구성 (header / section / list / form 등) |
| 상태       | default / empty / loading / failure |
| 데이터     | API endpoint + 표시 필드 |
| 권한       | 역할별 가시성·액션 |
| 인터랙션   | 탭 / 스와이프 / long-press → 어디로 |
| 연관 알림  | 이 화면을 가리키는 알림 |
| 와이어     | prototype/index.html#scr-... 또는 인라인 ASCII |
```

---

# 인증 (AUTH)

## SCR-AUTH-SPLASH 🟢

| 진입점 | 앱 cold start |
| 표현 | (App Root) |
| 목적 | 토큰 복원 동안 로고 표시. sleep 없음, 복원 끝나면 즉시 탭 진입 |
| 요소 | 중앙 로고 (야구 다이아몬드 아이콘) + "Dugout" 텍스트 + 배경 |
| 상태 | 단일 (loading 인디케이터 X — 보일 듯 말 듯 빠르게) |
| 데이터 | (없음) — TokenStore 조회 + GET /users/me (5초 timeout) |
| 권한 | 모두 |
| 인터랙션 | (없음) — 자동 전환 |
| 와이어 | [#scr-auth-splash](prototype/index.html#scr-auth-splash) |

## SCR-AUTH-LOGIN 🟢

| 진입점 | 마이 탭 → "로그인" / Deferred Auth 트리거 |
| 표현 | sheet (drag-down dismiss 가능) |
| 목적 | OAuth 4종 + 비로그인 계속 옵션 |
| 요소 | 헤더(타이틀 "로그인" + ✕) / 로고 영역 / 로그인 버튼 4개(카카오·네이버·Apple·Google) / "둘러보기 계속" 텍스트 버튼 |
| 상태 | default / OAuth 진행 중 (버튼 비활성 + spinner) / 실패 (상단 banner) |
| 데이터 | POST /api/v1/auth/login {provider, token} |
| 권한 | 모두 (비로그인 진입) |
| 인터랙션 | 카카오 → fullScreenCover OAuth → 성공 시 탭 진입 또는 pending action 재실행 |
| 연관 알림 | (없음) |
| 와이어 | [#scr-auth-login](prototype/index.html#scr-auth-login) |

**OAuth 제공자별 토큰 처리 (PRD F1):**
- 카카오·네이버·구글: AccessToken을 서버 전달
- Apple: IdentityToken(JWT) 서버 전달

## SCR-AUTH-OAUTH 🟢

| 진입점 | LOGIN의 OAuth 버튼 탭 |
| 표현 | fullScreenCover (시스템 OAuth web view) |
| 목적 | 외부 OAuth 진행 |
| 요소 | (시스템 web view) — `ASWebAuthenticationSession` |
| 상태 | 진행 / 사용자 취소 / 실패 |
| 데이터 | (외부) |
| 권한 | — |
| 와이어 | (시스템 컴포넌트, 와이어 X) |

## SCR-AUTH-ONBOARDING 🟢

| 진입점 | 신규 가입 (LOGIN 후 is_new_user=true) |
| 표현 | fullScreenCover (스킵 불가, 단계 진행) |
| 목적 | 닉네임·프로필 사진·포지션·타석·투타 입력 |
| 요소 | step indicator (1/3) / Form / "다음" CTA |
| 상태 | step 진행 / 유효성 실패 (inline) / 제출 실패 |
| 데이터 | PUT /users/me {nickname, profile_img_url, positions, ...} |
| 권한 | 신규 사용자만 |
| 인터랙션 | "다음" → step 진행 / 마지막 step 후 SCR-AUTH-ONBOARDING-CHOOSE |
| 와이어 | [#scr-auth-onboarding](prototype/index.html#scr-auth-onboarding) |

## SCR-AUTH-ONBOARDING-CHOOSE 🟢

| 진입점 | ONBOARDING 마지막 step |
| 표현 | fullScreenCover step의 마지막 화면 |
| 목적 | "팀 만들기 / 팀 참가 / 용병으로 시작" 선택 |
| 요소 | 큰 카드 3개 (각각 아이콘·제목·설명) / "나중에" 작은 텍스트 |
| 인터랙션 | 카드 탭 → 해당 sheet 또는 화면으로 이동 / "나중에" → 홈 진입 |
| 와이어 | [#scr-auth-onboarding-choose](prototype/index.html#scr-auth-onboarding-choose) |

---

# 홈 (HOME)

## SCR-HOME-DASHBOARD 🟢

| 진입점 | 홈 탭 root (로그인 + 팀 1+) |
| 표현 | NavigationStack root |
| 목적 | 다음 경기·내 출석·AI 인사이트·팀 공지를 한눈에 |
| 요소 | navigation bar(.large, "안녕하세요, 닉네임" + 🔔) / 팀 carousel (다중 팀 시) / "다음 경기" 카드 (날짜·시간·구장·내 응답·D-N) / "출석 미응답" 알림 카드 (조건부) / AI 인사이트 카드 (v0.5+: 출석 예측 등) / 팀 공지 섹션 (v1.0+) |
| 상태 | default / empty (다음 경기 없음 → "예정된 경기가 없어요") / loading / failure |
| 데이터 | GET /users/me/teams (홈 요약), GET /matches/upcoming (가까운 경기 1개) |
| 권한 | 로그인 + 팀 1+ |
| 인터랙션 | 다음 경기 카드 탭 → SCR-MATCH-DETAIL push / 🔔 → SCR-NOTIF-CENTER sheet |
| 연관 알림 | 모든 알림이 이 화면으로 deep-link 가능 |
| 와이어 | [#scr-home-dashboard](prototype/index.html#scr-home-dashboard) |

## SCR-HOME-EMPTY 🟢

| 진입점 | 홈 탭 root (비로그인 또는 팀 0) |
| 목적 | 첫 사용자 가이드 |
| 요소 | 큰 일러스트 / 환영 문구 / "팀 만들기" + "코드로 참여" CTA / "둘러보기" |
| 상태 | 단일 |
| 인터랙션 | "팀 만들기" → SCR-TEAM-CREATE sheet (비로그인 시 LOGIN 트리거 후) |
| 와이어 | [#scr-home-empty](prototype/index.html#scr-home-empty) |

---

# 팀 (TEAM)

## SCR-TEAM-LIST 🟢

| 진입점 | 팀 탭 root |
| 목적 | 내가 속한 팀 목록 + 새 팀 만들기/참여 |
| 요소 | navigation bar(.inline "내 팀" + 🔔) / 팀 카드 list (팀명·역할 배지·멤버 수·로고) / 하단 sticky "+ 팀 만들기" / "코드로 참여" 텍스트 버튼 |
| 상태 | default / empty (속한 팀 없음 → "팀을 만들거나 코드로 참여하세요") / loading / failure |
| 데이터 | GET /api/v1/users/me/teams |
| 권한 | 모두 (비로그인은 empty) |
| 인터랙션 | 팀 카드 탭 → SCR-TEAM-DETAIL push |
| 와이어 | [#scr-team-list](prototype/index.html#scr-team-list) |

## SCR-TEAM-CREATE 🟢

| 진입점 | TEAM-LIST 또는 HOME-EMPTY → "팀 만들기" |
| 표현 | sheet (Form, interactiveDismissDisabled while submitting) |
| 목적 | 팀 신규 생성 (생성자 = CAPTAIN) |
| 요소 | navigation bar(✕ 취소 / "저장") / Form section: 팀 정보(팀명·로고[v0.5+]·지역·부수 picker) / 활동(요일 multi-select·활동시간 text) / 라인업 모드(segmented "균등/실력") |
| 상태 | editing / submitting (저장 버튼 → ProgressView, 취소 비활성) / failed (Form 상단 inline banner) |
| 데이터 | POST /api/v1/teams |
| 권한 | 로그인 사용자 |
| 인터랙션 | 저장 → 성공 시 dismiss + SCR-TEAM-DETAIL push |
| 와이어 | [#scr-team-create](prototype/index.html#scr-team-create) |

## SCR-TEAM-JOIN 🟢

| 진입점 | TEAM-LIST 또는 HOME-EMPTY → "코드로 참여" |
| 표현 | sheet |
| 목적 | 초대 코드 입력 → 가입 |
| 요소 | navigation bar / 큰 입력창(코드 6자리) / 자동 대문자 변환 / "참여하기" 버튼 / QR 스캔 옵션(v0.5+) |
| 상태 | editing / submitting / failed (잘못된 코드, 만료, 이미 가입) |
| 데이터 | POST /api/v1/teams/join {code} |
| 권한 | 로그인 |
| 인터랙션 | 성공 → dismiss + SCR-TEAM-DETAIL push |
| 와이어 | [#scr-team-join](prototype/index.html#scr-team-join) |

## SCR-TEAM-DETAIL 🟢

| 진입점 | TEAM-LIST → 카드 탭 / 알림 딥링크 |
| 표현 | push |
| 목적 | 팀 정보 + 멤버 + 빠른 액션 |
| 요소 | navigation bar(.inline 팀명 + ⋯ 권한 시) / Hero(로고·팀명·지역·부수·매니저 배지) / 빠른 액션 grid(경기 등록·라인업·회비·초대 코드 — 권한 분기) / 활동 정보(요일·시간) / 멤버 섹션 (전체 보기 push 가능) / 다음 경기 미리보기 (있을 시) |
| 상태 | default / loading / failure |
| 데이터 | GET /teams/{id}, GET /teams/{id}/members |
| 권한 | 멤버 이상 (조회). 액션 버튼은 권한별 hidden 또는 표시 |
| 인터랙션 | 멤버 탭 → ConfirmationDialog (CAPTAIN만, 자기/CAPTAIN 제외) / 초대 코드 → 모달 또는 inline 표시 / 경기 등록 → SCR-MATCH-CREATE / ⋯ → 팀 수정·해체 |
| 와이어 | [#scr-team-detail](prototype/index.html#scr-team-detail) |

## SCR-TEAM-EDIT 🟢

| 진입점 | TEAM-DETAIL → ⋯ → "팀 수정" |
| 표현 | sheet |
| 목적 | 팀 기본 정보 수정 (CAPTAIN/MANAGER) |
| 요소 | TEAM-CREATE와 동일 Form (기존 값 prefill) |
| 권한 | CAPTAIN/MANAGER 만 진입점 표시 |
| 데이터 | PUT /teams/{id} (모든 필드 optional) |
| 와이어 | (TEAM-CREATE와 동일 골격) |

## SCR-TEAM-MEMBER-DIALOG 🟢

| 진입점 | TEAM-DETAIL 멤버 탭 (CAPTAIN, 자기/CAPTAIN 제외) |
| 표현 | confirmationDialog |
| 목적 | 한 멤버에 대한 다중 액션 |
| 요소 | dialog title = 멤버 닉네임 / actions: "매니저로 변경" "회계로 변경" "일반으로 변경" "추방"(destructive) "취소" |
| 데이터 | PUT /teams/{id}/members/{memberId} {role} 또는 DELETE |
| 권한 | CAPTAIN |
| 인터랙션 | 액션 후 자동 reload, 에러는 alert |
| 와이어 | [#scr-team-member-dialog](prototype/index.html#scr-team-member-dialog) |

---

# 경기 (MATCH)

## SCR-MATCH-LIST 🟢

| 진입점 | 일정 탭 root |
| 목적 | 속한 모든 팀의 경기 + 캘린더 |
| 요소 | navigation bar(.inline "일정" + 팀 필터 picker + 🔔) / 상단 segmented(캘린더/리스트) / 캘린더 뷰(월별 dot) 또는 리스트(D-N · 시간 · 상대팀 · 구장 · 내 응답 배지) / FAB "+" (CAPTAIN/MANAGER) |
| 상태 | default / empty (예정 경기 없음) / loading / failure |
| 데이터 | GET /teams/{id}/matches?from&to (팀별 호출 후 클라 머지) |
| 권한 | 멤버 이상 |
| 인터랙션 | 캘린더 dot 탭 → 해당 일자 리스트 / 카드 탭 → SCR-MATCH-DETAIL / FAB 탭 → SCR-MATCH-CREATE |
| 와이어 | [#scr-match-list](prototype/index.html#scr-match-list) |

## SCR-MATCH-CREATE 🟢

| 진입점 | MATCH-LIST FAB / TEAM-DETAIL 빠른 액션 |
| 표현 | sheet (Form) |
| 목적 | 경기 등록 |
| 요소 | navigation bar(✕/저장) / Form: 경기 정보(날짜·경기시간·집합시간) / 상대(상대팀명 또는 "미정") / 구장(이름·주소·DB 연결[v0.5+]) / 출석 투표(투표 마감 시간) / 메모 / 반복 일정 토글(v0.5+) |
| 상태 | editing / submitting / failed |
| 데이터 | POST /teams/{id}/matches |
| 권한 | CAPTAIN/MANAGER |
| 인터랙션 | 성공 → dismiss + 일정 reload |
| 연관 알림 | 등록 시 멤버 자동 알림 (v0.5+ FCM) |
| 와이어 | [#scr-match-create](prototype/index.html#scr-match-create) |

## SCR-MATCH-DETAIL 🟢

| 진입점 | MATCH-LIST / HOME 카드 / 알림 딥링크 |
| 표현 | push |
| 목적 | 경기 1건의 모든 정보 + 액션 (출석·라인업·결과) |
| 요소 | navigation bar(D-N + 팀명, ⋯ 권한 시) / Hero(날짜·경기시간·집합시간·구장·상대) / 내 응답 카드("참가하기" 또는 현재 응답 + 변경) / 출석 현황 섹션(참가/불참/미정/늦참/조퇴/미응답 카운트 + 막대) / AI 출석 예측 카드(v0.5+) / 라인업 섹션(미생성/초안/확정 분기 — v0.5+) / 메모 섹션 / 결과 섹션(경기 종료 후) |
| 상태 | default / loading / failure |
| 데이터 | GET /matches/{id} + GET /matches/{id}/attendance |
| 권한 | 멤버 이상 (조회). 라인업·수정 버튼 권한별 |
| 인터랙션 | "투표" → SCR-ATT-VOTE sheet / "전체 보기" → SCR-ATT-SUMMARY push / "라인업" → SCR-LINEUP-VIEW push / ⋯ → ConfirmationDialog(수정·취소) |
| 연관 알림 | 새 경기·출석 리마인드·라인업 확정·매칭 성사 모두 이 화면 deep-link |
| 와이어 | [#scr-match-detail](prototype/index.html#scr-match-detail) |

## SCR-MATCH-EDIT 🟢

| 진입점 | MATCH-DETAIL ⋯ → "수정" |
| 표현 | sheet |
| 목적 | 경기 정보 수정 |
| 요소 | MATCH-CREATE와 동일 Form (prefill) |
| 권한 | CAPTAIN/MANAGER |
| 데이터 | PUT /matches/{id} |
| 와이어 | (MATCH-CREATE와 동일 골격) |

---

# 출석 (ATT)

## SCR-ATT-VOTE 🟢

| 진입점 | MATCH-DETAIL "투표" / 알림 딥링크 |
| 표현 | sheet (medium detent — 화면 절반) |
| 목적 | 본인 출석 응답 (5상태) |
| 요소 | drag indicator / 헤더(경기 요약 1줄) / 5상태 picker(참가 ✅ / 불참 ❌ / 미정 ❓ / 늦참 ⏰ / 조퇴 🚪) / 사유 text(선택) / "응답하기" full-width CTA |
| 상태 | editing / submitting (CTA → spinner) / failed (alert) |
| 데이터 | POST(첫 투표) 또는 PUT(변경) /matches/{id}/attendance {status, reason} |
| 권한 | 팀 멤버 |
| 인터랙션 | 응답 후 dismiss + MATCH-DETAIL 갱신 |
| 연관 알림 | 출석 리마인드 |
| 와이어 | [#scr-att-vote](prototype/index.html#scr-att-vote) |

## SCR-ATT-SUMMARY 🟢

| 진입점 | MATCH-DETAIL "전체 보기" |
| 표현 | push |
| 목적 | 멤버별 응답·사유·시간을 한 리스트에서 확인 |
| 요소 | navigation bar("출석 현황") / 상단 카운트 카드 (참가 N / 불참 N / 미정 N / 늦참 N / 조퇴 N / 미응답 N) / 위험 배너 (예상 9명 미만 시) / 멤버 리스트 (탭: 전체/참가/불참/미응답) / 각 row: 닉네임·등번호·응답·사유·응답시간 |
| 상태 | default / loading / failure |
| 데이터 | GET /matches/{id}/attendance |
| 권한 | 멤버 이상 |
| 인터랙션 | 미응답 멤버 long-press → "독려 메시지 보내기"(v0.5+ — 알림톡) / 위험 배너 → SCR-MERC-RECRUIT(v1.0+) |
| 와이어 | [#scr-att-summary](prototype/index.html#scr-att-summary) |

---

# 라인업 (LINEUP) — v0.5 Beta

## SCR-LINEUP-VIEW 🟡

| 진입점 | MATCH-DETAIL 라인업 섹션 |
| 표현 | push |
| 목적 | 라인업 조회 + 카드 이미지 공유 |
| 요소 | navigation bar(라인업 + 편집 버튼[CAPTAIN/MANAGER]) / 야구장 다이아몬드 비주얼(9 포지션 + 지명타자) / 타순 리스트(1~9 + 대기) / 벤치 멤버 / 카드 이미지 미리보기 + "카카오톡 공유" 버튼 / 미생성 시 "AI 추천" CTA |
| 상태 | empty(미생성) / draft(초안) / confirmed(확정) / loading / failure |
| 데이터 | GET /matches/{id}/lineup, GET /matches/{id}/lineup/card |
| 권한 | 멤버는 확정 후 조회만, CAPTAIN/MANAGER는 항상 |
| 인터랙션 | "AI 추천" → POST /lineup/recommend → SCR-LINEUP-EDIT push / "편집" → SCR-LINEUP-EDIT push / "공유" → 시스템 share sheet |
| 연관 알림 | 라인업 확정·변경 |
| 와이어 | [#scr-lineup-view](prototype/index.html#scr-lineup-view) |

## SCR-LINEUP-EDIT 🟡

| 진입점 | LINEUP-VIEW "편집" 또는 "AI 추천" 결과 |
| 표현 | push |
| 목적 | 드래그&드롭 + 확정 |
| 요소 | navigation bar(✕/확정) / 다이아몬드 + 벤치 / 멤버를 드래그해서 포지션에 드롭 / 타순 reorder / 모드 토글(균등/실력) / "재추천" 버튼 / 멤버 long-press → 강제 제외 |
| 상태 | editing / submitting / confirming(alert: "확정 시 멤버에게 알림 발송") / failed |
| 데이터 | POST/PUT /matches/{id}/lineup, POST /lineup/confirm |
| 권한 | CAPTAIN/MANAGER |
| 인터랙션 | 확정 → alert 확인 → API → 카드 이미지 자동 생성 → 알림 발송 → dismiss |
| 와이어 | [#scr-lineup-edit](prototype/index.html#scr-lineup-edit) |

---

# 회비 (FEE) — v0.5 Beta

## SCR-FEE-LIST 🟠

| 진입점 | TEAM-DETAIL → "회비" 빠른 액션 |
| 표현 | push |
| 목적 | 팀 회비 목록 + 본인 납부 현황 |
| 요소 | navigation bar(팀명 + 회비, +[권한 시]) / 재정 요약 카드(총 수입·지출·잔액) / 회비 항목 리스트(타입 배지·금액·기한·내 납부 여부) |
| 상태 | default / empty / loading / failure |
| 데이터 | GET /teams/{id}/fees, GET /teams/{id}/finance/summary |
| 권한 | 멤버 이상. 생성·수정은 CAPTAIN/MANAGER/ACCOUNTANT |
| 인터랙션 | 항목 탭 → SCR-FEE-DETAIL push / "+" → SCR-FEE-CREATE sheet |
| 연관 알림 | 회비 청구·미납 리마인드 |
| 와이어 | [#scr-fee-list](prototype/index.html#scr-fee-list) |

## SCR-FEE-CREATE 🟠

| 진입점 | FEE-LIST + |
| 표현 | sheet |
| 요소 | 타입(정기/경기별 segmented) / 금액 / 기한 / 대상(전체·선택) / 메모 |
| 데이터 | POST /teams/{id}/fees |
| 권한 | CAPTAIN/MANAGER/ACCOUNTANT |
| 와이어 | [#scr-fee-create](prototype/index.html#scr-fee-create) |

## SCR-FEE-DETAIL 🟠

| 진입점 | FEE-LIST 항목 탭 / 알림 딥링크 |
| 표현 | push |
| 목적 | 1 회비 항목의 멤버별 납부 상태 |
| 요소 | navigation bar / 항목 정보 카드(금액·기한·D-N) / 멤버 리스트(닉네임·납부 상태·납부일) / 권한 시 "수동 처리" 버튼 |
| 데이터 | GET /teams/{id}/fees/{feeId}/payments |
| 권한 | 멤버는 본인만, 회계 이상은 전체 |
| 인터랙션 | 회계: row 탭 → "납부 처리" alert |
| 와이어 | [#scr-fee-detail](prototype/index.html#scr-fee-detail) |

---

# 매칭 (MATCHING) — v1.0

## SCR-MATCHING-HUB 🔴

| 진입점 | 매칭 탭 root |
| 표현 | NavigationStack root |
| 요소 | navigation bar("매칭") / 상단 segmented(연습경기 / 용병 / 구장) / 각 segment의 root 콘텐츠 (HUB 내에서 전환) |
| 와이어 | [#scr-matching-hub](prototype/index.html#scr-matching-hub) |

## SCR-MATCHING-CREATE 🔴

| 진입점 | MATCHING-HUB 연습경기 → "요청 등록" |
| 표현 | sheet |
| 요소 | 희망 날짜·시간 / 지역(현재 위치 또는 직접) / 부수 범위 / 홈/원정/무관 / 메모 |
| 데이터 | POST /matching/requests |
| 권한 | CAPTAIN/MANAGER |
| 와이어 | [#scr-matching-create](prototype/index.html#scr-matching-create) |

## SCR-MATCHING-RECOMMEND 🔴

| 진입점 | MATCHING-CREATE 성공 후 자동 push / MATCHING-HUB 활성 요청 탭 |
| 표현 | push |
| 요소 | 추천 팀 카드 list (팀명·부수·실력 유사도·거리·시간 호환·매너 점수) / 각 카드에 "제안하기" 버튼 |
| 데이터 | GET /matching/requests/{id}/recommend |
| 권한 | CAPTAIN/MANAGER |
| 인터랙션 | 카드 탭 → 상대 팀 미리보기 sheet / "제안하기" → POST propose → 토스트 |
| 와이어 | [#scr-matching-recommend](prototype/index.html#scr-matching-recommend) |

## SCR-MATCHING-PROPOSAL 🔴

| 진입점 | 알림 "매칭 제안" 딥링크 |
| 표현 | push |
| 요소 | 상대팀 정보 / 매칭 적합도 / 수락·거절 버튼 |
| 데이터 | GET /matching/requests/{id} |
| 권한 | CAPTAIN/MANAGER (수신팀) |
| 인터랙션 | 수락 → POST accept → 매칭 성사 / 거절 → reject |
| 와이어 | [#scr-matching-proposal](prototype/index.html#scr-matching-proposal) |

## SCR-MATCHING-DETAIL 🔴

| 진입점 | 매칭 성사 후 |
| 표현 | push |
| 요소 | 양 팀 정보 / 일시·구장 / 인앱 채팅 진입(v1.0+) / 결과 입력 진입 |
| 와이어 | [#scr-matching-detail](prototype/index.html#scr-matching-detail) |

## SCR-MATCHING-RESULT 🔴

| 진입점 | MATCHING-DETAIL 경기 후 |
| 표현 | sheet |
| 요소 | 스코어 입력(양 팀) / 매너 평가(별점 + 태그 multi-select) |
| 데이터 | POST /matching/results/{id} + .../review |
| 와이어 | [#scr-matching-result](prototype/index.html#scr-matching-result) |

---

# 용병 (MERC) — v1.0

## SCR-MERC-PROFILE 🔴

| 진입점 | MATCHING-HUB 용병 (본인이 무소속/용병 활성) |
| 표현 | push |
| 요소 | 본인 용병 프로필 요약 / 추천 받은 경기 list / 활성 토글 |
| 데이터 | GET /mercenary/profile, GET /mercenary/requests/recommended |

## SCR-MERC-PROFILE-EDIT 🔴

| 진입점 | MERC-PROFILE / MY-PROFILE → "용병 프로필" |
| 표현 | push |
| 요소 | 포지션·실력·지역·요일·시간·소개 |
| 데이터 | PUT /mercenary/profile |

## SCR-MERC-RECRUIT 🔴

| 진입점 | MATCH-DETAIL 위험 배너 / MATCHING-HUB 용병 → "모집 등록" |
| 표현 | sheet |
| 요소 | 필요 포지션·실력·참가비 / 자동으로 경기 정보 prefill (MATCH-DETAIL 진입 시) |
| 데이터 | POST /mercenary/requests |
| 권한 | CAPTAIN/MANAGER |

## SCR-MERC-APPLICANTS 🔴

| 진입점 | 모집 등록 후 알림 + MATCHING-HUB |
| 표현 | push |
| 요소 | 지원 용병 list (프로필·평가 점수·이력) / 수락 버튼 |
| 데이터 | POST /mercenary/requests/{id}/accept/{userId} |
| 권한 | CAPTAIN/MANAGER |

---

# 구장 (GROUND) — v1.0

## SCR-GROUND-LIST ⚪

| 진입점 | MATCHING-HUB 구장 |
| 요소 | 지도 + 리스트 토글 / 필터(시설·가격·조명·잔디) / 거리순 정렬 |
| 데이터 | GET /grounds (위치·필터) |
| 권한 | 모두 (비로그인 OK) |

## SCR-GROUND-DETAIL ⚪

| 진입점 | GROUND-LIST 카드 |
| 요소 | 사진 carousel / 정보(주소·시설·가격) / 리뷰 list / "리뷰 작성" |
| 데이터 | GET /grounds/{id}, POST /grounds/{id}/reviews |

---

# 알림 (NOTIF)

## SCR-NOTIF-CENTER 🟡

| 진입점 | 모든 탭 헤더 🔔 |
| 표현 | sheet (large detent) |
| 요소 | navigation bar(알림 + ✕) / 팀별 그룹헤더 + 알림 list (제목·본문·시간·읽음 dot) / 빈 상태 |
| 상태 | default / empty / loading / failure |
| 데이터 | GET /notifications |
| 권한 | 로그인 |
| 인터랙션 | 항목 탭 → 딥링크 라우팅 + 읽음 처리 |
| 와이어 | [#scr-notif-center](prototype/index.html#scr-notif-center) |

---

# 마이 (MY)

## SCR-MY-PROFILE 🟢

| 진입점 | 마이 탭 root |
| 표현 | NavigationStack root |
| 요소 | navigation bar(.large "마이페이지" + 🔔) / 프로필 카드(사진·닉네임·OAuth 표기 "카카오로 로그인됨") / 메뉴 list(소속 팀 / 용병 프로필 / 알림 설정 / 계정 / 로그아웃) |
| 상태 | 로그인 / 비로그인 분기 (비로그인 시 "로그인" CTA만 표시) |
| 데이터 | GET /users/me |
| 와이어 | [#scr-my-profile](prototype/index.html#scr-my-profile) |

## SCR-MY-NOTIF 🟡

| 진입점 | MY-PROFILE → "알림 설정" |
| 표현 | push |
| 요소 | 채널 (푸시/알림톡/이메일 토글) / 유형별 (출석/라인업/회비/매칭/용병 토글) / 방해 금지 시간 picker |
| 데이터 | PUT /users/me/notification-settings |

## SCR-MY-ACCOUNT 🟢

| 진입점 | MY-PROFILE → "계정 관리" |
| 표현 | push |
| 요소 | 닉네임 변경 / 프로필 사진 변경 / 이메일 표시(읽기) / 회원 탈퇴(파괴적, alert 2단계) |
| 데이터 | PUT /users/me, DELETE /users/me |

---

## 부록 A — 화면별 P0/P1/P2 매핑 (PRD cross-check)

| PRD 기능 | P | 관련 화면 |
|---------|---|----------|
| F1 인증 | P0 | AUTH-SPLASH, AUTH-LOGIN, AUTH-OAUTH, AUTH-ONBOARDING(2) |
| F2 팀 | P0 | TEAM-LIST, TEAM-CREATE, TEAM-JOIN, TEAM-DETAIL, TEAM-EDIT, TEAM-MEMBER-DIALOG, MY-PROFILE(소속 팀) |
| F3 경기·출석 | P0 | MATCH-LIST, MATCH-CREATE, MATCH-DETAIL, MATCH-EDIT, ATT-VOTE, ATT-SUMMARY, HOME-DASHBOARD(다음 경기) |
| F4 라인업 | P0 | LINEUP-VIEW, LINEUP-EDIT, MATCH-DETAIL(섹션) |
| F5 회비 | P1 | FEE-LIST, FEE-CREATE, FEE-DETAIL |
| F6 매칭 | P1 | MATCHING-HUB, MATCHING-CREATE, MATCHING-RECOMMEND, MATCHING-PROPOSAL, MATCHING-DETAIL, MATCHING-RESULT |
| F7 용병 | P1 | MERC-PROFILE, MERC-PROFILE-EDIT, MERC-RECRUIT, MERC-APPLICANTS |
| F8 구장 | P2 | GROUND-LIST, GROUND-DETAIL |
| F9 알림 | P0 | NOTIF-CENTER, MY-NOTIF + (모든 화면 헤더 🔔) |

모든 PRD F1~F9가 1+ 화면에 매핑됨 ✅
