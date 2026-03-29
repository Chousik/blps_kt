package ru.chousik.blps_kt.service

import java.time.OffsetDateTime
import java.util.UUID
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Page
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.server.ResponseStatusException
import ru.chousik.blps_kt.api.chat.CreateChatRequest
import ru.chousik.blps_kt.model.Chat
import ru.chousik.blps_kt.model.User
import ru.chousik.blps_kt.model.UserRole
import ru.chousik.blps_kt.pagination.OffsetBasedPageRequest
import ru.chousik.blps_kt.repository.ChatRepository
import ru.chousik.blps_kt.repository.UserRepository

@Service
class ChatService(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val chatSystemMessageService: ChatSystemMessageService,
    @Qualifier("jtaTransactionTemplate")
    private val transactionTemplate: TransactionTemplate
) {

    fun createChat(request: CreateChatRequest): Chat {
        val createdChat = transactionTemplate.execute {
            val guest = loadUser(request.guestUserId!!)
            val host = loadUser(request.hostUserId!!)
            val initiator = loadUser(request.initiatorUserId!!)

            if (guest.id == host.id) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "guest and host must be different users")
            }

            requireRole(guest, UserRole.GUEST, "guest must have GUEST role")
            requireRole(host, UserRole.HOST, "host must have HOST role")

            if (!canInitiateChat(initiator, guest, host)) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "initiator must be guest, host, or platform user")
            }

            if (chatRepository.existsByGuestIdAndHostId(guest.id, host.id)) {
                throw ResponseStatusException(HttpStatus.CONFLICT, "chat between guest and host already exists")
            }

            val now = OffsetDateTime.now()
            val chat = Chat().apply {
                id = UUID.randomUUID()
                this.guest = guest
                this.host = host
                createdAt = now
                updatedAt = now
            }
            val savedChat = chatRepository.save(chat)
            chatSystemMessageService.append(
                chat = savedChat,
                message = "Chat has been created for guest ${guest.username} and host ${host.username}."
            )
            savedChat
        }
        return requireNotNull(createdChat) { "chat creation transaction returned null result" }
    }

    fun getChatForUser(chatId: UUID, requesterId: UUID): Chat {
        val chat = chatRepository.findById(chatId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "chat not found") }
        val requester = loadUser(requesterId)

        if (!canAccessChat(chat, requester)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "user cannot access this chat")
        }

        return chat
    }

    @Transactional(readOnly = true)
    fun getChatsForUser(userId: UUID, requesterId: UUID, limit: Int, offset: Long): Page<Chat> {
        val user = loadUser(userId)
        val requester = loadUser(requesterId)

        if (requester.role != UserRole.PLATFORM && requester.id != user.id) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "only the user or platform can view chats")
        }

        val pageable = OffsetBasedPageRequest(limit, offset, Sort.by(Sort.Direction.DESC, "createdAt"))
        return chatRepository.findAllByGuestIdOrHostId(user.id, user.id, pageable)
    }

    private fun loadUser(userId: UUID): User =
        userRepository.findById(userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "user not found") }

    private fun requireRole(user: User, expectedRole: UserRole, message: String) {
        if (user.role != expectedRole) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, message)
        }
    }

    private fun canInitiateChat(initiator: User, guest: User, host: User): Boolean {
        return when (initiator.role) {
            UserRole.GUEST -> initiator.id == guest.id
            UserRole.HOST -> initiator.id == host.id
            UserRole.PLATFORM -> true
        }
    }

    private fun canAccessChat(chat: Chat, requester: User): Boolean {
        return when (requester.role) {
            UserRole.GUEST -> requester.id == chat.guest.id
            UserRole.HOST -> requester.id == chat.host.id
            UserRole.PLATFORM -> true
        }
    }
}
