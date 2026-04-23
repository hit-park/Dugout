//
//  AppConfig.swift
//  DugoutCoreNetwork
//

import Foundation

public enum AppConfig {
    /// 백엔드 기본 URL.
    /// Phase 1: 로컬 개발 (시뮬레이터에서는 localhost, 실기기는 맥북 IP 필요).
    public static let apiBaseURL: URL = {
        #if targetEnvironment(simulator)
        URL(string: "http://localhost:8080")!
        #else
        // TODO: 실기기 테스트 시 맥북 IP로 교체 또는 Info.plist 환경 변수 사용
        URL(string: "http://localhost:8080")!
        #endif
    }()
}
