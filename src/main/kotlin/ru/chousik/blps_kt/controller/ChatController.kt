package ru.chousik.blps_kt.controller

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.util.UUID
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.chousik.blps_kt.api.chat.ChatResponse
import ru.chousik.blps_kt.api.chat.CreateChatRequest
import ru.chousik.blps_kt.api.chat.PagedChatsResponse
import ru.chousik.blps_kt.service.ChatService

@RestController
@Validated
@RequestMapping
class ChatController(
    private val chatService: ChatService
) {

    @PostMapping("/chats")
    fun createChat(@Valid @RequestBody request: CreateChatRequest): ChatResponse {
        val chat = chatService.createChat(request)
        return ChatResponse.from(chat)
    }

    @GetMapping("/chats/{chatId}")
    fun getChat(
        @PathVariable chatId: UUID,
        @RequestParam("requesterUserId") requesterUserId: UUID
    ): ChatResponse {
        val chat = chatService.getChatForUser(chatId, requesterUserId)
        return ChatResponse.from(chat)
    }

    @GetMapping("/chats")
    fun getUserChats(
        @RequestParam("userId") userId: UUID,
        @RequestParam("requesterUserId") requesterUserId: UUID,
        @RequestParam("limit", defaultValue = "20") @Min(1) @Max(100) limit: Int,
        @RequestParam("offset", defaultValue = "0") @Min(0) offset: Long
    ): PagedChatsResponse {
        val page = chatService.getChatsForUser(userId, requesterUserId, limit, offset)
        return PagedChatsResponse(
            items = page.content.map { ChatResponse.from(it) },
            total = page.totalElements,
            limit = limit,
            offset = offset
        )
    }
}
