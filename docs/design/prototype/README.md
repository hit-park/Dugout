# 디자인 프로토타입 — 로컬에서 보기 + Figma 임포트 가이드

## 보는 법

### 옵션 1 — 파일 직접 열기 (가장 빠름)

```bash
open /Users/heetae/Documents/Source/Dugout/docs/design/prototype/index.html
```

### 옵션 2 — 로컬 서버 (CDN 캐시 안정)

```bash
cd /Users/heetae/Documents/Source/Dugout/docs/design/prototype
python3 -m http.server 8080
# 브라우저에서 http://localhost:8080 열기
```

## 1차 포함 화면 (v0.1 Alpha)

`screens.md`의 17개 핵심 화면을 카드 그리드로 배치:

- **인증**: SPLASH · LOGIN · ONBOARDING · ONBOARDING-CHOOSE
- **홈**: HOME-EMPTY · HOME-DASHBOARD
- **팀**: TEAM-LIST · TEAM-CREATE · TEAM-JOIN · TEAM-DETAIL · TEAM-MEMBER-DIALOG
- **경기**: MATCH-LIST · MATCH-CREATE · MATCH-DETAIL
- **출석**: ATT-VOTE · ATT-SUMMARY
- **마이/알림**: MY-PROFILE · NOTIF-CENTER
- **상태 표준**: STATE-EMPTY · STATE-LOADING · STATE-FAILURE

v0.5 Beta+/v1.0 화면(라인업·회비·매칭·용병·구장)은 카드 자리만 두고 "TBD"로 표시. 후속 plan에서 실제 화면 카드로 교체.

## Figma 임포트 — html.to.design 플러그인

[html.to.design](https://html.to.design/)은 HTML/CSS를 Figma 노드로 자동 변환하는 플러그인.

### 임포트 절차

1. Figma 데스크톱 앱 또는 웹 → 새 디자인 파일 생성
2. 메뉴 → Plugins → "html.to.design" 검색 → 실행
3. 입력 방식 선택:
   - **(a) 로컬 서버 URL**: `python3 -m http.server 8080` 실행 후 `http://localhost:8080` 입력
   - **(b) HTML ZIP 업로드**: `prototype/` 폴더를 ZIP으로 압축해 업로드
   - **(c) 한 화면씩**: 카드 단위로 잘라 import (정밀도 ↑)
4. 임포트 완료 → 모든 카드가 Figma 프레임으로 변환됨

### 임포트 후 권장 작업

- **컴포넌트화**: 반복되는 카드(`dg-card`), 버튼(`dg-btn-primary`/`secondary`), 배지(`dg-badge`), 디바이스 프레임(`device`)을 컴포넌트로 변환
- **변수 연결**: Tokens Studio 플러그인으로 `tokens.md` JSON 임포트 → 색·타이포·간격을 Figma 변수로 연결
- **Mobile Frame 정렬**: iPhone 15 (390 × 844) 또는 13 mini (375 × 812) 프레임 기준으로 재배치
- **상태 분리**: 한 화면에 default / empty / loading / failure를 variants로 묶기

### 알려진 제약

- Tailwind CDN의 동적 스타일 일부는 inline style로 변환되지 않을 수 있음 → 임포트 후 수동 보정
- 자체 폰트(Pretendard)는 Figma에 별도 설치 필요 — Pretendard는 [공식 사이트](https://github.com/orioncactus/pretendard)에서 설치
- 다단계 nested element가 너무 깊으면 임포트가 느려질 수 있음 → 한 화면씩 나눠서 import 권장

## 새 화면 추가 가이드

`index.html`에서 화면 카드 추가는 다음 템플릿 사용:

```html
<article id="scr-{domain}-{role}" class="space-y-3">
  <header class="text-xs">
    <div class="flex items-center gap-2">
      <span class="font-mono font-semibold">SCR-XXX-YYY</span>
      <span class="dg-badge bg-primary-100 text-primary-700">P0</span>
      <!-- sheet/fullScreen/dialog 표현이면 추가 배지 -->
    </div>
    <h3 class="font-semibold mt-1">화면 제목 (한글)</h3>
  </header>

  <div class="device">
    <div class="device-screen">
      <div class="status-bar"><span>9:41</span><span>·····</span></div>
      <div class="nav-bar"><span class="title-inline">제목</span><span class="nav-icon-btn">🔔</span></div>
      <!-- 콘텐츠 -->
      <div class="px-5 space-y-3">
        ...
      </div>
      <!-- 필요 시 tab-bar -->
    </div>
  </div>

  <p class="text-xs text-cream-500">한 줄 설명</p>
</article>
```

색·간격·타이포는 `tailwind.config`에 등록된 토큰 사용 (`bg-primary-500`, `text-cream-700`, `dg-card`, `dg-btn-primary` 등). 임의 색·임의 px 값 사용 금지.

## 다음 단계

- v0.5 Beta 화면 추가 (`SCR-LINEUP-*`, `SCR-FEE-*`, `SCR-MY-NOTIF`) — 후속 plan
- v1.0 화면 추가 (`SCR-MATCHING-*`, `SCR-MERC-*`, `SCR-GROUND-*`) — 후속 plan
- Figma 임포트 완료 후 임포트된 Figma URL을 이 README에 링크
