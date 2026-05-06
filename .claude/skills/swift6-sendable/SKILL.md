---
name: swift6-sendable
description: Use when writing or reviewing Swift code in dugout-ios — enforces Swift 6 Strict Concurrency rules (Sendable, @MainActor, actor isolation) and prevents the most common violations. Activate any time you create or modify .swift files in this project.
---

# Swift 6 Strict Concurrency 가이드 (Dugout iOS)

`Project.swift`에서 모든 타겟이 `SWIFT_STRICT_CONCURRENCY=complete` 로 설정됨.
위반 시 빌드 에러로 차단된다. 이 스킬은 자주 발생하는 위반을 사전에 막는다.

---

## 1. 타입 종류별 격리 규칙 (반드시 따를 것)

| 타입 | 격리 규칙 |
|------|-----------|
| 도메인 Entity | `struct` + `Sendable` |
| DTO | `struct` + `Codable, Sendable` |
| Repository **프로토콜** | `protocol Foo: Sendable` |
| Repository **구현체** | `final class` (모든 프로퍼티 `let`) + `Sendable`, 또는 `actor` |
| ViewModel | `@MainActor final class FooViewModel: ObservableObject` |
| Service / UseCase | `actor` 또는 `Sendable final class` |
| Logger / 공유 가변 상태 | `actor` |

## 2. async 경계 규칙

- async 함수 **인자·반환·throws** 모든 타입은 Sendable
- `Task { ... }` 캡처는 `[weak self]` + 캡처값 모두 Sendable
- 백그라운드 → UI 갱신: `Task { @MainActor in ... }` 또는 `await MainActor.run { ... }`
- Repository는 `any FooRepository` 형태로 ViewModel에 주입 (프로토콜이 Sendable이어야 함)

## 3. 절대 금지

- `class` + `var` 프로퍼티를 ViewModel/Repository에 사용 (`actor`로 바꾸거나 `let`만)
- `@Published var` 를 nonisolated 컨텍스트에서 직접 갱신
- `DispatchQueue.main.async { ... }` (Swift 6에서는 `MainActor.run` 사용)
- `try!` / `try?` 사용 (테스트 코드 외) — 에러 핸들링 의무

---

## 위반 패턴 → 수정 패턴

### 위반 1: class 도메인 모델

```swift
// ❌
class TeamMember {
    var name: String
    init(name: String) { self.name = name }
}

// ✅
struct TeamMember: Sendable, Identifiable, Hashable {
    let id: Int64
    let name: String
}
```

### 위반 2: ViewModel에서 백그라운드 → @Published 직접 갱신

```swift
// ❌
final class TeamDetailViewModel: ObservableObject {
    @Published var team: Team?
    private let repo: TeamRepository

    func load() {
        Task {
            self.team = try? await repo.fetch(id: 1)   // ⚠️ MainActor 격리 위반
        }
    }
}

// ✅
@MainActor
final class TeamDetailViewModel: ObservableObject {
    @Published private(set) var team: Team?
    @Published var errorMessage: String?

    private let repo: any TeamRepository  // 프로토콜이 Sendable

    init(repo: any TeamRepository) {
        self.repo = repo
    }

    func load(id: Int64) async {
        do {
            team = try await repo.fetch(id: id)
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
```

### 위반 3: 비-Sendable 클로저 캡처

```swift
// ❌
class Logger {                       // class + 비-Sendable 멤버
    var buffer: [String] = []
    func log(_ msg: String) { buffer.append(msg) }
}

let logger = Logger()
Task {
    logger.log("done")               // ⚠️ Sendable 위반 + race
}

// ✅
actor Logger {
    private var buffer: [String] = []
    func log(_ msg: String) { buffer.append(msg) }
}

let logger = Logger()
Task {
    await logger.log("done")
}
```

### 위반 4: Repository 구현체에 var 프로퍼티

```swift
// ❌
final class TeamRepositoryImpl: TeamRepository {
    var cache: [Int64: Team] = [:]   // var → Sendable 위반
}

// ✅ (옵션 A: 캐시를 actor로 격리)
actor TeamRepositoryImpl: TeamRepository {
    private var cache: [Int64: Team] = [:]

    func fetch(id: Int64) async throws -> Team {
        if let hit = cache[id] { return hit }
        let team = try await network.fetchTeam(id: id)
        cache[id] = team
        return team
    }
}

// ✅ (옵션 B: 캐시 제거, immutable로)
final class TeamRepositoryImpl: TeamRepository, Sendable {
    private let network: any NetworkClient

    init(network: any NetworkClient) {
        self.network = network
    }

    func fetch(id: Int64) async throws -> Team {
        try await network.fetchTeam(id: id)
    }
}
```

### 위반 5: 백그라운드 작업 후 UI 갱신

```swift
// ❌
@MainActor
final class HomeViewModel: ObservableObject {
    @Published var items: [Item] = []

    func refresh() {
        Task.detached {                       // detached → nonisolated
            let result = try await loadItems()
            self.items = result               // ⚠️ MainActor 격리 위반
        }
    }
}

// ✅
@MainActor
final class HomeViewModel: ObservableObject {
    @Published private(set) var items: [Item] = []
    @Published var errorMessage: String?

    func refresh() async {                    // 호출자가 MainActor면 그대로 OK
        do {
            items = try await loadItems()
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
```

---

## 작성 템플릿

### 도메인 Entity

```swift
import Foundation

public struct Team: Sendable, Identifiable, Hashable {
    public let id: Int64
    public let name: String
    public let division: TeamDivision
    public let createdAt: Date

    public init(id: Int64, name: String, division: TeamDivision, createdAt: Date) {
        self.id = id
        self.name = name
        self.division = division
        self.createdAt = createdAt
    }
}

public enum TeamDivision: String, Sendable, CaseIterable, Codable {
    case d1, d2, d3, d4
}
```

### Repository 프로토콜

```swift
public protocol TeamRepository: Sendable {
    func fetchAll() async throws -> [Team]
    func fetch(id: Int64) async throws -> Team
}
```

### ViewModel

```swift
import Foundation
import SwiftUI

@MainActor
public final class TeamListViewModel: ObservableObject {
    @Published public private(set) var teams: [Team] = []
    @Published public private(set) var isLoading = false
    @Published public var errorMessage: String?

    private let repository: any TeamRepository

    public init(repository: any TeamRepository) {
        self.repository = repository
    }

    public func load() async {
        isLoading = true
        defer { isLoading = false }
        do {
            teams = try await repository.fetchAll()
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
```

---

## 사전 검증 명령

코드 변경 후 항상 빌드해서 Sendable / concurrency 경고가 없는지 확인:

```bash
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout \
  -configuration Debug \
  -destination 'generic/platform=iOS Simulator' \
  -quiet build 2>&1 \
  | grep -iE "warning:|error:" \
  | grep -iE "sendable|concurrency|isolation|actor|mainactor"
```

위 출력이 **비어 있으면 통과**. 한 줄이라도 나오면 위반 — 위 패턴 가이드로 수정 후 재빌드.

---

## PR 체크리스트

- [ ] 새 타입은 위 "타입별 격리 규칙" 표를 따랐는가?
- [ ] async 함수 시그니처의 모든 타입이 Sendable인가?
- [ ] ViewModel은 `@MainActor + final class + ObservableObject` 인가?
- [ ] 백그라운드 → UI 갱신 시 MainActor 진입을 명시했는가?
- [ ] 빌드 시 Sendable / concurrency 경고 0개?
- [ ] `try!` / `try?` 무시 사용처가 없는가? (에러 핸들링 누락)

위 모두 ✅ 일 때만 머지 후보.
