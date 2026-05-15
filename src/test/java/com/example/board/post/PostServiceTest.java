package com.example.board.post;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-05-16T01:23:45.678Z");

    @Mock
    private PostRepository postRepository;

    private PostService postService;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        postService = new PostService(postRepository, fixedClock);
    }

    @Test
    void create_persists_post_with_server_side_timestamp_and_zero_view_count() {
        PostCreateRequest request = new PostCreateRequest("alice", "Hello", "World");
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post toSave = invocation.getArgument(0);
            return toSave.withId(42L);
        });

        PostResponse response = postService.create(request);

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(captor.capture());
        Post saved = captor.getValue();
        assertThat(saved.getAuthor()).isEqualTo("alice");
        assertThat(saved.getTitle()).isEqualTo("Hello");
        assertThat(saved.getContent()).isEqualTo("World");
        assertThat(saved.getCreatedAt()).isEqualTo(FIXED_NOW);
        assertThat(saved.getUpdatedAt()).isEqualTo(FIXED_NOW);
        assertThat(saved.getViewCount()).isZero();

        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.author()).isEqualTo("alice");
        assertThat(response.title()).isEqualTo("Hello");
        assertThat(response.content()).isEqualTo("World");
        assertThat(response.createdAt()).isEqualTo(FIXED_NOW);
        assertThat(response.updatedAt()).isEqualTo(FIXED_NOW);
        assertThat(response.viewCount()).isZero();
    }

    @Test
    void list_passes_fixed_sort_pageable_and_maps_results_in_repository_order() {
        Post p1 = new Post("alice", "First", "Body1", FIXED_NOW).withId(1L);
        Post p2 = new Post("bob", "Second", "Body2", FIXED_NOW).withId(2L);
        Page<Post> repoPage = new PageImpl<>(List.of(p2, p1), PageRequest.of(0, 20), 2L);
        when(postRepository.findAll(any(Pageable.class))).thenReturn(repoPage);

        PageResponse<PostResponse> result = postService.list(0, 20);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(postRepository).findAll(pageableCaptor.capture());
        Pageable used = pageableCaptor.getValue();
        assertThat(used.getPageNumber()).isEqualTo(0);
        assertThat(used.getPageSize()).isEqualTo(20);
        assertThat(used.getSort())
                .isEqualTo(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

        assertThat(result.content())
                .extracting(PostResponse::id, PostResponse::title)
                .containsExactly(
                        org.assertj.core.api.Assertions.tuple(2L, "Second"),
                        org.assertj.core.api.Assertions.tuple(1L, "First")
                );
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(2L);
        assertThat(result.totalPages()).isEqualTo(1);
    }
}
