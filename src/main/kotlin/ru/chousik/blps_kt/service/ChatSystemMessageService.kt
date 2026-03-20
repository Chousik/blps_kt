package ru.chousik.blps_kt.service

import java.time.OffsetDateTime
import java.util.UUID
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import ru.chousik.blps_kt.api.chat.ChatMessageResponse
import ru.chousik.blps_kt.model.Chat
import ru.chousik.blps_kt.model.ChatMessage
import ru.chousik.blps_kt.repository.ChatMessageRepository

@Service
class ChatSystemMessageService(
    private val chatMessageRepository: ChatMessageRepository,
    private val messagingTemplate: SimpMessagingTemplate
) {

    fun append(chat: Chat, message: String) {
        val entity = ChatMessage().apply {
            id = UUID.randomUUID()
            this.chat = chat
            senderUser = null
            this.message = message
            createdAt = OffsetDateTime.now()
        }
        val saved = chatMessageRepository.save(entity)
        messagingTemplate.convertAndSend("/topic/chats/${chat.id}", ChatMessageResponse.from(saved))
    }
}
