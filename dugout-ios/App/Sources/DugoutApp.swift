import SwiftUI
import DugoutDesignSystem
import DugoutAuthFeature

@main
struct DugoutApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    @State private var router = AppRouter()
    @State private var authViewModel = AuthViewModel()
    @State private var onboardingViewModel = OnboardingViewModel()

    init() {
        DGFontRegistrar.registerIfNeeded()
    }

    var body: some Scene {
        WindowGroup {
            rootView
                .preferredColorScheme(.light)
                .task { await checkAuthOnLaunch() }
        }
    }

    @ViewBuilder
    private var rootView: some View {
        switch router.route {
        case .splash:
            SplashView()
        case .login:
            AuthView(
                viewModel: authViewModel,
                onLoginSuccess: { router.navigateAfterLogin(user: $0) },
                onBrowseWithoutLogin: { router.navigate(to: .main) }
            )
        case .onboarding(let step):
            onboardingView(for: step)
        case .main:
            MainTabView(authViewModel: authViewModel, router: router)
        }
    }

    @ViewBuilder
    private func onboardingView(for step: OnboardingStep) -> some View {
        switch step {
        case .nickname:
            OnboardingNicknameView(viewModel: onboardingViewModel) { user in
                authViewModel.updateCurrentUser(user)
                router.navigate(to: .onboarding(.position))
            }
        case .position:
            OnboardingPositionView(viewModel: onboardingViewModel) { user in
                authViewModel.updateCurrentUser(user)
                router.navigate(to: .onboarding(.startMode))
            }
        case .startMode:
            OnboardingStartModeView(
                viewModel: onboardingViewModel,
                onCreateTeam: { router.navigate(to: .main) },  // Phase 3: TEAM-2로 교체
                onJoinTeam: { router.navigate(to: .main) },    // Phase 3: TEAM-3으로 교체
                onMercenary: { router.navigate(to: .main) },
                onSkip: { router.navigate(to: .main) }
            )
        }
    }

    private func checkAuthOnLaunch() async {
        // Splash는 2.0초 유지 + 0.4초 fade-out. 인증 확인과 병렬로 최소 대기.
        async let minimumWait: () = Task.sleep(for: .seconds(2.4))
        async let authCheck: () = authViewModel.checkAuthStatus()
        _ = try? await (minimumWait, authCheck)

        if let user = authViewModel.currentUser {
            router.navigateAfterLogin(user: user)
        } else {
            router.navigate(to: .login)
        }
    }
}
