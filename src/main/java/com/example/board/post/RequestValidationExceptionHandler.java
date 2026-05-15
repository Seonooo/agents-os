package com.example.board.post;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@ControllerAdvice
class RequestValidationExceptionHandler extends ResponseEntityExceptionHandler {

    private final Clock clock;

    RequestValidationExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(
                        fe.getField(),
                        fe.getDefaultMessage() == null ? "" : fe.getDefaultMessage()))
                .toList();
        return validationResponse(fieldErrors, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getParameterValidationResults().stream()
                .flatMap(result -> {
                    String field = result.getMethodParameter().getParameterName();
                    String safeField = field == null ? "" : field;
                    return result.getResolvableErrors().stream()
                            .map(error -> new ErrorResponse.FieldError(
                                    safeField,
                                    error.getDefaultMessage() == null ? "" : error.getDefaultMessage()));
                })
                .toList();
        return validationResponse(fieldErrors, status, request);
    }

    private ResponseEntity<Object> validationResponse(
            List<ErrorResponse.FieldError> fieldErrors,
            HttpStatusCode status,
            WebRequest request) {
        ErrorResponse body = new ErrorResponse(
                ErrorCode.VALIDATION_FAILED,
                "Request validation failed",
                pathOf(request),
                Instant.now(clock),
                fieldErrors
        );
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(body, responseHeaders, status);
    }

    private String pathOf(WebRequest request) {
        if (request instanceof ServletWebRequest servlet) {
            return servlet.getRequest().getRequestURI();
        }
        return "";
    }
}
