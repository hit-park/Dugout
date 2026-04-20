package com.dugout.api.domain.team.controller

import com.dugout.api.domain.team.dto.CreateTeamRequest
import com.dugout.api.domain.team.dto.InviteCodeResponse
import com.dugout.api.domain.team.dto.JoinTeamRequest
import com.dugout.api.domain.team.dto.MyTeamResponse
import com.dugout.api.domain.team.dto.TeamMemberResponse
import com.dugout.api.domain.team.dto.TeamResponse
import com.dugout.api.domain.team.dto.UpdateMemberRequest
import com.dugout.api.domain.team.dto.UpdateTeamRequest
import com.dugout.api.domain.team.service.TeamService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class TeamController(
    private val teamService: TeamService,
) {

    @PostMapping("/teams")
    fun createTeam(
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: CreateTeamRequest,
    ): ResponseEntity<TeamResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(teamService.createTeam(userId, request))

    @GetMapping("/teams/{teamId}")
    fun getTeam(@PathVariable teamId: Long): ResponseEntity<TeamResponse> =
        ResponseEntity.ok(teamService.getTeam(teamId))

    @PutMapping("/teams/{teamId}")
    fun updateTeam(
        @AuthenticationPrincipal userId: Long,
        @PathVariable teamId: Long,
        @Valid @RequestBody request: UpdateTeamRequest,
    ): ResponseEntity<TeamResponse> =
        ResponseEntity.ok(teamService.updateTeam(userId, teamId, request))

    @PostMapping("/teams/{teamId}/invite")
    fun generateInviteCode(
        @AuthenticationPrincipal userId: Long,
        @PathVariable teamId: Long,
    ): ResponseEntity<InviteCodeResponse> =
        ResponseEntity.ok(teamService.generateInviteCode(userId, teamId))

    @PostMapping("/teams/join")
    fun joinTeam(
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: JoinTeamRequest,
    ): ResponseEntity<TeamMemberResponse> =
        ResponseEntity.ok(teamService.joinTeam(userId, request))

    @GetMapping("/teams/{teamId}/members")
    fun getMembers(@PathVariable teamId: Long): ResponseEntity<List<TeamMemberResponse>> =
        ResponseEntity.ok(teamService.getMembers(teamId))

    @PutMapping("/teams/{teamId}/members/{memberId}")
    fun updateMember(
        @AuthenticationPrincipal userId: Long,
        @PathVariable teamId: Long,
        @PathVariable memberId: Long,
        @Valid @RequestBody request: UpdateMemberRequest,
    ): ResponseEntity<TeamMemberResponse> =
        ResponseEntity.ok(teamService.updateMember(userId, teamId, memberId, request))

    @DeleteMapping("/teams/{teamId}/members/{memberId}")
    fun removeMember(
        @AuthenticationPrincipal userId: Long,
        @PathVariable teamId: Long,
        @PathVariable memberId: Long,
    ): ResponseEntity<Void> {
        teamService.removeMember(userId, teamId, memberId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/users/me/teams")
    fun getMyTeams(@AuthenticationPrincipal userId: Long): ResponseEntity<List<MyTeamResponse>> =
        ResponseEntity.ok(teamService.getMyTeams(userId))
}
