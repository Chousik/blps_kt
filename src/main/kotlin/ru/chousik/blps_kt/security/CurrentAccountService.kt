package ru.chousik.blps_kt.security

import java.security.Principal
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class CurrentAccountService {

    fun currentAccount(): AuthenticatedAccount =
        fromAuthentication(SecurityContextHolder.getContext().authentication)

    fun fromPrincipal(principal: Principal?): AuthenticatedAccount =
        when (principal) {
            is Authentication -> fromAuthentication(principal)
            is AuthenticatedAccount -> principal
            else -> throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "authenticated principal is required")
        }

    fun fromAuthentication(authentication: Authentication?): AuthenticatedAccount {
        if (authentication == null || !authentication.isAuthenticated) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "authentication is required")
        }

        val principal = authentication.principal
        return when (principal) {
            is AuthenticatedAccount -> principal
            else -> throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "unsupported authenticated principal")
        }
    }
}

