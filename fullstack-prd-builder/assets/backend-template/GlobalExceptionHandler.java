package {{basePackage}}.exception;

import {{basePackage}}.common.Result;
{{#jdk17}}
import jakarta.validation.ConstraintViolationException;
{{/jdk17}}
{{^jdk17}}
import javax.validation.ConstraintViolationException;
{{/jdk17}}
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
{{#jdk17}}
import org.springframework.web.servlet.resource.NoResourceFoundException;
{{/jdk17}}

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ============================================================
    // 400 Bad Request — Client input / validation errors
    // ============================================================

    /**
     * @Valid on @RequestBody DTO fails validation (field-level annotations).
     * Returns the first error message per field, joined by comma.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", msg);
        return Result.error(400, msg);
    }

    /**
     * @Valid on a form-bind target (rare in REST APIs, but covered).
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBind(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("Bind validation failed: {}", msg);
        return Result.error(400, msg);
    }

    /**
     * @Validated / @Valid on path variables or query parameters (class-level validation).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining(", "));
        log.warn("Constraint violation: {}", msg);
        return Result.error(400, msg);
    }

    /**
     * Request body is missing, unparseable, or contains malformed JSON.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("Malformed request body: {}", e.getMessage());
        return Result.error(400, "Malformed request body. Please check your JSON syntax.");
    }

    /**
     * Required query parameter or form field is missing.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingServletRequestParameter(MissingServletRequestParameterException e) {
        log.warn("Missing required parameter '{}' (type: {})", e.getParameterName(), e.getParameterType());
        return Result.error(400, String.format("Required parameter '%s' is missing.", e.getParameterName()));
    }

    /**
     * Type conversion failure — e.g. sending "abc" where a Long is expected.
     */
    @ExceptionHandler(TypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleTypeMismatch(TypeMismatchException e) {
        String msg = String.format("Type mismatch for '%s': expected %s.",
                e.getPropertyName(),
                e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "unknown");
        log.warn("Type mismatch: {}", msg);
        return Result.error(400, msg);
    }

    /**
     * Invalid method argument; generic fallback for argument-level errors not caught above.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Illegal argument: {}", e.getMessage());
        return Result.error(400, e.getMessage());
    }

    // ============================================================
    // 403 Forbidden — Authorization
    // ============================================================

    /**
     * Spring Security access denied (no permission / role mismatch).
     * If Spring Security is not in the project this handler is simply never called.
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDenied(org.springframework.security.access.AccessDeniedException e) {
        log.warn("Access denied: {}", e.getMessage());
        return Result.error(403, "Access denied. You do not have permission to perform this action.");
    }

    // ============================================================
    // 404 Not Found
    // ============================================================

    /**
     * No handler mapped for the requested URL (DispatcherServlet-level).
     * Requires spring.mvc.throw-exception-if-no-handler-found=true (enforced by default in dev profile).
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNoHandlerFound(NoHandlerFoundException e) {
        log.warn("No handler found for {} {}", e.getHttpMethod(), e.getRequestURL());
        return Result.error(404, String.format("Endpoint %s %s not found.", e.getHttpMethod(), e.getRequestURL()));
    }

    {{#jdk17}}
    /**
     * Static resource not found (Spring Boot 3.x / Spring Framework 6.x).
     * For example, a missing favicon.ico or CSS file — treated as 404 rather than 500.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNoResourceFound(NoResourceFoundException e) {
        log.warn("Resource not found: {}", e.getMessage());
        return Result.error(404, "Resource not found.");
    }
    {{/jdk17}}

    // ============================================================
    // 405 Method Not Allowed
    // ============================================================

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("Method {} not allowed for this endpoint", e.getMethod());
        return Result.error(405, String.format("HTTP method '%s' is not supported. Supported: %s.",
                e.getMethod(), e.getSupportedHttpMethods()));
    }

    // ============================================================
    // 415 Unsupported Media Type
    // ============================================================

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public Result<Void> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        log.warn("Unsupported media type: {}", e.getContentType());
        return Result.error(415, String.format("Content-Type '%s' is not supported. Supported: %s.",
                e.getContentType(), e.getSupportedMediaTypes()));
    }

    // ============================================================
    // 409 Conflict — Data integrity failures
    // ============================================================

    /**
     * Database constraint violation (unique key, not null, check constraint, etc.).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Result<Void> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.error("Data integrity violation", e);
        Throwable root = e.getRootCause();
        String detail = root != null ? root.getMessage() : e.getMessage();
        return Result.error(409, "Data conflict: " + (detail != null ? detail : "A database constraint has been violated."));
    }

    /**
     * Duplicate key (unique index violation). Subclass of DataIntegrityViolationException.
     * Handle separately for a friendlier message.
     */
    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Result<Void> handleDuplicateKey(DuplicateKeyException e) {
        log.warn("Duplicate key violation: {}", e.getMessage());
        return Result.error(409, "Duplicate record detected. This record already exists.");
    }

    // ============================================================
    // 500 Internal Server Error — Server-side failures
    // ============================================================

    /**
     * Failed to serialize the response body to JSON.
     */
    @ExceptionHandler(HttpMessageNotWritableException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleHttpMessageNotWritable(HttpMessageNotWritableException e) {
        log.error("Failed to serialize response body", e);
        return Result.error(500, "Internal error while writing the response. Please contact support.");
    }

    /**
     * Generic runtime exception — catch-all for unanticipated errors.
     * Exposes the exception message in dev/test profiles; in production profiles
     * the message should be generic. (Application-level code can override this
     * by throwing a BusinessException instead.)
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleRuntime(RuntimeException e) {
        log.error("Unhandled runtime exception", e);
        return Result.error(500, e.getMessage() != null ? e.getMessage() : "Internal server error.");
    }

    /**
     * Ultimate catch-all for any exception not explicitly handled above.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return Result.error(500, "Internal server error. Please contact support.");
    }

}
