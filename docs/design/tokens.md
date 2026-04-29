# Design Tokens — 색 · 타이포 · 간격

> **목적**: 디자인 토큰을 W3C Design Tokens 표준 JSON과 병행 표기해, Figma Tokens Studio 임포트와 SwiftUI 코드 양쪽에서 단일 진실 소스가 되게 한다.
>
> **스타일 컨셉**: 야구장(천연 잔디 그린) + 흙(클레이 오렌지) + 라이너(화이트). 진중하고 신뢰감 있는 팀 도구의 톤.

---

## 1. 색 (Color)

### 1-1. Primary — Field Green

야구장 천연 잔디를 모티프로 한 진녹. Brand의 첫인상.

| Step | Hex | 용도 |
|------|-----|------|
| 50 | `#E8F5EE` | 매우 옅은 배경 (선택 카드 배경) |
| 100 | `#C8E6D2` | 옅은 배경 (배지 BG) |
| 200 | `#9FD3B0` | hover/pressed light |
| 300 | `#73BE8B` | accent light |
| 400 | `#4CA86B` | accent |
| **500** | **`#2D8A4E`** | **primary 기준 (CTA, brand)** |
| 600 | `#236F40` | hover/pressed |
| 700 | `#1A5532` | dark theme primary |
| 800 | `#123E25` | text on light primary BG |
| 900 | `#0B2917` | extreme dark |

### 1-2. Secondary — Clay Orange

내야 흙 색. Primary 보완·강조 (예: 경기 D-Day, 알림 배지).

| Step | Hex | 용도 |
|------|-----|------|
| 50 | `#FDF1E8` | |
| 100 | `#FADBC4` | |
| 300 | `#F0B083` | |
| **500** | **`#D27640`** | **secondary 기준** |
| 700 | `#974D24` | |
| 900 | `#4A2510` | |

### 1-3. Neutral — Warm Gray

배경·텍스트·구분선. 차가운 회색이 아닌 약간 따뜻한 회색 (야구장 흙 톤과 어울림).

| Step | Hex | 용도 |
|------|-----|------|
| 0 | `#FFFFFF` | 표면(카드, sheet) |
| 50 | `#FAF7F4` | 페이지 배경 |
| 100 | `#F1ECE5` | 구분 배경 |
| 200 | `#E0DAD0` | divider, border |
| 300 | `#C2BBAF` | disabled border |
| 400 | `#9A9389` | placeholder |
| 500 | `#73706A` | secondary text |
| 700 | `#46443F` | primary text |
| 900 | `#1F1E1B` | heading |

### 1-4. Semantic

| 토큰 | Hex | 용도 |
|-----|-----|------|
| `success` | `#16A34A` | 출석 ATTEND, 납부 완료 |
| `warning` | `#D97706` | 경고 배너, 출석 LATE |
| `danger` | `#DC2626` | 추방, 삭제, 출석 ABSENT |
| `info` | `#2563EB` | 정보 뱃지 (AI 추천 표기 등) |

### 1-5. Status (출석 5상태 전용)

| 상태 | 색 | 아이콘 |
|------|----|--------|
| ATTEND (참가) | `success` `#16A34A` | ✓ |
| ABSENT (불참) | `danger` `#DC2626` | ✕ |
| MAYBE (미정) | `neutral.400` `#9A9389` | ? |
| LATE (늦참) | `warning` `#D97706` | ⏰ |
| EARLY_LEAVE (조퇴) | `secondary.500` `#D27640` | 🚪 |

### 1-6. W3C Design Tokens JSON (color)

