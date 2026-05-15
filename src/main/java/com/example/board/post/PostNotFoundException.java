package com.example.board.post;

public class PostNotFoundException extends RuntimeException {

    private final Long id;

    public PostNotFoundException(Long id) {
        super("Post not found: id=" + id);
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
