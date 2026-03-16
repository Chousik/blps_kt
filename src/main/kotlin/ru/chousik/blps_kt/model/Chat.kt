package ru.chousik.blps_kt.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PastOrPresent
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "chats")
class Chat {

    @Id
    lateinit var id: UUID

    @field:NotNull
    @field:PastOrPresent
    @Column(name = "created_at", nullable = false)
    lateinit var createdAt: OffsetDateTime

    @field:NotNull
    @field:PastOrPresent
    @Column(name = "updated_at", nullable = false)
    lateinit var updatedAt: OffsetDateTime

    @OneToMany(mappedBy = "chat")
    val messages: MutableList<ChatMessage> = mutableListOf()
}