```json
{
  "color": {
    "primary": {
      "50":  { "value": "#E8F5EE", "type": "color" },
      "100": { "value": "#C8E6D2", "type": "color" },
      "200": { "value": "#9FD3B0", "type": "color" },
      "300": { "value": "#73BE8B", "type": "color" },
      "400": { "value": "#4CA86B", "type": "color" },
      "500": { "value": "#2D8A4E", "type": "color" },
      "600": { "value": "#236F40", "type": "color" },
      "700": { "value": "#1A5532", "type": "color" },
      "800": { "value": "#123E25", "type": "color" },
      "900": { "value": "#0B2917", "type": "color" }
    },
    "secondary": {
      "50":  { "value": "#FDF1E8", "type": "color" },
      "100": { "value": "#FADBC4", "type": "color" },
      "300": { "value": "#F0B083", "type": "color" },
      "500": { "value": "#D27640", "type": "color" },
      "700": { "value": "#974D24", "type": "color" },
      "900": { "value": "#4A2510", "type": "color" }
    },
    "neutral": {
      "0":   { "value": "#FFFFFF", "type": "color" },
      "50":  { "value": "#FAF7F4", "type": "color" },
      "100": { "value": "#F1ECE5", "type": "color" },
      "200": { "value": "#E0DAD0", "type": "color" },
      "300": { "value": "#C2BBAF", "type": "color" },
      "400": { "value": "#9A9389", "type": "color" },
      "500": { "value": "#73706A", "type": "color" },
      "700": { "value": "#46443F", "type": "color" },
      "900": { "value": "#1F1E1B", "type": "color" }
    },
    "semantic": {
      "success": { "value": "#16A34A", "type": "color" },
      "warning": { "value": "#D97706", "type": "color" },
      "danger":  { "value": "#DC2626", "type": "color" },
      "info":    { "value": "#2563EB", "type": "color" }
    },
    "status": {
      "attend":      { "value": "{color.semantic.success}", "type": "color" },
      "absent":      { "value": "{color.semantic.danger}",  "type": "color" },
      "maybe":       { "value": "{color.neutral.400}",       "type": "color" },
      "late":        { "value": "{color.semantic.warning}",  "type": "color" },
      "early_leave": { "value": "{color.secondary.500}",     "type": "color" }
    }
  }
}
```

### 1-7. 의미 토큰 (semantic alias)

화면에서 직접 쓰는 이름. 컬러 step을 의미 이름으로 매핑.

| 의미 토큰 | 라이트 모드 | 다크 모드 (v0.5+) |
|----------|------------|------------------|
| `surface.background` | `neutral.50` | `neutral.900` |
| `surface.card` | `neutral.0` | `neutral.700` (어둡지 않게) |
| `text.primary` | `neutral.900` | `neutral.0` |
| `text.secondary` | `neutral.500` | `neutral.300` |
| `text.tertiary` | `neutral.400` | `neutral.400` |
| `text.onPrimary` | `neutral.0` | `neutral.0` |
| `border.default` | `neutral.200` | `neutral.700` |
| `border.strong` | `neutral.300` | `neutral.500` |
| `action.primary` | `primary.500` | `primary.400` |
| `action.primaryHover` | `primary.600` | `primary.500` |
| `action.danger` | `semantic.danger` | `semantic.danger` |

> 다크 모드는 v0.5+ 작업 항목. 토큰 구조만 미리 잡아둔다.

---

## 2. 타이포 (Typography)

### 2-1. 폰트 스택

| 플랫폼 | 본문 | 숫자(tabular) |
|--------|-----|--------------|
| iOS | SF Pro (system) | SF Pro Display (`monospacedDigit`) |
| Android | Pretendard (한글 가독성) | Roboto Mono (수치 표 정렬) |
| Web prototype | `system-ui, -apple-system, "Apple SD Gothic Neo", "Pretendard", sans-serif` | `"SF Mono", "Roboto Mono", monospace` |

### 2-2. 스케일

