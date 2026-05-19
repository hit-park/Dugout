import ProjectDescription

let bundleIDPrefix = "com.dugout"
let deploymentTargets: DeploymentTargets = .iOS("17.0")
let destinations: Destinations = .iOS

// MARK: - Shared Settings

let baseSettings: SettingsDictionary = [
    "SWIFT_VERSION": "6.0",
    "SWIFT_STRICT_CONCURRENCY": "complete",
    "IPHONEOS_DEPLOYMENT_TARGET": "17.0",
]

// MARK: - Target Helpers

/// 프레임워크 타겟 생성 헬퍼
func frameworkTarget(
    name: String,
    sourcesPath: String,
    resources: ResourceFileElements? = nil,
    dependencies: [TargetDependency] = []
) -> Target {
    .target(
        name: name,
        destinations: destinations,
        product: .framework,
        bundleId: "\(bundleIDPrefix).\(name)",
        deploymentTargets: deploymentTargets,
        sources: ["\(sourcesPath)/**"],
        resources: resources,
        dependencies: dependencies,
        settings: .settings(base: baseSettings)
    )
}

// MARK: - Core Targets

let coreNetwork = frameworkTarget(
    name: "DugoutCoreNetwork",
    sourcesPath: "Core/Network/Sources",
    dependencies: [
        .external(name: "Alamofire"),
    ]
)

let designSystem = frameworkTarget(
    name: "DugoutDesignSystem",
    sourcesPath: "Core/DesignSystem/Sources",
    resources: ["Core/DesignSystem/Resources/**"]
)

// MARK: - Feature Targets

let authFeature = frameworkTarget(
    name: "DugoutAuthFeature",
    sourcesPath: "Features/Auth/Sources",
    dependencies: [
        .target(name: "DugoutCoreNetwork"),
        .target(name: "DugoutDesignSystem"),
    ]
)

let homeFeature = frameworkTarget(
    name: "DugoutHomeFeature",
    sourcesPath: "Features/Home/Sources",
    dependencies: [
        .target(name: "DugoutCoreNetwork"),
        .target(name: "DugoutDesignSystem"),
        .target(name: "DugoutAuthFeature"),
        .target(name: "DugoutTeamFeature"),
    ]
)

let teamFeature = frameworkTarget(
    name: "DugoutTeamFeature",
    sourcesPath: "Features/Team/Sources",
    dependencies: [
        .target(name: "DugoutCoreNetwork"),
        .target(name: "DugoutDesignSystem"),
    ]
)

// MARK: - App Target

let app: Target = .target(
    name: "Dugout",
    destinations: destinations,
    product: .app,
    bundleId: "\(bundleIDPrefix).Dugout",
    deploymentTargets: deploymentTargets,
    infoPlist: .extendingDefault(with: [
        "UILaunchScreen": [:],
        "CFBundleShortVersionString": "1.0.0",
        "CFBundleVersion": "1",
        "UIApplicationSceneManifest": [
            "UIApplicationSupportsMultipleScenes": false,
        ],
        "NSAppTransportSecurity": [
            "NSAllowsArbitraryLoads": true,
            "NSExceptionDomains": [
                "localhost": [
                    "NSExceptionAllowsInsecureHTTPLoads": true,
                ],
            ],
        ],
    ]),
    sources: ["App/Sources/**"],
    resources: ["App/Resources/**"],
    dependencies: [
        .target(name: "DugoutAuthFeature"),
        .target(name: "DugoutHomeFeature"),
        .target(name: "DugoutTeamFeature"),
    ],
    settings: .settings(base: baseSettings)
)

// MARK: - Project

let project = Project(
    name: "Dugout",
    organizationName: "Dugout",
    targets: [
        app,
        coreNetwork,
        designSystem,
        authFeature,
        homeFeature,
        teamFeature,
    ]
)
