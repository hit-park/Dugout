import Foundation

public struct User: Sendable, Equatable, Identifiable, Hashable {
    public let id: Int64
    public let email: String?
    public let nickname: String
    public let profileImgUrl: String?
    public let provider: AuthProvider
    public let jerseyNumber: Int?
    public let mainPosition: BaseballPosition?
    public let subPositions: [BaseballPosition]
    public let onboardingStep: Int

    public init(
        id: Int64,
        email: String?,
        nickname: String,
        profileImgUrl: String?,
        provider: AuthProvider,
        jerseyNumber: Int? = nil,
        mainPosition: BaseballPosition? = nil,
        subPositions: [BaseballPosition] = [],
        onboardingStep: Int = 0
    ) {
        self.id = id
        self.email = email
        self.nickname = nickname
        self.profileImgUrl = profileImgUrl
        self.provider = provider
        self.jerseyNumber = jerseyNumber
        self.mainPosition = mainPosition
        self.subPositions = subPositions
        self.onboardingStep = onboardingStep
    }

    public var isOnboardingComplete: Bool { onboardingStep >= 3 }
}

public enum AuthProvider: String, Sendable, Hashable, CaseIterable {
    case kakao = "KAKAO"
    case naver = "NAVER"
    case google = "GOOGLE"
    case apple = "APPLE"
    case dev = "DEV"

    public static var oauthProviders: [AuthProvider] {
        [.kakao, .naver, .google, .apple]
    }
}

public extension AuthProvider {
    var displayName: String {
        switch self {
        case .kakao: "카카오"
        case .naver: "네이버"
        case .google: "Google"
        case .apple: "Apple"
        case .dev: "개발 모드"
        }
    }
}

public enum BaseballPosition: String, Sendable, Hashable, CaseIterable {
    case pitcher = "P"
    case catcher = "C"
    case firstBase = "1B"
    case secondBase = "2B"
    case thirdBase = "3B"
    case shortStop = "SS"
    case leftField = "LF"
    case centerField = "CF"
    case rightField = "RF"
    case designatedHitter = "DH"

    public var displayName: String {
        switch self {
        case .pitcher:           "투수"
        case .catcher:           "포수"
        case .firstBase:         "1루수"
        case .secondBase:        "2루수"
        case .thirdBase:         "3루수"
        case .shortStop:         "유격수"
        case .leftField:         "좌익수"
        case .centerField:       "중견수"
        case .rightField:        "우익수"
        case .designatedHitter:  "지명타자"
        }
    }

    public var shortName: String { rawValue }

    /// 3×3 그리드 배치 순서 (DH는 별도 행)
    public static var gridPositions: [BaseballPosition] {
        [.pitcher, .catcher, .firstBase,
         .secondBase, .thirdBase, .shortStop,
         .leftField, .centerField, .rightField]
    }
}

public enum OnboardingStartMode: String, Sendable, Hashable {
    case createTeam = "CREATE_TEAM"
    case joinTeam = "JOIN_TEAM"
    case mercenary = "MERCENARY"
}
