package com.example.board.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostUpdateRequest(
        @NotBlank @Size(max = 80) String title,
        @NotBlank @Size(max = 5000) String content
) {
}
