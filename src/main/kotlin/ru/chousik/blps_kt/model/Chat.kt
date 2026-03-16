package ru.chousik.blps_kt.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PastOrPresent
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(
    name = "chats",
    uniqueConstraints = [UniqueConstraint(columnNames = ["guest_user_id", "host_user_id"])]
)
class Chat {

    @Id
    lateinit var id: UUID

    @field:NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guest_user_id", nullable = false)
    lateinit var guest: User

    @field:NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "host_user_id", nullable = false)
    lateinit var host: User

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
