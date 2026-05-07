package com.dugout.api.domain.fee.service

import com.dugout.api.domain.fee.dto.CreateFeeRequest
import com.dugout.api.domain.fee.dto.FeePaymentResponse
import com.dugout.api.domain.fee.dto.FeeResponse
import com.dugout.api.domain.fee.dto.FinanceSummaryResponse
import com.dugout.api.domain.fee.dto.ProcessPaymentRequest
import com.dugout.api.domain.fee.dto.UpdateFeeRequest
import com.dugout.api.domain.fee.entity.Fee
import com.dugout.api.domain.fee.entity.FeePayment
import com.dugout.api.domain.fee.entity.PaymentStatus
import com.dugout.api.domain.fee.repository.FeePaymentRepository
import com.dugout.api.domain.fee.repository.FeeRepository
import com.dugout.api.domain.team.entity.TeamRole
import com.dugout.api.domain.team.repository.TeamMemberRepository
import com.dugout.api.domain.team.repository.TeamRepository
import com.dugout.api.global.error.BusinessException
import com.dugout.api.global.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class FeeService(
    private val feeRepository: FeeRepository,
    private val feePaymentRepository: FeePaymentRepository,
    private val teamRepository: TeamRepository,
    private val teamMemberRepository: TeamMemberRepository,
) {

    @Transactional
    fun createFee(userId: Long, teamId: Long, request: CreateFeeRequest): FeeResponse {
        requireFeeManagement(teamId, userId)

        val team = teamRepository.findById(teamId)
            .orElseThrow { BusinessException(ErrorCode.TEAM_NOT_FOUND) }

        val fee = feeRepository.save(
            Fee.create(
                team = team,
                title = request.title,
                amount = request.amount,
                feeType = request.feeType,
                dueDate = request.dueDate,
                matchId = request.matchId,
                memo = request.memo,
            ),
        )

        val activeMembers = teamMemberRepository.findByTeamIdAndIsActiveTrue(teamId)
        val payments = activeMembers.map { FeePayment.ofUnpaid(fee, it.user) }
        feePaymentRepository.saveAll(payments)

        return buildFeeResponse(fee, totalCount = payments.size.toLong())
    }

    fun getFees(userId: Long, teamId: Long): List<FeeResponse> {
        requireTeamMember(teamId, userId)
        val fees = feeRepository.findByTeamIdOrderByDueDateDesc(teamId)
        return fees.map { buildFeeResponse(it) }
    }

    @Transactional
    fun updateFee(userId: Long, feeId: Long, request: UpdateFeeRequest): FeeResponse {
        val fee = findFee(feeId)
        requireFeeManagement(fee.team.id, userId)

        fee.update(
            title = request.title,
            amount = request.amount,
            dueDate = request.dueDate,
            memo = request.memo,
        )
        return buildFeeResponse(fee)
    }

    fun getPayments(userId: Long, feeId: Long): List<FeePaymentResponse> {
        val fee = findFee(feeId)
        requireTeamMember(fee.team.id, userId)
        return feePaymentRepository.findByFeeId(feeId).map(FeePaymentResponse::from)
    }

    @Transactional
    fun processPayment(
        actorUserId: Long,
        feeId: Long,
        targetUserId: Long,
        request: ProcessPaymentRequest,
    ): FeePaymentResponse {
        val fee = findFee(feeId)
        requireFeeManagement(fee.team.id, actorUserId)

        validatePaymentRequest(fee, request)

        val payment = feePaymentRepository.findByFeeIdAndUserId(feeId, targetUserId)
            ?: throw BusinessException(ErrorCode.FEE_PAYMENT_NOT_FOUND)

        payment.applyPayment(
            amountPaid = request.amountPaid,
            status = request.status,
            confirmedBy = actorUserId,
            memo = request.memo,
        )

        return FeePaymentResponse.from(payment)
    }

    fun getFinanceSummary(userId: Long, teamId: Long): FinanceSummaryResponse {
        requireTeamMember(teamId, userId)

        val collected = feePaymentRepository.sumCollectedByTeam(teamId)
        val outstanding = feePaymentRepository.sumOutstandingByTeam(teamId)
        val feeCount = feeRepository.findByTeamIdOrderByDueDateDesc(teamId).size

        return FinanceSummaryResponse(
            teamId = teamId,
            totalCollected = collected,
            totalOutstanding = outstanding,
            feeCount = feeCount,
        )
    }

    private fun validatePaymentRequest(fee: Fee, request: ProcessPaymentRequest) {
        if (request.amountPaid < 0) {
            throw BusinessException(ErrorCode.INVALID_FEE_AMOUNT)
        }
        if (request.amountPaid > fee.amount) {
            throw BusinessException(ErrorCode.INVALID_FEE_AMOUNT)
        }
        when (request.status) {
            PaymentStatus.PAID -> if (request.amountPaid != fee.amount) {
                throw BusinessException(ErrorCode.INVALID_PAYMENT_STATUS)
            }
            PaymentStatus.PARTIAL -> if (request.amountPaid <= 0L || request.amountPaid >= fee.amount) {
                throw BusinessException(ErrorCode.INVALID_PAYMENT_STATUS)
            }
            PaymentStatus.UNPAID -> if (request.amountPaid != 0L) {
                throw BusinessException(ErrorCode.INVALID_PAYMENT_STATUS)
            }
        }
    }

    private fun findFee(feeId: Long): Fee =
        feeRepository.findById(feeId)
            .orElseThrow { BusinessException(ErrorCode.FEE_NOT_FOUND) }

    private fun buildFeeResponse(fee: Fee, totalCount: Long? = null): FeeResponse {
        val resolvedTotal = totalCount
            ?: feePaymentRepository.findByFeeId(fee.id).size.toLong()
        val paid = feePaymentRepository.countByFeeIdAndStatus(fee.id, PaymentStatus.PAID)
        val unpaid = feePaymentRepository.countByFeeIdAndStatus(fee.id, PaymentStatus.UNPAID)
        return FeeResponse.of(fee, resolvedTotal, paid, unpaid)
    }

    private fun requireTeamMember(teamId: Long, userId: Long) {
        if (!teamRepository.existsById(teamId)) {
            throw BusinessException(ErrorCode.TEAM_NOT_FOUND)
        }
        if (!teamMemberRepository.existsByTeamIdAndUserIdAndIsActiveTrue(teamId, userId)) {
            throw BusinessException(ErrorCode.NOT_TEAM_MEMBER)
        }
    }

    private fun requireFeeManagement(teamId: Long, userId: Long) {
        val member = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
            ?: throw BusinessException(ErrorCode.NOT_TEAM_MEMBER)

        if (!member.isActive) {
            throw BusinessException(ErrorCode.NOT_TEAM_MEMBER)
        }

        if (member.role !in MANAGEMENT_ROLES) {
            throw BusinessException(ErrorCode.TEAM_ROLE_NOT_ALLOWED)
        }
    }

    companion object {
        private val MANAGEMENT_ROLES = listOf(TeamRole.CAPTAIN, TeamRole.MANAGER, TeamRole.ACCOUNTANT)
    }
}
