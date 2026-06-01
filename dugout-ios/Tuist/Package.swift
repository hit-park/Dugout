// swift-tools-version: 6.0
@preconcurrency import PackageDescription

#if TUIST
import struct ProjectDescription.PackageSettings

let packageSettings = PackageSettings(
    productTypes: [
        "Alamofire": .framework,
    ]
)
#endif

let package = Package(
    name: "Dugout",
    dependencies: [
        // Alamofire 5.11+ 는 Xcode 16.3 / Swift 6.1 런타임(_swift_coroFrameAlloc) 요구.
        // 현 환경(Xcode 16.0 / Swift 6.0)과 호환되는 5.10.x 로 고정.
        .package(url: "https://github.com/Alamofire/Alamofire.git", .upToNextMinor(from: "5.10.0")),
        .package(url: "https://github.com/firebase/firebase-ios-sdk", from: "11.0.0"),
    ]
)
