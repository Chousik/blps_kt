package ru.chousik.blps_kt.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class XmlAccountRegistry(
    @Value("\${blps.security.accounts-location:classpath:security/accounts.xml}")
    private val accountsLocation: String
) {

    fun findByUsername(username: String): XmlAccountDefinition? =
        XmlAccountsSupport.loadAccounts(accountsLocation)
            .firstOrNull { it.username == username }

    fun getByUsername(username: String): XmlAccountDefinition =
        findByUsername(username)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "authenticated account not found in XML registry")

    fun appendAccount(account: XmlAccountDefinition) {
        XmlAccountsSupport.appendAccount(accountsLocation, account)
    }

    fun <T> withWriteLock(action: () -> T): T =
        XmlAccountsSupport.withWriteLock(action)
}
