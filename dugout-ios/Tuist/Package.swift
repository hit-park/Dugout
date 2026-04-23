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
        .package(url: "https://github.com/Alamofire/Alamofire.git", from: "5.10.0"),
    ]
)
