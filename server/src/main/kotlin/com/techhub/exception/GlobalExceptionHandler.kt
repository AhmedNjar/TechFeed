package com.techhub.exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

// FIX: this file did not exist anywhere in the project. Without it,
// validation errors (e.g. from SavePostRequest's @NotBlank/@URL constraints)
// and any unhandled exception return Spring Boot's default error page/JSON
// shape instead of a clean, consistent response.
@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors
            .associate { it.field to (it.defaultMessage ?: "invalid") }
        return ResponseEntity
            .badRequest()
            .body(ErrorResponse(
                status  = 400,
                error   = "Validation Failed",
                message = errors.toString(),
            ))
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneral(ex: Exception): ResponseEntity<ErrorResponse> {
        log.error("Unhandled exception: ${ex.message}", ex)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse(
                status  = 500,
                error   = "Internal Server Error",
                message = "Something went wrong. Please try again.",
            ))
    }
}

data class ErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
    val timestamp: Instant = Instant.now(),
)
