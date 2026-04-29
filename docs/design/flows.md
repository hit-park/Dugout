# Flows — 도메인별 사용자 플로우

> **목적**: PRD 4-2의 9개 영역(F1~F9)을 사용자 시점의 플로우로 시각화. 화면 사양(`screens.md`)과 프로토타입(`prototype/`)이 어떤 사용자 여정을 구현하는지 한눈에 보이게 한다.
>
> **범례**:
> - `SCR-XXX` = `screens.md`의 화면 ID (cross-reference)
> - sequence diagram = 시간축 + 행위자 간 상호작용 (서버 호출이 핵심인 경우)
> - flowchart = 의사결정 분기 (사용자 선택지가 핵심인 경우)

---

## 1. 인증 + Deferred Auth (F1)

### 1-1. 첫 진입 + Deferred Auth

```mermaid
flowchart TD
    Start([앱 실행]) --> Splash[SCR-AUTH-SPLASH]
    Splash --> Check{토큰 있음?}
    Check -->|아니오| Tab[탭 진입<br/>비로그인 상태]
    Check -->|예| FetchMe[GET /users/me]
    FetchMe -->|성공| Tab2[탭 진입<br/>로그인 상태]
    FetchMe -->|401| Clear[토큰 정리] --> Tab
    FetchMe -->|타임아웃/네트워크| Tab

    Tab --> Browse[탭 자유 탐색<br/>홈/일정/매칭/팀/마이]
    Browse --> ActionAttempt{액션 시도}
    ActionAttempt -->|조회| Continue1[즉시 진행]
    ActionAttempt -->|쓰기/내 데이터| LoginSheet[SCR-AUTH-LOGIN sheet]
    LoginSheet --> Provider{OAuth 선택}
    Provider --> Kakao[카카오]
    Provider --> Naver[네이버]
    Provider --> Apple[Apple]
    Provider --> Google[Google]
    Kakao & Naver & Apple & Google --> OAuth[OAuth 진행<br/>fullScreenCover]
    OAuth -->|성공| Pending{pending action?}
    OAuth -->|실패| Failure[에러 표시 + sheet 유지]
    Pending -->|있음| ResumeAction[원래 액션 자동 재시도]
    Pending -->|없음| DismissOnly[sheet dismiss]
    ResumeAction --> Tab2
    DismissOnly --> Tab2
```

### 1-2. 온보딩 (최초 가입)

```mermaid
sequenceDiagram
    participant U as 사용자
    participant S as 서버
    participant App as 앱

    U->>App: OAuth 로그인 성공
    App->>S: POST /api/v1/auth/login (provider, token)
    S-->>App: { is_new_user: true, jwt }
    App->>U: SCR-AUTH-ONBOARDING (fullScreenCover)
    U->>App: 닉네임 / 프로필 사진 입력
    U->>App: 포지션 / 타석 / 투타 선택
    App->>S: PUT /users/me (profile)
    S-->>App: 200 OK
    App->>U: SCR-AUTH-ONBOARDING-CHOOSE
    U->>App: "팀 만들기" / "팀 참가" / "용병으로 시작" 선택
    App->>U: 해당 화면 push (또는 홈 진입 + 첫 사용 가이드)
```

### 1-3. 로그아웃

```mermaid
flowchart LR
    My[SCR-MY-PROFILE] --> Logout[로그아웃 버튼]
    Logout --> Confirm{확인 alert}
    Confirm -->|취소| My
    Confirm -->|확인| ClearLocal[로컬 토큰 삭제]
    ClearLocal --> ServerLogout[POST /auth/logout<br/>fire-and-forget]
    ClearLocal --> ResetNav[NavigationStack 재생성<br/>id 변경]
    ResetNav --> Tab[비로그인 탭 root]
```

---

## 2. 팀 (F2)

### 2-1. 팀 생성

