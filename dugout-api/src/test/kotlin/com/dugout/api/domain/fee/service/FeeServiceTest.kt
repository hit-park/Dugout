package com.dugout.api.domain.fee.service

import com.dugout.api.domain.fee.dto.CreateFeeRequest
import com.dugout.api.domain.fee.dto.ProcessPaymentRequest
import com.dugout.api.domain.fee.entity.Fee
import com.dugout.api.domain.fee.entity.FeePayment
import com.dugout.api.domain.fee.entity.FeeType
import com.dugout.api.domain.fee.entity.PaymentStatus
import com.dugout.api.domain.fee.repository.FeePaymentRepository
import com.dugout.api.domain.fee.repository.FeeRepository
import com.dugout.api.domain.team.entity.Team
import com.dugout.api.domain.team.entity.TeamMember
import com.dugout.api.domain.team.entity.TeamRole
import com.dugout.api.domain.team.repository.TeamMemberRepository
import com.dugout.api.domain.team.repository.TeamRepository
import com.dugout.api.domain.user.entity.AuthProvider
import com.dugout.api.domain.user.entity.User
import com.dugout.api.global.error.BusinessException
import com.dugout.api.global.error.ErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class FeeServiceTest {

    @Mock lateinit var feeRepository: FeeRepository
    @Mock lateinit var feePaymentRepository: FeePaymentRepository
    @Mock lateinit var teamRepository: TeamRepository
    @Mock lateinit var teamMemberRepository: TeamMemberRepository

    private lateinit var feeService: FeeService

    @BeforeEach
    fun setUp() {
        feeService = FeeService(
            feeRepository,
            feePaymentRepository,
            teamRepository,
            teamMemberRepository,
        )
    }

    private fun sampleTeam() = Team.create(name = "두갓FC", region = "서울 강남구")
    private fun user(nickname: String = "김주장") = User.create(AuthProvider.KAKAO, "kakao-1", nickname)

    private fun createRequest(amount: Long = 50_000L) = CreateFeeRequest(
        title = "5월 회비",
        amount = amount,
        feeType = FeeType.MONTHLY,
        dueDate = LocalDate.of(2026, 5, 31),
    )

    @Test
    fun `회비 생성 - ACCOUNTANT가 생성 가능하고 활성 멤버에게 FeePayment 자동 생성`() {
        val team = sampleTeam()
        val accountantUser = user("회계")
        val accountant = TeamMember.create(team, accountantUser, TeamRole.ACCOUNTANT)
        val memberA = TeamMember.create(team, user("선수A"), TeamRole.MEMBER)
        val memberB = TeamMember.create(team, user("선수B"), TeamRole.MEMBER)

        whenever(teamMemberRepository.findByTeamIdAndUserId(team.id, 1L)).thenReturn(accountant)
        whenever(teamRepository.findById(team.id)).thenReturn(Optional.of(team))
        whenever(feeRepository.save(any<Fee>())).thenAnswer { it.getArgument<Fee>(0) }
        whenever(teamMemberRepository.findByTeamIdAndIsActiveTrue(team.id))
            .thenReturn(listOf(accountant, memberA, memberB))
        whenever(feePaymentRepository.saveAll(any<List<FeePayment>>())).thenAnswer {
            it.getArgument<List<FeePayment>>(0)
        }
        whenever(feePaymentRepository.countByFeeIdAndStatus(any(), eq(PaymentStatus.PAID))).thenReturn(0L)
        whenever(feePaymentRepository.countByFeeIdAndStatus(any(), eq(PaymentStatus.UNPAID))).thenReturn(3L)

        val response = feeService.createFee(1L, team.id, createRequest())

        assertEquals("5월 회비", response.title)
        assertEquals(50_000L, response.amount)
        assertEquals("MONTHLY", response.feeType)
        assertEquals(3L, response.totalCount)
    }

    @Test
    fun `회비 생성 - MEMBER 권한은 TEAM_ROLE_NOT_ALLOWED`() {
        val team = sampleTeam()
        val member = TeamMember.create(team, user(), TeamRole.MEMBER)
        whenever(teamMemberRepository.findByTeamIdAndUserId(team.id, 1L)).thenReturn(member)

        val exception = assertThrows<BusinessException> {
            feeService.createFee(1L, team.id, createRequest())
        }
        assertEquals(ErrorCode.TEAM_ROLE_NOT_ALLOWED, exception.errorCode)
    }

    @Test
    fun `회비 생성 - 비멤버는 NOT_TEAM_MEMBER`() {
        whenever(teamMemberRepository.findByTeamIdAndUserId(1L, 1L)).thenReturn(null)

        val exception = assertThrows<BusinessException> {
            feeService.createFee(1L, 1L, createRequest())
        }
        assertEquals(ErrorCode.NOT_TEAM_MEMBER, exception.errorCode)
    }

    @Test
    fun `납부 처리 - PAID 상태인데 amountPaid가 fee 금액과 다르면 INVALID_PAYMENT_STATUS`() {
        val team = sampleTeam()
        val captainUser = user("주장")
        val captain = TeamMember.create(team, captainUser, TeamRole.CAPTAIN)
        val targetUser = user("선수")
        val fee = Fee.create(team, "5월 회비", 50_000L, FeeType.MONTHLY, LocalDate.of(2026, 5, 31))

        whenever(feeRepository.findById(fee.id)).thenReturn(Optional.of(fee))
        whenever(teamMemberRepository.findByTeamIdAndUserId(team.id, 1L)).thenReturn(captain)

        val exception = assertThrows<BusinessException> {
            feeService.processPayment(
                actorUserId = 1L,
                feeId = fee.id,
                targetUserId = targetUser.id,
                request = ProcessPaymentRequest(PaymentStatus.PAID, amountPaid = 30_000L),
            )
        }
        assertEquals(ErrorCode.INVALID_PAYMENT_STATUS, exception.errorCode)
    }

    @Test
    fun `납부 처리 - PARTIAL은 0 이하 또는 fee 금액 이상이면 INVALID_PAYMENT_STATUS`() {
        val team = sampleTeam()
        val captain = TeamMember.create(team, user("주장"), TeamRole.CAPTAIN)
        val targetUser = user("선수")
        val fee = Fee.create(team, "5월 회비", 50_000L, FeeType.MONTHLY, LocalDate.of(2026, 5, 31))

        whenever(feeRepository.findById(fee.id)).thenReturn(Optional.of(fee))
        whenever(teamMemberRepository.findByTeamIdAndUserId(team.id, 1L)).thenReturn(captain)

        val exception = assertThrows<BusinessException> {
            feeService.processPayment(
                1L, fee.id, targetUser.id,
                ProcessPaymentRequest(PaymentStatus.PARTIAL, amountPaid = 50_000L),
            )
        }
        assertEquals(ErrorCode.INVALID_PAYMENT_STATUS, exception.errorCode)
    }

    @Test
    fun `납부 처리 - 정상 PAID 처리되면 paidAt 기록되고 응답 status는 PAID`() {
        val team = sampleTeam()
        val captainUser = user("주장")
        val captain = TeamMember.create(team, captainUser, TeamRole.CAPTAIN)
        val targetUser = user("선수")
        val fee = Fee.create(team, "5월 회비", 50_000L, FeeType.MONTHLY, LocalDate.of(2026, 5, 31))
        val payment = FeePayment.ofUnpaid(fee, targetUser)

        whenever(feeRepository.findById(fee.id)).thenReturn(Optional.of(fee))
        whenever(teamMemberRepository.findByTeamIdAndUserId(team.id, 1L)).thenReturn(captain)
        whenever(feePaymentRepository.findByFeeIdAndUserId(fee.id, targetUser.id)).thenReturn(payment)

        val response = feeService.processPayment(
            1L, fee.id, targetUser.id,
            ProcessPaymentRequest(PaymentStatus.PAID, amountPaid = 50_000L, memo = "현금 수금"),
        )

        assertEquals("PAID", response.status)
        assertEquals(50_000L, response.amountPaid)
        assertEquals(1L, response.confirmedBy)
    }

    @Test
    fun `회비 단건 수정 - 존재하지 않는 fee면 FEE_NOT_FOUND`() {
        whenever(feeRepository.findById(999L)).thenReturn(Optional.empty())

        val exception = assertThrows<BusinessException> {
            feeService.updateFee(1L, 999L, com.dugout.api.domain.fee.dto.UpdateFeeRequest(amount = 60_000L))
        }
        assertEquals(ErrorCode.FEE_NOT_FOUND, exception.errorCode)
    }
}
