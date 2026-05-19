import Foundation
import Observation

@MainActor
@Observable
public final class OnboardingViewModel {
    public enum NicknameStatus: Sendable {
        case idle
        case checking
        case available
        case unavailable(String)
        case error(String)
    }

    public enum SaveState: Sendable {
        case idle
        case saving
        case failed(String)
    }

    // MARK: - Nickname step
    public var nickname: String = ""
    public var jerseyNumber: String = ""
    public var nicknameStatus: NicknameStatus = .idle
    public var saveState: SaveState = .idle

    // MARK: - Position step
    public var selectedMainPosition: BaseballPosition? = nil
    public var selectedSubPositions: Set<BaseballPosition> = []

    // MARK: - Private
    private let repository: any AuthRepository
    private var nicknameCheckTask: Task<Void, Never>?

    public init(repository: any AuthRepository = AuthRepositoryImpl()) {
        self.repository = repository
    }

    // MARK: - Nickname

    public var isNicknameValid: Bool {
        if case .available = nicknameStatus { return true }
        return false
    }

    public func onNicknameChanged(_ value: String) {
        nicknameCheckTask?.cancel()
        nicknameStatus = value.isEmpty ? .idle : .checking

        nicknameCheckTask = Task {
            try? await Task.sleep(for: .milliseconds(600))
            guard !Task.isCancelled, !value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
            await checkNickname(value.trimmingCharacters(in: .whitespacesAndNewlines))
        }
    }

    private func checkNickname(_ nickname: String) async {
        do {
            let result = try await repository.checkNickname(nickname)
            nicknameStatus = switch result {
            case .available: .available
            case .unavailable(let msg): .unavailable(msg)
            }
        } catch {
            nicknameStatus = .error("확인 중 오류가 발생했어요")
        }
    }

    public func saveNicknameStep() async -> User? {
        guard isNicknameValid else { return nil }
        saveState = .saving
        do {
            let number = Int(jerseyNumber.trimmingCharacters(in: .whitespacesAndNewlines))
            let user = try await repository.updateProfile(nickname: nickname, jerseyNumber: number)
            saveState = .idle
            return user
        } catch {
            saveState = .failed("저장 중 오류가 발생했어요")
            return nil
        }
    }

    // MARK: - Position

    public var isPositionValid: Bool { selectedMainPosition != nil }

    public func toggleSubPosition(_ position: BaseballPosition) {
        if selectedSubPositions.contains(position) {
            selectedSubPositions.remove(position)
        } else {
            selectedSubPositions.insert(position)
        }
    }

    public func savePositionStep() async -> User? {
        guard let main = selectedMainPosition else { return nil }
        saveState = .saving
        do {
            let subs = selectedSubPositions.filter { $0 != main }
            let user = try await repository.updatePosition(main: main, subs: Array(subs))
            saveState = .idle
            return user
        } catch {
            saveState = .failed("저장 중 오류가 발생했어요")
            return nil
        }
    }

    // MARK: - Start mode

    public func saveStartMode(_ mode: OnboardingStartMode?) async -> User? {
        saveState = .saving
        do {
            let user = try await repository.completeOnboarding(step: 3, startMode: mode)
            saveState = .idle
            return user
        } catch {
            saveState = .failed("저장 중 오류가 발생했어요")
            return nil
        }
    }
}