```mermaid
flowchart TD
    Entry[팀 탭 / 홈 빈 상태] --> Tap["팀 만들기"]
    Tap --> Auth{로그인됨?}
    Auth -->|아니오| Login[로그인 sheet] --> Auth
    Auth -->|예| Sheet[SCR-TEAM-CREATE sheet]
    Sheet --> Fill[팀명·로고·지역·부수<br/>활동요일·시간·라인업모드 입력]
    Fill --> Submit{유효성}
    Submit -->|실패| InlineError[inline 에러 표시] --> Fill
    Submit -->|성공| API[POST /api/v1/teams]
    API -->|성공| Toast[토스트: 팀 생성 완료]
    Toast --> Dismiss[sheet dismiss]
    Dismiss --> Detail[SCR-TEAM-DETAIL push<br/>생성자 = CAPTAIN]
    API -->|실패| Banner[Form 상단 banner] --> Fill
```

### 2-2. 팀 가입 (초대 코드)

```mermaid
sequenceDiagram
    participant Cap as 주장
    participant Mem as 멤버
    participant App
    participant S as 서버

    Cap->>App: 팀 상세 → "초대 코드 보기"
    App->>S: POST /teams/{id}/invite
    S-->>App: { invite_code: "ABC123" }
    Cap->>App: 코드 복사 또는 QR 표시
    Cap->>Mem: 카톡/밴드로 코드 공유

    Mem->>App: 팀 탭 → "코드로 참여"
    App->>Mem: SCR-TEAM-JOIN sheet
    Mem->>App: 코드 입력
    App->>S: POST /teams/join { code }
    S-->>App: { team, member }
    App->>Mem: SCR-TEAM-DETAIL push
```

### 2-3. 멤버 관리 (역할 변경 / 추방)

```mermaid
flowchart LR
    TD[SCR-TEAM-DETAIL] --> Members[멤버 목록]
    Members --> TapMember{멤버 탭}
    TapMember -->|자기 자신| NoAction[비활성]
    TapMember -->|CAPTAIN| NoAction
    TapMember -->|일반| Dialog[ConfirmationDialog]
    Dialog --> ChangeRole[매니저/회계/일반으로 변경]
    Dialog --> Kick[추방]
    Dialog --> Cancel[취소]
    ChangeRole --> ApiR[PUT /teams/X/members/Y] --> Reload[팀 상세 reload]
    Kick --> ApiK[DELETE /teams/X/members/Y] --> Reload
```

---

## 3. 경기 일정 (F3 — 일정 부분)

### 3-1. 경기 등록

```mermaid
flowchart TD
    Entry[일정 탭 + 또는<br/>팀 상세 → 경기 등록] --> Auth{권한}
    Auth -->|MEMBER| Hidden[버튼 숨김]
    Auth -->|CAPTAIN/MANAGER| Sheet[SCR-MATCH-CREATE sheet]
    Sheet --> Form[날짜/경기시간/집합시간<br/>구장 선택/직접입력<br/>상대팀(선택)<br/>투표마감시간(선택)<br/>메모]
    Form --> Submit
    Submit --> API[POST /teams/X/matches]
    API -->|성공| Toast[경기 등록 완료]
    Toast --> Dismiss[dismiss]
    Dismiss --> Reload[일정 캘린더 reload]
    Note1[멤버 알림 발송<br/>v0.5+ FCM/카카오] -.-> API
```

### 3-2. 경기 수정·취소

```mermaid
flowchart LR
    Detail[SCR-MATCH-DETAIL] --> Header[툴바 ⋯]
    Header --> Dialog[ConfirmationDialog]
    Dialog --> Edit[수정]
    Dialog --> Cancel[취소]
    Edit --> Sheet[SCR-MATCH-EDIT sheet] --> ApiE[PUT /matches/X] --> Reload
    Cancel --> Confirm{alert: 정말 취소?} --> ApiD[DELETE /matches/X] --> Reload[일정 reload]
```

---

## 4. 출석 투표 (F3 — 출석 부분)

### 4-1. 멤버 시점 (투표하기)

