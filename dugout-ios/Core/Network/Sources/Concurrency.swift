//
//  Concurrency.swift
//  DugoutCoreNetwork
//

import Foundation

/// 작업이 지정된 시간 안에 끝나지 않으면 throw.
public struct TimeoutError: Error, Sendable {
    public init() {}
}

/// 비동기 작업에 timeout을 적용한다.
/// operation 또는 sleep 중 먼저 끝나는 쪽이 결과를 반환하고 나머지는 cancel.
public func withTimeout<T: Sendable>(
    seconds: TimeInterval,
    _ operation: @escaping @Sendable () async throws -> T
) async throws -> T {
    try await withThrowingTaskGroup(of: T.self) { group in
        group.addTask { try await operation() }
        group.addTask {
            try await Task.sleep(for: .seconds(seconds))
            throw TimeoutError()
        }
        let result = try await group.next()!
        group.cancelAll()
        return result
    }
}
