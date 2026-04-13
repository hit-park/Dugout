package com.dugout.api.domain.user.repository

import com.dugout.api.domain.user.entity.AuthProvider
import com.dugout.api.domain.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    fun findByProviderAndProviderId(provider: AuthProvider, providerId: String): User?
}
