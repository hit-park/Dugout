package com.dugout.api.domain.user.dto

import com.dugout.api.domain.user.entity.User

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserResponse,
)

data class UserResponse(
    val id: Long,
    val email: String?,
    val nickname: String,
    val profileImgUrl: String?,
    val provider: String,
) {
    companion object {
        fun from(user: User): UserResponse = UserResponse(
            id = user.id,
            email = user.email,
            nickname = user.nickname,
            profileImgUrl = user.profileImgUrl,
            provider = user.provider.name,
        )
    }
}