```mermaid
sequenceDiagram
    participant M as 멤버
    participant App
    participant S as 서버

    Note over M: 트리거 ① 알림 ② 캘린더 ③ 홈 카드
    M->>App: 경기 카드 탭
    App->>S: GET /matches/X
    S-->>App: MatchResponse
    App->>S: GET /matches/X/attendance
    S-->>App: AttendanceSummaryResponse
    App->>M: SCR-MATCH-DETAIL (내 응답 상태 표시)

    M->>App: "출석 투표" 버튼
    App->>M: SCR-ATT-VOTE sheet
    M->>App: 참가/불참/미정/늦참/조퇴 선택 + 사유(선택)
    M->>App: 제출
    App->>S: POST /matches/X/attendance
    S-->>App: AttendanceResponse
    App->>M: 토스트 "응답 완료" + sheet dismiss
    App->>S: GET /matches/X/attendance (재조회)
    App->>M: SCR-MATCH-DETAIL 갱신
```

### 4-2. 주장 시점 (현황 확인)

```mermaid
flowchart TD
    Detail[SCR-MATCH-DETAIL] --> Section[출석 현황 섹션]
    Section --> Counts[참가 N / 불참 N / 미정 N<br/>늦참 N / 조퇴 N / 미응답 N]
    Section --> Predict{AI 출석 예측<br/>v0.5+}
    Predict -->|있음| Show["예상 14~16명<br/>김OO 90% 참가 예상"]
    Section --> Tap["전체 보기"]
    Tap --> Summary[SCR-ATT-SUMMARY push]
    Summary --> List[멤버별 응답 + 사유 + 시간]
    List --> Risk{예상 9명 미만?}
    Risk -->|예| Banner[경고 배너<br/>"용병 모집 추천"]
    Banner --> RecruitBtn[용병 모집 버튼<br/>v1.0+]
```

---

## 5. 라인업 (F4 — v0.5 Beta)

```mermaid
flowchart TD
    Detail[SCR-MATCH-DETAIL] --> Section[라인업 섹션]
    Section --> Status{라인업 상태}
    Status -->|미생성| Empty[빈 상태 + "AI 추천" 버튼]
    Status -->|초안| Draft[현재 라인업 미리보기 + "편집"]
    Status -->|확정| Confirmed[라인업 카드 이미지]

    Empty --> RecBtn[AI 추천 버튼]
    RecBtn --> ApiR[POST /matches/X/lineup/recommend]
    ApiR -->|3초 이내| Result[추천 결과]
    Result --> Edit[SCR-LINEUP-EDIT push]

    Edit --> Diamond[야구장 다이아몬드 뷰]
    Diamond --> DragDrop[드래그&드롭으로<br/>포지션·타순 수정]
    Edit --> Save[저장] --> ApiS[POST/PUT /matches/X/lineup]
    Edit --> Confirm[확정]
    Confirm --> Alert{alert: 확정 시 멤버에게<br/>알림 발송됨}
    Alert -->|확인| ApiC[POST /matches/X/lineup/confirm]
    ApiC --> CardGen[카드 이미지 생성<br/>GET /lineup/card]
    CardGen --> Notify[전 멤버 푸시 + 알림톡]
    Notify --> Share["카카오톡 공유" 액션]
```

---

## 6. 회비 (F5 — v0.5 Beta)

### 6-1. 회비 생성·납부

```mermaid
flowchart TD
    TD[SCR-TEAM-DETAIL] --> FeeSection[회비 진입]
    FeeSection --> List[SCR-FEE-LIST]
    List --> Auth{권한}
    Auth -->|CAPTAIN/MANAGER/ACCOUNTANT| Plus["+ 회비 생성"]
    Auth -->|MEMBER| ViewOnly[조회만]

    Plus --> Sheet[SCR-FEE-CREATE sheet]
    Sheet --> Form[정기/경기별 선택<br/>금액·기한·대상]
    Form --> ApiC[POST /teams/X/fees] --> Reload[회비 목록 reload]

    List --> TapFee[회비 항목 탭]
    TapFee --> Detail2[SCR-FEE-DETAIL push]
    Detail2 --> MemberStatus[멤버별 납부 현황]
    MemberStatus --> ManualMark{권한 확인}
    ManualMark -->|회계+| Mark[수동 납부 처리<br/>POST /fees/X/payments/Y]
```

