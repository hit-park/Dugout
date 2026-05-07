package com.dugout.api.domain.mercenary.entity

import com.dugout.api.domain.user.entity.User
import com.dugout.api.global.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "mercenary_applications",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["request_id", "user_id"]),
    ],
)
class MercenaryApplication(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    val request: MercenaryRequest,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false, length = 10)
    var position: String,

    @Column(columnDefinition = "TEXT")
    var memo: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: MercenaryApplicationStatus = MercenaryApplicationStatus.PENDING,
) : BaseEntity() {

    fun accept() {
        this.status = MercenaryApplicationStatus.ACCEPTED
    }

    fun reject() {
        this.status = MercenaryApplicationStatus.REJECTED
    }

    companion object {
        fun create(
            request: MercenaryRequest,
            user: User,
            position: String,
            memo: String? = null,
        ): MercenaryApplication = MercenaryApplication(
            request = request,
            user = user,
            position = position,
            memo = memo,
        )
    }
}
