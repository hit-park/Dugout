import SwiftUI
import DugoutAuthFeature

public enum AppTab: Hashable, Sendable, CaseIterable {
    case home, schedule, matching, team, my
}

public enum OnboardingStep: Hashable, Sendable {
    case nickname
    case position
    case startMode
}

public enum AppRoute: Equatable, Sendable {
    case splash
    case login
    case onboarding(OnboardingStep)
    case main
}

@MainActor
@Observable
public final class AppRouter {
    public var route: AppRoute = .splash
    public var selectedTab: AppTab = .home

    public var homePath = NavigationPath()
    public var schedulePath = NavigationPath()
    public var matchingPath = NavigationPath()
    public var teamPath = NavigationPath()
    public var myPath = NavigationPath()

    public func navigate(to route: AppRoute) {
        self.route = route
    }

    func handlePush(_ route: PushRoute) {
        selectedTab = .schedule
        guard let matchId = route.matchId else { return }
        schedulePath = NavigationPath()
        schedulePath.append(matchId)
    }

    public func navigateAfterLogin(user: User) {
        if user.isOnboardingComplete {
            route = .main
        } else {
            route = .onboarding(nextOnboardingStep(for: user))
        }
    }

    private func nextOnboardingStep(for user: User) -> OnboardingStep {
        switch user.onboardingStep {
        case 0: .nickname
        case 1: .position
        default: .startMode
        }
    }
}
