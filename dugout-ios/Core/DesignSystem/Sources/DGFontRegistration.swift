//
//  DGFontRegistration.swift
//  DugoutDesignSystem
//
//  프레임워크 번들 폰트는 Info.plist UIAppFonts 로 자동 등록되지 않으므로
//  런타임에 1회 수동 등록한다. 폰트 파일이 없으면 no-op (Font.custom 이
//  시스템 폰트로 graceful fallback).
//

import CoreText
import Foundation

public enum DGFontRegistrar {

    private static let didRegister = LockIsolated(false)

    /// 앱 진입 시 1회 호출. idempotent.
    public static func registerIfNeeded() {
        didRegister.withValue { registered in
            guard !registered else { return }
            registered = true
            registerBundledFonts()
        }
    }

    private static func registerBundledFonts() {
        let bundle = Bundle.module
        let extensions = ["otf", "ttf"]
        let urls = extensions.flatMap { ext in
            bundle.urls(forResourcesWithExtension: ext, subdirectory: nil) ?? []
        }
        for url in urls {
            var error: Unmanaged<CFError>?
            // 이미 등록된 폰트는 false 반환 — 무시 (idempotent).
            CTFontManagerRegisterFontsForURL(url as CFURL, .process, &error)
            error?.release()
        }
    }
}

/// 작은 Sendable 락 박스 (전역 1회 가드 전용).
private final class LockIsolated<Value>: @unchecked Sendable {
    private var value: Value
    private let lock = NSLock()

    init(_ value: Value) { self.value = value }

    func withValue<T>(_ body: (inout Value) -> T) -> T {
        lock.lock()
        defer { lock.unlock() }
        return body(&value)
    }
}
