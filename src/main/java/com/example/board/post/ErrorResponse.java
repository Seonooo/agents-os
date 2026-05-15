package com.example.board.post;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        ErrorCode code,
        String message,
        String path,
        Instant timestamp,
        List<FieldError> errors
) {
    public record FieldError(String field, String message) {
    }
}
