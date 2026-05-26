package com.dugout.api.domain.user.entity

import com.dugout.api.global.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "users",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["provider", "provider_id"]),
    ],
)
class User(
    @Column(unique = true)
    var email: String? = null,

    @Column(nullable = false, length = 50)
    var nickname: String,

    @Column(length = 20)
    var phone: String? = null,

    @Column(name = "profile_img_url", length = 500)
    var profileImgUrl: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val provider: AuthProvider,

    @Column(name = "provider_id", nullable = false)
    val providerId: String,

    @Column(name = "fcm_token", length = 500)
    var fcmToken: String? = null,
) : BaseEntity() {

    fun updateProfile(nickname: String, profileImgUrl: String?) {
        this.nickname = nickname
        this.profileImgUrl = profileImgUrl
    }

    companion object {
        fun create(
            provider: AuthProvider,
            providerId: String,
            nickname: String,
            email: String? = null,
            profileImgUrl: String? = null,
        ): User = User(
            provider = provider,
            providerId = providerId,
            nickname = nickname,
            email = email,
            profileImgUrl = profileImgUrl,
        )
    }
}
