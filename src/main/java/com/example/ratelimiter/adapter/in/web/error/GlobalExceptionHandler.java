package com.example.ratelimiter.adapter.in.web.error;

import com.example.ratelimiter.domain.exception.ConflictException;
import com.example.ratelimiter.domain.exception.NotFoundException;
import com.example.ratelimiter.domain.exception.RateLimiterUnavailableException;
import com.example.ratelimiter.domain.exception.UnknownConsumerException;
import com.example.ratelimiter.domain.exception.UpstreamTimeoutException;
import com.example.ratelimiter.domain.exception.UpstreamUnavailableException;
import com.example.ratelimiter.domain.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.Locale;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Maps every exception raised while handling a request to the uniform JSON error format and
 * logs it with its full stack trace (the log pattern adds class, method and line).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({ValidationException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class, ConstraintViolationException.class})
    public ResponseEntity<ErrorResponse> badRequest(Exception exception,
            HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, rootMessage(exception), exception, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> invalidBody(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> snakeCase(error.getField()) + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return respond(HttpStatus.BAD_REQUEST, message, exception, request);
    }

    @ExceptionHandler({NotFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ErrorResponse> notFound(Exception exception, HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, exception.getMessage(), exception, request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> methodNotAllowed(
            HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
        return respond(HttpStatus.METHOD_NOT_ALLOWED, exception.getMessage(), exception, request);
    }

    @ExceptionHandler(UnknownConsumerException.class)
    public ResponseEntity<ErrorResponse> forbidden(
            UnknownConsumerException exception, HttpServletRequest request) {
        return respond(HttpStatus.FORBIDDEN, exception.getMessage(), exception, request);
    }

    @ExceptionHandler({ConflictException.class, OptimisticLockingFailureException.class})
    public ResponseEntity<ErrorResponse> conflict(Exception exception, HttpServletRequest request) {
        return respond(HttpStatus.CONFLICT, exception.getMessage(), exception, request);
    }

    /** Database constraints and triggers are the last line of defence for invariants. */
    @ExceptionHandler({DataIntegrityViolationException.class, TransactionSystemException.class})
    public ResponseEntity<ErrorResponse> integrityViolation(
            Exception exception, HttpServletRequest request) {
        return respond(HttpStatus.CONFLICT,
                "Data integrity violation: " + rootMessage(exception), exception, request);
    }

    @ExceptionHandler(RateLimiterUnavailableException.class)
    public ResponseEntity<ErrorResponse> limiterUnavailable(
            RateLimiterUnavailableException exception, HttpServletRequest request) {
        return respond(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), exception, request);
    }

    @ExceptionHandler(UpstreamUnavailableException.class)
    public ResponseEntity<ErrorResponse> badGateway(
            UpstreamUnavailableException exception, HttpServletRequest request) {
        return respond(HttpStatus.BAD_GATEWAY, exception.getMessage(), exception, request);
    }

    @ExceptionHandler(UpstreamTimeoutException.class)
    public ResponseEntity<ErrorResponse> gatewayTimeout(
            UpstreamTimeoutException exception, HttpServletRequest request) {
        return respond(HttpStatus.GATEWAY_TIMEOUT, exception.getMessage(), exception, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unexpected(Exception exception,
            HttpServletRequest request) {
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", exception,
                request);
    }

    private static ResponseEntity<ErrorResponse> respond(
            HttpStatus status, String message, Exception exception, HttpServletRequest request) {
        logException(status, request, exception);
        return ResponseEntity.status(status).body(ErrorResponses.of(status, message, request));
    }

    private static void logException(
            HttpStatus status, HttpServletRequest request, Exception exception) {
        String summary = String.format("%s %s -> %d", request.getMethod(),
                request.getRequestURI(), status.value());
        if (status.is5xxServerError()) {
            LOG.error(summary, exception);
        } else {
            LOG.warn(summary, exception);
        }
    }

    /** Field names are reported as they appear in the JSON contract. */
    private static String snakeCase(String field) {
        return field.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }

    private static String rootMessage(Throwable exception) {
        Throwable root = exception;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage() == null ? exception.getMessage() : root.getMessage();
    }
}
