package com.example.board.post;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String author;

    @Column(nullable = false, length = 80)
    private String title;

    @Column(nullable = false, length = 5000)
    private String content;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private long viewCount;

    protected Post() {
    }

    public Post(String author, String title, String content, Instant now) {
        this.author = author;
        this.title = title;
        this.content = content;
        this.createdAt = now;
        this.updatedAt = now;
        this.viewCount = 0L;
    }

    private Post(Long id, String author, String title, String content,
                 Instant createdAt, Instant updatedAt, long viewCount) {
        this.id = id;
        this.author = author;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.viewCount = viewCount;
    }

    public Post withId(Long newId) {
        return new Post(newId, this.author, this.title, this.content,
                this.createdAt, this.updatedAt, this.viewCount);
    }

    public Long getId() {
        return id;
    }

    public String getAuthor() {
        return author;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getViewCount() {
        return viewCount;
    }
}
