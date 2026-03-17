package ru.chousik.blps_kt.config

import java.util.UUID
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.MessageDeliveryException
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.stereotype.Component
import ru.chousik.blps_kt.service.ChatMessageService

@Component
class WebSocketAuthChannelInterceptor(
    private val chatMessageService: ChatMessageService
) : ChannelInterceptor {

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
        val accessor = StompHeaderAccessor.wrap(message)
        val destination = accessor.destination ?: return message

        when (accessor.command) {
            StompCommand.SEND -> {
                if (destination.startsWith("/app/chats/") && destination.endsWith("/messages")) {
                    chatMessageService.requireWriteAccess(
                        extractChatId(destination, "/app/chats/", "/messages"),
                        extractRequesterId(accessor)
                    )
                }
            }

            StompCommand.SUBSCRIBE -> {
                if (destination.startsWith("/topic/chats/")) {
                    chatMessageService.requireReadAccess(
                        extractChatId(destination, "/topic/chats/", ""),
                        extractRequesterId(accessor)
                    )
                }
            }

            else -> Unit
        }

        return message
    }

    private fun extractRequesterId(accessor: StompHeaderAccessor): UUID {
        val requesterUserId = accessor.getFirstNativeHeader("requesterUserId")
            ?: throw MessageDeliveryException("requesterUserId header is required")
        return try {
            UUID.fromString(requesterUserId)
        } catch (_: IllegalArgumentException) {
            throw MessageDeliveryException("requesterUserId must be a valid UUID")
        }
    }

    private fun extractChatId(destination: String, prefix: String, suffix: String): UUID {
        val value = destination.removePrefix(prefix).removeSuffix(suffix)
        return try {
            UUID.fromString(value)
        } catch (_: IllegalArgumentException) {
            throw MessageDeliveryException("chatId in destination must be a valid UUID")
        }
    }
}
