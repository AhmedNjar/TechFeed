package com.techhub

import com.techhub.config.ApiProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.stereotype.Component

@SpringBootApplication(scanBasePackages = ["com.techhub"])
@EnableScheduling
@EnableAsync
open class TechIntelligenceHubApplication

fun main(args: Array<String>) {
    runApplication<TechIntelligenceHubApplication>(*args)
}

/**
 * Startup guard — warns loudly if the insecure default admin/extension
 * secret is still set, so it doesn't go unnoticed into a real deployment.
 */
@Component
class StartupValidator(private val apiProperties: ApiProperties) {

    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun validateSecrets() {
        if (apiProperties.chromeExtensionSecret == "dev-secret-change-in-prod") {
            log.warn("WARNING: CHROME_SECRET is using the default insecure value!")
            log.warn("Set the CHROME_SECRET environment variable before deploying.")
            log.warn("The Chrome Extension save and Admin endpoints are effectively unprotected.")
        } else {
            log.info("Chrome extension / admin secret is configured.")
        }
    }
}
