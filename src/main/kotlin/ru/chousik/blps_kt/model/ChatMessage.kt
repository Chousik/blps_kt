package ru.chousik.blps_kt.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PastOrPresent
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "chat_messages")
class ChatMessage {

    @Id
    lateinit var id: UUID

    @field:NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_id", nullable = false)
    lateinit var chat: Chat

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_user_id")
    var senderUser: User? = null

    @field:NotBlank
    @field:Size(max = 2000)
    @Column(name = "message", nullable = false, length = 2000)
    lateinit var message: String

    @field:NotNull
    @field:PastOrPresent
    @Column(name = "created_at", nullable = false)
    lateinit var createdAt: OffsetDateTime
}
