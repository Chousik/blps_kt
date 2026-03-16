package ru.chousik.blps_kt.service

import java.math.RoundingMode
import java.time.OffsetDateTime
import java.util.UUID
import org.springframework.data.domain.Page
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import ru.chousik.blps_kt.api.extraservice.ExtraServiceRequestCreateDTO
import ru.chousik.blps_kt.api.extraservice.ExtraServiceRequestResponseDTO
import ru.chousik.blps_kt.api.extraservice.ExtraServiceRequestUpdateDTO
import ru.chousik.blps_kt.model.Chat
import ru.chousik.blps_kt.model.ExtraServiceRequest
import ru.chousik.blps_kt.model.ExtraServiceRequestStatus
import ru.chousik.blps_kt.model.User
import ru.chousik.blps_kt.model.UserRole
import ru.chousik.blps_kt.pagination.OffsetBasedPageRequest
import ru.chousik.blps_kt.repository.ChatRepository
import ru.chousik.blps_kt.repository.ExtraServiceRequestRepository
import ru.chousik.blps_kt.repository.UserRepository

@Service
class ExtraServiceRequestService(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val extraServiceRequestRepository: ExtraServiceRequestRepository
) {

    @Transactional
    fun createExtraService(
        chatId: UUID,
        requesterId: UUID,
        dto: ExtraServiceRequestCreateDTO
    ): ExtraServiceRequestResponseDTO {
        val chat = loadChat(chatId)
        val requester = loadUser(requesterId)

        ensureHostRequester(chat, requester)

        val now = OffsetDateTime.now()
        val entity = ExtraServiceRequest().apply {
            id = UUID.randomUUID()
            this.chat = chat
            status = ExtraServiceRequestStatus.WAITING_GUEST_APPROVAL
            title = dto.title!!.trim()
            description = dto.description!!.trim()
            amount = dto.amount!!.setScale(2, RoundingMode.HALF_UP)
            currency = dto.currency!!.uppercase()
            createdAt = now
            updatedAt = now
        }

        return ExtraServiceRequestResponseDTO.from(extraServiceRequestRepository.save(entity))
    }

    @Transactional(readOnly = true)
    fun getExtraServicesForChat(
        chatId: UUID,
        requesterId: UUID,
        limit: Int,
        offset: Long
    ): Page<ExtraServiceRequestResponseDTO> {
        val chat = loadChat(chatId)
        val requester = loadUser(requesterId)
        ensureParticipantOrSupport(chat, requester)

        val pageable = OffsetBasedPageRequest(limit, offset, Sort.by(Sort.Direction.DESC, "createdAt"))
        return extraServiceRequestRepository.findAllByChatId(chatId, pageable)
            .map { ExtraServiceRequestResponseDTO.from(it) }
    }

    @Transactional(readOnly = true)
    fun getExtraService(serviceId: UUID, requesterId: UUID): ExtraServiceRequestResponseDTO {
        val service = loadExtraService(serviceId)
        val requester = loadUser(requesterId)
        ensureParticipantOrSupport(service.chat, requester)
        return ExtraServiceRequestResponseDTO.from(service)
    }

    @Transactional
    fun updateExtraService(
        serviceId: UUID,
        requesterId: UUID,
        dto: ExtraServiceRequestUpdateDTO
    ): ExtraServiceRequestResponseDTO {
        val service = loadExtraService(serviceId)
        val requester = loadUser(requesterId)
        ensureHostOrSupport(service.chat, requester)

        dto.title?.let { service.title = it.trim() }
        dto.description?.let { service.description = it.trim() }
        dto.amount?.let { service.amount = it.setScale(2, RoundingMode.HALF_UP) }
        dto.currency?.let { service.currency = it.uppercase() }
        dto.status?.let { service.status = it }
        service.updatedAt = OffsetDateTime.now()

        return ExtraServiceRequestResponseDTO.from(extraServiceRequestRepository.save(service))
    }

    @Transactional
    fun deleteExtraService(serviceId: UUID, requesterId: UUID) {
        val service = loadExtraService(serviceId)
        val requester = loadUser(requesterId)
        ensureHostOrSupport(service.chat, requester)
        extraServiceRequestRepository.delete(service)
    }

    private fun loadChat(chatId: UUID): Chat =
        chatRepository.findById(chatId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "chat not found") }

    private fun loadUser(userId: UUID): User =
        userRepository.findById(userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "user not found") }

    private fun loadExtraService(serviceId: UUID): ExtraServiceRequest =
        extraServiceRequestRepository.findById(serviceId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "extra service not found") }

    private fun ensureHostRequester(chat: Chat, requester: User) {
        if (requester.role != UserRole.HOST || requester.id != chat.host.id) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "only host of the chat can create extra services")
        }
    }

    private fun ensureParticipantOrSupport(chat: Chat, requester: User) {
        val allowed = requester.role == UserRole.PLATFORM ||
            requester.id == chat.host.id || requester.id == chat.guest.id
        if (!allowed) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "access denied to extra services")
        }
    }

    private fun ensureHostOrSupport(chat: Chat, requester: User) {
        val allowed = (requester.role == UserRole.HOST && requester.id == chat.host.id) ||
            requester.role == UserRole.PLATFORM
        if (!allowed) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "only host or support can modify extra services")
        }
    }
}
