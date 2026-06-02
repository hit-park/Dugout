//
//  NotificationSettingsViewModel.swift
//  Dugout
//

import Foundation

@MainActor
final class NotificationSettingsViewModel: ObservableObject {
    @Published var matchCreated = true
    @Published var lineupConfirmed = true
    @Published var attendanceReminder = true
    @Published var attendanceChanged = true
    @Published var dndEnabled = true
    @Published var dndStart = NotificationSettingsViewModel.time(22, 0)
    @Published var dndEnd = NotificationSettingsViewModel.time(8, 0)
    @Published var isLoading = false
    @Published var errorMessage: String?

    private let repository: any NotificationPreferenceRepository

    init(repository: any NotificationPreferenceRepository = NotificationPreferenceRepositoryImpl()) {
        self.repository = repository
    }

    func load() async {
        isLoading = true
        defer { isLoading = false }
        do {
            apply(try await repository.fetch())
        } catch {
            errorMessage = "알림 설정을 불러오지 못했습니다"
        }
    }

    func save() async {
        do {
            apply(try await repository.update(currentDTO()))
        } catch {
            errorMessage = "알림 설정 저장에 실패했습니다"
        }
    }

    private func apply(_ dto: NotificationPreferenceDTO) {
        matchCreated = dto.matchCreated
        lineupConfirmed = dto.lineupConfirmed
        attendanceReminder = dto.attendanceReminder
        attendanceChanged = dto.attendanceChanged
        dndEnabled = dto.dndEnabled
        dndStart = Self.parse(dto.dndStart) ?? dndStart
        dndEnd = Self.parse(dto.dndEnd) ?? dndEnd
    }

    private func currentDTO() -> NotificationPreferenceDTO {
        NotificationPreferenceDTO(
            matchCreated: matchCreated,
            lineupConfirmed: lineupConfirmed,
            attendanceReminder: attendanceReminder,
            attendanceChanged: attendanceChanged,
            dndEnabled: dndEnabled,
            dndStart: Self.format(dndStart),
            dndEnd: Self.format(dndEnd)
        )
    }

    private static func time(_ h: Int, _ m: Int) -> Date {
        Calendar.current.date(from: DateComponents(hour: h, minute: m)) ?? Date()
    }

    private static func parse(_ s: String) -> Date? {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "HH:mm:ss"
        return formatter.date(from: s)
    }

    private static func format(_ d: Date) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "HH:mm:ss"
        return formatter.string(from: d)
    }
}
