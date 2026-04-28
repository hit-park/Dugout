//
//  TeamRepository.swift
//  DugoutTeamFeature
//

import Foundation

public protocol TeamRepository: Sendable {
    /// 팀 생성 (생성자는 자동으로 CAPTAIN)
    func createTeam(_ request: CreateTeamRequest) async throws -> Team

    /// 팀 상세 조회
    func fetchTeam(id: Int64) async throws -> Team

    /// 팀 멤버 목록 조회
    func fetchMembers(teamId: Int64) async throws -> [TeamMember]

    /// 초대 코드 생성 (CAPTAIN/MANAGER만 가능)
    func generateInviteCode(teamId: Int64) async throws -> String

    /// 초대 코드로 팀 가입
    func joinTeam(inviteCode: String) async throws -> TeamMember

    /// 팀 정보 수정 (CAPTAIN/MANAGER 권한). 변경할 필드만 채워 보낸다.
    func updateTeam(id: Int64, request: UpdateTeamRequest) async throws -> Team
}

/// 팀 생성 요청 데이터 (Domain 계층).
public struct CreateTeamRequest: Sendable, Equatable {
    public let name: String
    public let region: String
    public let division: Int
    public let activityDays: [String]
    public let activityTime: String?
    public let lineupMode: LineupMode

    public init(
        name: String,
        region: String,
        division: Int = 4,
        activityDays: [String] = [],
        activityTime: String? = nil,
        lineupMode: LineupMode = .balanced
    ) {
        self.name = name
        self.region = region
        self.division = division
        self.activityDays = activityDays
        self.activityTime = activityTime
        self.lineupMode = lineupMode
    }
}

/// 팀 정보 수정 요청 데이터 (Domain 계층). 모든 필드 optional — 변경할 필드만 채워서 보낸다.
public struct UpdateTeamRequest: Sendable, Equatable {
    public let name: String?
    public let region: String?
    public let division: Int?
    public let activityDays: [String]?
    public let activityTime: String?
    public let lineupMode: LineupMode?

    public init(
        name: String? = nil,
        region: String? = nil,
        division: Int? = nil,
        activityDays: [String]? = nil,
        activityTime: String? = nil,
        lineupMode: LineupMode? = nil
    ) {
        self.name = name
        self.region = region
        self.division = division
        self.activityDays = activityDays
        self.activityTime = activityTime
        self.lineupMode = lineupMode
    }
}