| 토큰 | 크기 | 라인 높이 | 굵기 | 용도 |
|------|-----|----------|------|-----|
| `display` | 32 | 40 | 700 | 큰 환영 문구 (온보딩) |
| `title1` | 28 | 36 | 700 | 큰 페이지 타이틀 (.large nav title) |
| `title2` | 22 | 28 | 600 | 페이지 타이틀 (.inline nav, 큰 카드 hero) |
| `title3` | 20 | 26 | 600 | 섹션 헤더, 카드 타이틀 |
| `headline` | 17 | 22 | 600 | 강조 본문 |
| `body` | 17 | 22 | 400 | 본문 기본 |
| `callout` | 16 | 21 | 400 | 짧은 본문 |
| `subheadline` | 15 | 20 | 400 | 부제 |
| `footnote` | 13 | 18 | 400 | 부가 정보 (시간·D-Day) |
| `caption` | 12 | 16 | 400 | 캡션, 배지 |

### 2-3. 숫자 표시 규칙

- 출석 카운트, 점수, 회비 금액 등은 `tabular figures` 사용 (동일 너비)
- `iOS`: `.monospacedDigit()` modifier
- `web`: `font-variant-numeric: tabular-nums;`

### 2-4. W3C Design Tokens JSON (typography)

```json
{
  "typography": {
    "display":     { "value": { "fontSize": 32, "lineHeight": 40, "fontWeight": 700 }, "type": "typography" },
    "title1":      { "value": { "fontSize": 28, "lineHeight": 36, "fontWeight": 700 }, "type": "typography" },
    "title2":      { "value": { "fontSize": 22, "lineHeight": 28, "fontWeight": 600 }, "type": "typography" },
    "title3":      { "value": { "fontSize": 20, "lineHeight": 26, "fontWeight": 600 }, "type": "typography" },
    "headline":    { "value": { "fontSize": 17, "lineHeight": 22, "fontWeight": 600 }, "type": "typography" },
    "body":        { "value": { "fontSize": 17, "lineHeight": 22, "fontWeight": 400 }, "type": "typography" },
    "callout":     { "value": { "fontSize": 16, "lineHeight": 21, "fontWeight": 400 }, "type": "typography" },
    "subheadline": { "value": { "fontSize": 15, "lineHeight": 20, "fontWeight": 400 }, "type": "typography" },
    "footnote":    { "value": { "fontSize": 13, "lineHeight": 18, "fontWeight": 400 }, "type": "typography" },
    "caption":     { "value": { "fontSize": 12, "lineHeight": 16, "fontWeight": 400 }, "type": "typography" }
  }
}
```

---

## 3. 간격 (Spacing)

8-base 시스템.

| 토큰 | 값 (pt) | 용도 |
|-----|--------|------|
| `xxs` | 2 | 미세 (배지 내부) |
| `xs` | 4 | 아이콘 ↔ 텍스트 |
| `sm` | 8 | 카드 내부, 섹션 내 row |
| `md` | 12 | 카드 패딩, 작은 섹션 간 |
| `lg` | 16 | 페이지 좌우 여백 (기본) |
| `xl` | 24 | 섹션 간 |
| `xxl` | 32 | 큰 섹션 분리 |
| `xxxl` | 48 | hero 영역 상하 |

```json
{
  "spacing": {
    "xxs": { "value": 2,  "type": "dimension" },
    "xs":  { "value": 4,  "type": "dimension" },
    "sm":  { "value": 8,  "type": "dimension" },
    "md":  { "value": 12, "type": "dimension" },
    "lg":  { "value": 16, "type": "dimension" },
    "xl":  { "value": 24, "type": "dimension" },
    "xxl": { "value": 32, "type": "dimension" },
    "xxxl":{ "value": 48, "type": "dimension" }
  }
}
```

---

## 4. 모서리 (Radius)

| 토큰 | 값 (pt) | 용도 |
|-----|--------|------|
| `none` | 0 | flat |
| `sm` | 6 | 배지, 작은 칩 |
| `md` | 10 | 입력 필드, 작은 카드 |
| `lg` | 14 | 카드 기본 (DGCard) |
| `xl` | 20 | 큰 모달, sheet 상단 |
| `pill` | 9999 | 라운드 버튼·배지 |