### 6-2. 미납 리마인더 (자동)

```mermaid
sequenceDiagram
    participant Sched as 스케줄러
    participant S as 서버
    participant U as 미납 멤버

    Note over Sched: 기한 후 3일·7일
    Sched->>S: 미납자 조회
    S-->>Sched: 미납 멤버 리스트
    Sched->>U: FCM 푸시 + 카카오 알림톡
    U->>U: 알림 탭 → 앱 진입 (딥링크)
    U->>S: SCR-FEE-DETAIL 진입
    Note over U: 본인 납부 표시는 자동(외부 결제) <br/>또는 회계가 수동 처리
```

---

## 7. 팀 매칭 (F6 — v1.0)

```mermaid
sequenceDiagram
    participant CapA as 주장 A
    participant App
    participant S as 서버
    participant CapB as 주장 B
    participant AI

    CapA->>App: 매칭 탭 → "요청 등록"
    App->>CapA: SCR-MATCHING-CREATE sheet
    CapA->>App: 날짜·지역·부수·홈/원정 입력
    App->>S: POST /matching/requests
    S-->>App: { request_id }
    App->>S: GET /matching/requests/{id}/recommend
    S->>AI: 매칭 스코어 계산<br/>(실력 40% + 거리 25% + 시간 20% + 매너 15%)
    AI-->>S: Top 5 팀
    S-->>App: 추천 리스트
    App->>CapA: SCR-MATCHING-RECOMMEND push

    CapA->>App: 팀 B 선택 → "제안하기"
    App->>S: POST /matching/requests/{id}/propose/{teamB}
    S->>CapB: 푸시 + 알림톡 "매칭 제안 도착"

    CapB->>App: 알림 탭 → SCR-MATCHING-PROPOSAL
    CapB->>App: 수락 / 거절
    App->>S: POST /matching/requests/{id}/accept (or reject)

    alt 수락
        S-->>App: 매칭 성사
        S->>CapA: 푸시 + 알림톡
        S->>CapB: 양 팀 멤버에게 알림
        Note over App: 인앱 채팅으로 세부 조율
    else 거절
        S->>CapA: 푸시 "거절됨, 다음 추천"
    end

    Note over App: 경기 후
    CapA->>App: SCR-MATCHING-RESULT
    CapA->>App: 스코어 입력 + 매너 평가
    CapB->>App: (대칭) 스코어 입력 + 평가
    App->>S: POST /matching/results/{id}
    S-->>S: ELO 레이팅 자동 업데이트
```

---

## 8. 용병 매칭 (F7 — v1.0)

### 8-1. 팀 → 용병 모집

```mermaid
flowchart TD
    Match[SCR-MATCH-DETAIL] --> Risk[예상 9명 미만 경고]
    Risk --> Recruit["용병 모집"]
    Recruit --> Sheet[SCR-MERC-RECRUIT sheet]
    Sheet --> Fill[필요 포지션·실력·참가비]
    Fill --> Api[POST /mercenary/requests]
    Api --> AIRec[AI 추천<br/>GET /requests/X/recommend]
    AIRec --> List[추천 용병 리스트]
    List --> Select[용병 선택 → 알림 발송]
```

### 8-2. 용병 → 지원

```mermaid
sequenceDiagram
    participant Merc as 용병
    participant App
    participant S as 서버
    participant CapA as 주장

    Note over S: 새 모집 등록 시 매칭 알고리즘 작동
    S->>Merc: 알림톡 "조건 맞는 경기 등록"
    Merc->>App: SCR-MATCHING-MERC → 모집 상세
    Merc->>App: "지원하기"
    App->>S: POST /mercenary/requests/{id}/apply
    S->>CapA: 푸시 "용병 지원 도착"

    CapA->>App: SCR-MERC-APPLICANTS
    CapA->>App: 수락 (1명)
    App->>S: POST /mercenary/requests/{id}/accept/{userId}
    S->>Merc: 푸시 "수락됨!"

    Note over App: 경기 후 양쪽 평가
    CapA->>App: 용병 평가 (실력/매너/시간)
    Merc->>App: 팀 평가
    App->>S: POST /mercenary/reviews
```

