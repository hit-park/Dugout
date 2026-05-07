package com.dugout.api.domain.ground.service

import com.dugout.api.domain.ground.dto.CreateGroundRequest
import com.dugout.api.domain.ground.dto.CreateGroundReviewRequest
import com.dugout.api.domain.ground.entity.FieldType
import com.dugout.api.domain.ground.entity.Ground
import com.dugout.api.domain.ground.entity.GroundReview
import com.dugout.api.domain.ground.repository.GroundRepository
import com.dugout.api.domain.ground.repository.GroundReviewRepository
import com.dugout.api.domain.user.entity.AuthProvider
import com.dugout.api.domain.user.entity.User
import com.dugout.api.domain.user.repository.UserRepository
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
import org.mockito.kotlin.whenever
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class GroundServiceTest {

    @Mock lateinit var groundRepository: GroundRepository
    @Mock lateinit var groundReviewRepository: GroundReviewRepository
    @Mock lateinit var userRepository: UserRepository

    private lateinit var service: GroundService

    @BeforeEach
    fun setUp() {
        service = GroundService(groundRepository, groundReviewRepository, userRepository)
    }

    private fun sampleGround(name: String = "잠실종합운동장 야구장") = Ground.create(
        name = name,
        address = "서울 송파구 올림픽로 25",
        latitude = 37.5121,
        longitude = 127.0723,
        fieldType = FieldType.NATURAL,
    )

    @Test
    fun `구장 생성 - 정상 등록`() {
        whenever(groundRepository.save(any<Ground>())).thenAnswer { it.getArgument<Ground>(0) }

        val response = service.createGround(
            CreateGroundRequest(
                name = "잠실종합운동장 야구장",
                address = "서울 송파구 올림픽로 25",
                latitude = 37.5121,
                longitude = 127.0723,
                fieldType = FieldType.NATURAL,
                hasLights = true,
            ),
        )

        assertEquals("잠실종합운동장 야구장", response.name)
        assertEquals("NATURAL", response.fieldType)
        assertEquals(true, response.hasLights)
    }

    @Test
    fun `구장 검색 - lat·lng·radius 일부 누락이면 INVALID_LOCATION_QUERY`() {
        val exception = assertThrows<BusinessException> {
            service.searchGrounds(lat = 37.5, lng = null, radiusKm = 5.0)
        }
        assertEquals(ErrorCode.INVALID_LOCATION_QUERY, exception.errorCode)
    }

    @Test
    fun `구장 검색 - radiusKm가 0 이하면 INVALID_LOCATION_QUERY`() {
        val exception = assertThrows<BusinessException> {
            service.searchGrounds(lat = 37.5, lng = 127.0, radiusKm = 0.0)
        }
        assertEquals(ErrorCode.INVALID_LOCATION_QUERY, exception.errorCode)
    }

    @Test
    fun `구장 단건 조회 - 없으면 GROUND_NOT_FOUND`() {
        whenever(groundRepository.findById(999L)).thenReturn(Optional.empty())

        val exception = assertThrows<BusinessException> { service.getGround(999L) }
        assertEquals(ErrorCode.GROUND_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `리뷰 생성 - 동일 사용자 중복이면 DUPLICATE_GROUND_REVIEW`() {
        val ground = sampleGround()
        whenever(groundRepository.findById(ground.id)).thenReturn(Optional.of(ground))
        whenever(groundReviewRepository.existsByGroundIdAndUserId(ground.id, 1L)).thenReturn(true)

        val exception = assertThrows<BusinessException> {
            service.createReview(1L, ground.id, CreateGroundReviewRequest(rating = 4))
        }
        assertEquals(ErrorCode.DUPLICATE_GROUND_REVIEW, exception.errorCode)
    }

    @Test
    fun `리뷰 생성 - 정상 생성 시 ground 평균·개수가 갱신됨`() {
        val ground = sampleGround()
        val user = User.create(AuthProvider.KAKAO, "kakao-1", "리뷰어")

        whenever(groundRepository.findById(ground.id)).thenReturn(Optional.of(ground))
        whenever(groundReviewRepository.existsByGroundIdAndUserId(ground.id, user.id)).thenReturn(false)
        whenever(userRepository.findById(user.id)).thenReturn(Optional.of(user))
        whenever(groundReviewRepository.save(any<GroundReview>())).thenAnswer {
            it.getArgument<GroundReview>(0)
        }
        whenever(groundReviewRepository.avgRatingByGroundId(ground.id)).thenReturn(4.5)
        whenever(groundReviewRepository.countByGroundId(ground.id)).thenReturn(2)

        val response = service.createReview(
            user.id,
            ground.id,
            CreateGroundReviewRequest(rating = 5, content = "조명 좋음"),
        )

        assertEquals(5, response.rating)
        assertEquals("조명 좋음", response.content)
        assertEquals(4.5, ground.avgRating)
        assertEquals(2, ground.reviewCount)
    }
}
