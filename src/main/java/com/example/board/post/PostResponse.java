package com.example.board.post;

import java.time.Instant;

public record PostResponse(
        Long id,
        String author,
        String title,
        String content,
        Instant createdAt,
        Instant updatedAt,
        long viewCount
) {
    public static PostResponse from(Post post) {
        return new PostResponse(
                post.getId(),
                post.getAuthor(),
                post.getTitle(),
                post.getContent(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getViewCount()
        );
    }
}
