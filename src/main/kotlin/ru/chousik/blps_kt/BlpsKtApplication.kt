package ru.chousik.blps_kt

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class BlpsKtApplication

fun main(args: Array<String>) {
    runApplication<BlpsKtApplication>(*args)
}