---

## 5. 그림자 (Elevation)

| 토큰 | 값 | 용도 |
|------|----|------|
| `none` | (없음) | flat |
| `sm` | `0 1px 2px rgba(0,0,0,0.04)` | 카드 기본 |
| `md` | `0 4px 8px rgba(0,0,0,0.06)` | hover, sheet |
| `lg` | `0 8px 24px rgba(0,0,0,0.10)` | 모달, popover |

iOS는 그림자보다 border 또는 배경 색차로 분리 (HIG에 가까운 톤). 그림자는 보조.

---

## 6. iOS DGColor / DGFont / DGSpacing 매핑

기존 코드의 토큰 자산 (`Core/DesignSystem/Sources/Theme.swift`)과 매핑. 신규 토큰은 추가 권고.

| 디자인 토큰 | DGColor (있으면) | 비고 |
|------------|----------------|------|
| `color.surface.background` | `DGColor.background` | 기존 매핑 |
| `color.surface.card` | (신설 권고) `DGColor.surface` | 카드 배경 |
| `color.text.primary` | `DGColor.textPrimary` | |
| `color.text.secondary` | `DGColor.textSecondary` | |
| `color.action.primary` | `DGColor.primary` | |
| `color.semantic.warning` | `DGColor.warning` | |
| `color.semantic.danger` | (신설) `DGColor.danger` | |
| `color.status.attend` | (신설) `DGColor.attendAttend` 등 | |

| 타이포 토큰 | DGFont |
|-----------|--------|
| `title1` | `DGFont.title` (확인 필요) |
| `title3` | `DGFont.title3` |
| `headline` | (신설) `DGFont.headline` |
| `body` | `DGFont.body` |
| `callout` | `DGFont.callout` |
| `footnote` | `DGFont.footnote` |
| `caption` | `DGFont.caption` |

| 간격 토큰 | DGSpacing |
|----------|-----------|
| `xs` (4) | `DGSpacing.xs` |
| `sm` (8) | `DGSpacing.sm` |
| `md` (12) | `DGSpacing.md` |
| `lg` (16) | `DGSpacing.lg` |
| `xl` (24) | (신설) `DGSpacing.xl` |

> **권고**: SwiftUI 코드 plan 시작 시, 위 신설 토큰을 일괄 추가하는 작은 plan (DesignSystem 갱신)을 먼저 분리 처리.

---

## 7. Figma Tokens Studio 임포트 가이드

[Tokens Studio (구 Figma Tokens) 플러그인](https://tokens.studio/) 활용:

1. Figma 파일 열기 → 메뉴 → Plugins → Tokens Studio for Figma
2. 사이드바 → Settings (⚙) → JSON → "Import"
3. 위 JSON(섹션 1-6, 2-4, 3) 합쳐서 붙여넣기 또는 파일 업로드
4. Apply to selection / Apply globally 선택
5. Figma의 색상·타이포·간격 변수가 자동 생성됨

**프로젝트 구조 권장**:
- `core` token set (위 모든 토큰)
- `light` token set (의미 alias)
- `dark` token set (v0.5+)
- Themes로 light/dark 토글

**SwiftUI와 동기화**:
- Tokens Studio → GitHub sync 기능 활용
- 또는 별도 빌드 스크립트로 JSON → Swift 코드 생성 (style-dictionary 등). 후속 plan에서 검토.

---

## 8. 사용 가이드 (한 줄 요약)

- 색은 의미 토큰을 1순위로 사용 (`text.primary`), 직접 step(`primary.500`)은 컴포넌트 내부에서만
- 타이포는 항상 토큰 (절대 `Font.system(size: 18)` 직접 X)
- 간격은 8의 배수 외 사용 금지 (`5px`, `7px` 같은 비대칭 값은 디자인 회의 사유)
- 컴포넌트는 1차로 `DG*` 활용, 신설은 토큰 통과 후 등록
