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
import ru.chousik.blps_kt.api.payment.ExtraServiceDecision
import ru.chousik.blps_kt.api.payment.ExtraServiceDecisionRequest
import ru.chousik.blps_kt.api.payment.ExtraServiceDecisionResponse
import ru.chousik.blps_kt.api.payment.PaymentRequestView
import ru.chousik.blps_kt.api.extraservice.ExtraServiceRequestCreateDTO
import ru.chousik.blps_kt.api.extraservice.ExtraServiceRequestResponseDTO
import ru.chousik.blps_kt.api.extraservice.ExtraServiceRequestUpdateDTO
import ru.chousik.blps_kt.model.Chat
import ru.chousik.blps_kt.model.ExtraServiceRequest
import ru.chousik.blps_kt.model.ExtraServiceRequestStatus
import ru.chousik.blps_kt.model.PaymentRequest
import ru.chousik.blps_kt.model.PaymentRequestStatus
import ru.chousik.blps_kt.model.User
import ru.chousik.blps_kt.model.UserRole
import ru.chousik.blps_kt.pagination.OffsetBasedPageRequest
import ru.chousik.blps_kt.repository.ChatRepository
import ru.chousik.blps_kt.repository.ExtraServiceRequestRepository
import ru.chousik.blps_kt.repository.PaymentRequestRepository
import ru.chousik.blps_kt.repository.UserRepository
import ru.chousik.blps_kt.service.payment.YooKassaClient

@Service
class ExtraServiceRequestService(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val extraServiceRequestRepository: ExtraServiceRequestRepository,
    private val paymentRequestRepository: PaymentRequestRepository,
    private val chatSystemMessageService: ChatSystemMessageService,
    private val yooKassaClient: YooKassaClient
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

        val saved = extraServiceRequestRepository.save(entity)
        chatSystemMessageService.append(
            chat = chat,
            message = "Host proposed extra service '${saved.title}' for ${saved.amount} ${saved.currency}."
        )
        return ExtraServiceRequestResponseDTO.from(saved)
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

    @Transactional(readOnly = true)
    fun getExtraServicePayment(serviceId: UUID, requesterId: UUID): PaymentRequestView {
        val service = loadExtraService(serviceId)
        val requester = loadUser(requesterId)
        ensureParticipantOrSupport(service.chat, requester)

        val payment = paymentRequestRepository.findFirstByExtraServiceRequestIdOrderByCreatedAtDesc(service.id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "payment request not found for extra service")

        return PaymentRequestView.from(payment)
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

    @Transactional
    fun decideExtraService(
        serviceId: UUID,
        requesterId: UUID,
        request: ExtraServiceDecisionRequest
    ): ExtraServiceDecisionResponse {
        val service = loadExtraService(serviceId)
        val requester = loadUser(requesterId)
        ensureGuestDecisionAllowed(service, requester)

        if (service.status != ExtraServiceRequestStatus.WAITING_GUEST_APPROVAL) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "extra service decision is only allowed from WAITING_GUEST_APPROVAL status"
            )
        }

        return when (request.decision!!) {
            ExtraServiceDecision.REJECT -> rejectExtraService(service)
            ExtraServiceDecision.ACCEPT -> acceptExtraService(service, requester)
        }
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

    private fun ensureGuestDecisionAllowed(service: ExtraServiceRequest, requester: User) {
        val allowed = requester.role == UserRole.GUEST && requester.id == service.chat.guest.id
        if (!allowed) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "only guest of the chat can decide on extra service")
        }
    }

    private fun rejectExtraService(service: ExtraServiceRequest): ExtraServiceDecisionResponse {
        service.status = ExtraServiceRequestStatus.REJECTED
        service.updatedAt = OffsetDateTime.now()
        val saved = extraServiceRequestRepository.save(service)
        chatSystemMessageService.append(
            chat = saved.chat,
            message = "Guest rejected extra service '${saved.title}'."
        )
        return ExtraServiceDecisionResponse(
            extraService = ExtraServiceRequestResponseDTO.from(saved)
        )
    }

    private fun acceptExtraService(service: ExtraServiceRequest, requester: User): ExtraServiceDecisionResponse {
        val existingPayment = paymentRequestRepository.findFirstByExtraServiceRequestIdOrderByCreatedAtDesc(service.id)
        if (existingPayment != null) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "payment request for this extra service already exists"
            )
        }

        val now = OffsetDateTime.now()
        val paymentRequestId = UUID.randomUUID()
        val createdPayment = yooKassaClient.createPayment(paymentRequestId, service, requester)
        val payment = PaymentRequest().apply {
            id = paymentRequestId
            extraServiceRequestId = service.id
            initiatedBy = requester
            providerPaymentId = createdPayment.providerPaymentId
            paymentUrl = createdPayment.paymentUrl
            status = mapProviderPaymentStatus(createdPayment.providerStatus)
            createdAt = now
            expiresAt = createdPayment.expiresAt
        }

        service.status = mapExtraServiceStatus(createdPayment.providerStatus)
        service.updatedAt = now

        val savedService = extraServiceRequestRepository.save(service)
        val savedPayment = paymentRequestRepository.save(payment)

        chatSystemMessageService.append(
            chat = savedService.chat,
            message = "Guest accepted extra service '${savedService.title}'. Payment request created."
        )

        return ExtraServiceDecisionResponse(
            extraService = ExtraServiceRequestResponseDTO.from(savedService),
            payment = PaymentRequestView.from(savedPayment)
        )
    }

    private fun mapProviderPaymentStatus(providerStatus: String): PaymentRequestStatus =
        when (providerStatus.lowercase()) {
            "pending" -> PaymentRequestStatus.PENDING
            "succeeded" -> PaymentRequestStatus.PAID
            "canceled" -> PaymentRequestStatus.FAILED
            else -> PaymentRequestStatus.WAITING_PAYMENT
        }

    private fun mapExtraServiceStatus(providerStatus: String): ExtraServiceRequestStatus =
        when (providerStatus.lowercase()) {
            "pending" -> ExtraServiceRequestStatus.PAYMENT_LINK_SENT
            "succeeded" -> ExtraServiceRequestStatus.PAID
            "canceled" -> ExtraServiceRequestStatus.PAYMENT_FAILED
            else -> ExtraServiceRequestStatus.PAYMENT_LINK_SENT
        }

}
