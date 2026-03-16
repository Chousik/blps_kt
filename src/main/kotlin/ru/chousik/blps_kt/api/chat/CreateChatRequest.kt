package ru.chousik.blps_kt.api.chat

import jakarta.validation.constraints.NotNull
import java.util.UUID

data class CreateChatRequest(
    @field:NotNull
    val guestUserId: UUID?,

    @field:NotNull
    val hostUserId: UUID?,

    @field:NotNull
    val initiatorUserId: UUID?
)
