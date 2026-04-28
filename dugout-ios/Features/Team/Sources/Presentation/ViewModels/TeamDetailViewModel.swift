//
//  TeamDetailViewModel.swift
//  DugoutTeamFeature
//

import Foundation
import Observation
import DugoutCoreNetwork

@MainActor
@Observable
public final class TeamDetailViewModel {
    public struct LoadedData: Sendable, Equatable {
        public let team: Team
        public let members: [TeamMember]
    }

    public enum State: Sendable {
        case idle
        case loading
        case loaded(LoadedData)
        case failed(String)
    }

    public private(set) var state: State = .idle
    public private(set) var inviteCode: String?
    public private(set) var inviteCodeError: String?
    public private(set) var isGeneratingInviteCode: Bool = false
    public var selectedMember: TeamMember?
    public var memberActionError: String?
    public private(set) var isMemberActionInFlight: Bool = false

    private let teamId: Int64
    private let currentUserId: Int64?
    private let repository: any TeamRepository

    public init(
        teamId: Int64,
        currentUserId: Int64?,
        repository: any TeamRepository = TeamRepositoryImpl()
    ) {
        self.teamId = teamId
        self.currentUserId = currentUserId
        self.repository = repository
    }

    /// 현재 사용자의 팀 내 역할. 비로그인이거나 멤버가 아니면 nil.
    public var myRole: TeamRole? {
        guard
            case .loaded(let data) = state,
            let userId = currentUserId,
            let me = data.members.first(where: { $0.userId == userId })
        else { return nil }
        return me.role
    }

    public var canShowInviteCode: Bool { myRole?.canShowInviteCode ?? false }
    public var canEditTeam: Bool       { myRole?.canEditTeam ?? false }
    public var canManageMembers: Bool  { myRole?.canManageMembers ?? false }

    public func load() async {
        state = .loading
        inviteCode = nil
        inviteCodeError = nil
        async let teamTask = repository.fetchTeam(id: teamId)
        async let membersTask = repository.fetchMembers(teamId: teamId)
        do {
            let (team, members) = try await (teamTask, membersTask)
            state = .loaded(LoadedData(team: team, members: members))
        } catch let error as APIError {
            state = .failed(error.userMessage)
        } catch {
            state = .failed("팀 정보를 불러오지 못했습니다")
        }
    }

    public func generateInviteCode() async {
        isGeneratingInviteCode = true
        inviteCodeError = nil
        defer { isGeneratingInviteCode = false }
        do {
            let code = try await repository.generateInviteCode(teamId: teamId)
            inviteCode = code
        } catch let error as APIError {
            inviteCodeError = error.userMessage
        } catch {
            inviteCodeError = "초대 코드 생성에 실패했습니다"
        }
    }

    public func isMemberActionable(_ member: TeamMember) -> Bool {
        guard canManageMembers else { return false }
        if member.role == .captain { return false }
        if member.userId == currentUserId { return false }
        return true
    }

    public func tapMember(_ member: TeamMember) {
        guard isMemberActionable(member) else { return }
        selectedMember = member
    }

    public func updateMemberRole(_ role: TeamRole) async {
        guard !isMemberActionInFlight, let member = selectedMember else { return }
        isMemberActionInFlight = true
        defer {
            isMemberActionInFlight = false
            selectedMember = nil
        }
        do {
            _ = try await repository.updateMember(
                teamId: teamId,
                memberId: member.id,
                role: role
            )
            await load()
        } catch let error as APIError {
            memberActionError = error.userMessage
        } catch {
            memberActionError = "역할 변경에 실패했습니다"
        }
    }

    public func removeMember() async {
        guard !isMemberActionInFlight, let member = selectedMember else { return }
        isMemberActionInFlight = true
        defer {
            isMemberActionInFlight = false
            selectedMember = nil
        }
        do {
            try await repository.removeMember(teamId: teamId, memberId: member.id)
            await load()
        } catch let error as APIError {
            memberActionError = error.userMessage
        } catch {
            memberActionError = "추방에 실패했습니다"
        }
    }
}