---

## 9. 알림 진입 (F9 — 횡단)

```mermaid
flowchart TD
    Channel{알림 채널} --> Push[FCM 푸시]
    Channel --> Talk[카카오 알림톡]
    Channel --> Email[이메일]

    Push --> Tap[알림 탭]
    Talk --> Link[딥링크 탭]

    Tap & Link --> Auth{앱 설치 + 로그인}
    Auth -->|미설치| Store[App Store / Play Store]
    Auth -->|미로그인| LoginSheet[로그인 sheet] --> Auth
    Auth -->|OK| Route[딥링크 라우팅]

    Route --> NewMatch["새 경기"<br/>→ SCR-MATCH-DETAIL]
    Route --> AttendRemind["출석 리마인드"<br/>→ SCR-ATT-VOTE]
    Route --> LineupConfirmed["라인업 확정"<br/>→ SCR-LINEUP-VIEW]
    Route --> FeeBill["회비 청구"<br/>→ SCR-FEE-DETAIL]
    Route --> MatchProposal["매칭 제안"<br/>→ SCR-MATCHING-PROPOSAL]
    Route --> MercRecommend["용병 추천"<br/>→ SCR-MATCHING-MERC"]

    InApp[앱 내 헤더 🔔] --> Center[SCR-NOTIF-CENTER sheet]
    Center --> Tap2[알림 항목 탭] --> Route
```

**알림 채널별 발송 매트릭스 (PRD 4-2 F9-2 그대로):**

| 알림 유형 | 시점 | 푸시 | 알림톡 | 딥링크 → |
|---------|------|:-:|:-:|----------|
| 새 경기 일정 | 등록 즉시 | ✅ | ✅ | SCR-MATCH-DETAIL |
| 출석 리마인드 | 48h, 24h 전 | ✅ | ✅ (24h) | SCR-ATT-VOTE |
| 라인업 확정 | 주장 확정 시 | ✅ | ✅ | SCR-LINEUP-VIEW |
| 라인업 변경 | 변경 시 | ✅ | ❌ | SCR-LINEUP-VIEW |
| 회비 청구 | 청구일 | ✅ | ✅ | SCR-FEE-DETAIL |
| 회비 미납 리마인드 | 기한 후 3·7일 | ✅ | ✅ | SCR-FEE-DETAIL |
| 매칭 요청 수신 | 요청 시 | ✅ | ✅ | SCR-MATCHING-PROPOSAL |
| 매칭 성사 | 성사 시 | ✅ | ✅ | SCR-MATCHING-DETAIL |
| 용병 추천 | 조건 맞는 경기 | ✅ | ❌ | SCR-MATCHING-MERC |

---

## 10. 횡단 — 한 사용자, 여러 팀

PRD F1-6: "여러 팀 소속 지원". 다음 화면들은 "팀 컨텍스트" 명시가 필수:

```mermaid
flowchart LR
    Schedule[일정 탭] --> Filter{팀 필터}
    Filter --> All[전체 팀 통합 보기]
    Filter --> Per[팀별 보기<br/>세그먼트]

    Notif[알림 센터] --> Group[팀별 그룹핑 + 헤더]

    Home[홈] --> Carousel[팀 카드 carousel<br/>또는 한 화면당 한 팀]
```

- 일정 탭은 기본 "전체 통합", 필터로 팀별 전환
- 알림은 팀별 그룹헤더 (예: "수원 라이거스 — 출석 리마인드 3건")
- 팀 컨텍스트가 핵심인 화면(라인업·회비·멤버)은 navigation bar에 팀명 노출 (`title="라이거스 · 라인업"` 형태)
