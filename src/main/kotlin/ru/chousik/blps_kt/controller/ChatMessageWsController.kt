package ru.chousik.blps_kt.controller

import jakarta.validation.Valid
import java.util.UUID
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller
import org.springframework.validation.annotation.Validated
import ru.chousik.blps_kt.api.chat.CreateChatMessageRequest
import ru.chousik.blps_kt.service.ChatMessageService

@Controller
@Validated
class ChatMessageWsController(
    private val chatMessageService: ChatMessageService,
    private val messagingTemplate: SimpMessagingTemplate
) {

    @MessageMapping("/chats/{chatId}/messages")
    fun sendMessage(
        @DestinationVariable chatId: UUID,
        @Header("requesterUserId") requesterUserId: UUID,
        @Valid @Payload request: CreateChatMessageRequest
    ) {
        val response = chatMessageService.createMessage(chatId, requesterUserId, request)
        messagingTemplate.convertAndSend("/topic/chats/$chatId", response)
    }
}
