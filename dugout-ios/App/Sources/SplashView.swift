import SwiftUI
import DugoutDesignSystem

struct SplashView: View {
    @State private var opacity = 1.0

    var body: some View {
        ZStack {
            DGColor.p500.ignoresSafeArea()

            VStack(spacing: 12) {
                Image(systemName: "baseball.fill")
                    .font(.system(size: 72, weight: .bold))
                    .foregroundStyle(.white)
                Text("Dugout")
                    .font(DGFont.pretendard(.extrabold, size: 40))
                    .foregroundStyle(.white)
                    .tracking(-1)
            }
        }
        .opacity(opacity)
        .onAppear {
            withAnimation(.easeOut(duration: 0.4).delay(2.0)) {
                opacity = 0
            }
        }
    }
}

#Preview {
    SplashView()
}
