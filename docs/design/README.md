# Dugout — 클라이언트 디자인 가이드

> Dugout 앱의 정보 구조(IA), 사용자 플로우, 화면 사양, 디자인 토큰을 한 곳에 모은 가이드. PRD/TDD를 진실 소스로 하여 클라이언트 개발자(iOS/Android)와 디자이너에게 전달하는 베이스라인이다.
>
> 이 가이드는 **현재 iOS 구현과 별개로** 작성되었다. 코드와 가이드가 어긋나면 PRD/TDD가 우선이며, 코드는 후속 정렬 plan에서 맞춘다.

---

## 산출물 한눈에

| 파일 | 역할 | 언제 참조 |
|------|------|----------|
| **[ia.md](ia.md)** | 탭 구조 / 네비게이션 컨벤션 / 권한 매트릭스 / 상태 표준 | 새 화면을 시작할 때마다 (가장 자주) |
| **[flows.md](flows.md)** | 9개 도메인의 Mermaid 사용자 플로우 | 새 도메인에 진입할 때 |
| **[screens.md](screens.md)** | 화면별 사양 (목적/요소/상태/데이터/권한/인터랙션/와이어 링크) | 화면 단위 코드 작업 시작 시 |
| **[tokens.md](tokens.md)** | 색·타이포·간격 (W3C Design Tokens, Figma Tokens Studio 호환) | 시각 디자인 작업 시 |
| **[prototype/index.html](prototype/index.html)** | Tailwind 모바일 시안 갤러리 (v0.1 Alpha 1차) | 디자이너 협업·시각 검토·Figma 임포트 |
| **[prototype/README.md](prototype/README.md)** | 프로토타입 보는 법 + Figma 임포트 가이드 | 프로토타입 처음 열 때 |

---

## 사용 시나리오

### A. 신규 iOS 개발자가 첫 화면을 구현할 때

1. `ia.md` 읽기 — 탭 구조와 네비게이션 컨벤션 숙지
2. `flows.md`에서 해당 도메인 플로우 확인
3. `screens.md`에서 해당 화면 사양 정독 (API endpoint, 권한, 인터랙션)
4. `prototype/index.html`을 브라우저로 열어 시각 확인
5. `tokens.md`에서 색·간격·타이포 토큰 확인
6. SwiftUI로 구현

### B. 디자이너가 Figma로 정교화할 때

1. `prototype/README.md`의 임포트 가이드대로 html.to.design 플러그인으로 임포트
2. Tokens Studio로 `tokens.md` JSON 임포트
3. 컴포넌트화·variants 분리·픽셀 정교화
4. 임포트한 Figma URL을 `prototype/README.md`에 추가

### C. 새 도메인이 추가될 때 (디자인 가이드 업데이트 순서)

```
ia.md (위계·권한 추가)
  ↓
flows.md (도메인 플로우 추가)
  ↓
screens.md (화면 사양 추가)
  ↓
tokens.md (필요 시 신규 토큰)
  ↓
prototype/index.html (시각 카드 추가)
```

이 순서를 지키면 하위 산출물이 상위 결정의 영향을 받는 동안 일관성이 유지된다.

---

## 범위 (v1.0까지)

`docs/PRD.md` 4-2 + 8장(릴리스)에 따라:

- **사양 (ia·flows·screens·tokens)**: v1.0까지 9개 영역(F1~F9) 전체
- **프로토타입 (시각)**: 1차로 v0.1 Alpha (인증·팀·경기·출석) 17화면 + 상태 표준 3화면. v0.5 Beta+ 화면은 자리표시(TBD) 후 후속 plan에서 채운다.

| 릴리스 | PRD 기능 | 프로토타입 |
|--------|---------|----------|
| v0.1 Alpha (M3) | F1·F2·F3 | ✅ 1차 포함 |
| v0.5 Beta (M5) | +F4·F5·F9 (라인업·회비·알림 인프라) | 🚧 자리표시만 |
| v1.0 정식 (M6) | +F6·F7·F8 (매칭·용병·구장) | 🚧 자리표시만 |

---

## 핵심 원칙

1. **PRD/TDD 우선** — 코드와 가이드가 어긋나면 PRD/TDD가 진실. 가이드는 PRD/TDD를 화면 단위로 풀어 쓴 것.
2. **단일 진실 소스** — 같은 정보가 여러 파일에 등장하지 않도록 cross-reference로 연결. 예: 권한 매트릭스는 `ia.md` §5에만, `screens.md`는 "권한 매트릭스 참조"라고만 적는다.
3. **5탭 + 헤더 종 알림** — iOS HIG 준수. PRD 4-1의 6영역은 콘텐츠 분류이지 탭이 아니다 (`ia.md` §1).
4. **Deferred Auth** — 비로그인 자유 탐색, 액션 시 로그인 트리거 (`ia.md` §7, `flows.md` §1).
5. **상태 4종은 항상** — Empty / Loading / Failure를 모든 데이터 화면에 (`ia.md` §6).
6. **다중 팀 컨텍스트 명시** — 한 사용자가 여러 팀 (`flows.md` §10).

---

## 변경 관리

- 도메인 결정이 바뀌면 가이드를 먼저 업데이트하고 코드 plan을 시작한다.
- 큰 IA 변경(탭 구조 등)은 PR 1개로 묶고, screens·prototype·tokens는 후속 PR로 분리해도 된다.
- 가이드 업데이트는 한국어 커밋 메시지: `docs(design): ...`

---

## 외부 도구

- [Tokens Studio for Figma](https://tokens.studio/) — `tokens.md` JSON 임포트
- [html.to.design](https://html.to.design/) — `prototype/` Figma 임포트
- [Pretendard](https://github.com/orioncactus/pretendard) — 한글 폰트
- Mermaid — `flows.md`는 GitHub/IDE에서 자동 렌더링
