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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
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

        PageResponse<PostResponse> result = postService.list(0, 20, null);

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

    @Test
    void list_with_blank_query_delegates_to_findAll_not_search() {
        Page<Post> repoPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0L);
        when(postRepository.findAll(any(Pageable.class))).thenReturn(repoPage);

        postService.list(0, 20, "   ");

        verify(postRepository).findAll(any(Pageable.class));
        verify(postRepository, never()).findByTitleContainingIgnoreCase(any(), any(Pageable.class));
    }

    @Test
    void list_with_query_calls_findByTitleContainingIgnoreCase_with_fixed_sort_and_maps() {
        Post matching = new Post("alice", "Spring Boot 4", "irrelevant body", FIXED_NOW).withId(7L);
        Page<Post> repoPage = new PageImpl<>(List.of(matching), PageRequest.of(0, 20), 1L);
        when(postRepository.findByTitleContainingIgnoreCase(eq("spring"), any(Pageable.class)))
                .thenReturn(repoPage);

        PageResponse<PostResponse> result = postService.list(0, 20, "spring");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(postRepository).findByTitleContainingIgnoreCase(eq("spring"), pageableCaptor.capture());
        Pageable used = pageableCaptor.getValue();
        assertThat(used.getSort())
                .isEqualTo(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

        verify(postRepository, never()).findAll(any(Pageable.class));

        assertThat(result.content())
                .extracting(PostResponse::id, PostResponse::title)
                .containsExactly(org.assertj.core.api.Assertions.tuple(7L, "Spring Boot 4"));
        assertThat(result.totalElements()).isEqualTo(1L);
    }

    @Test
    void find_increments_view_count_then_fetches_and_returns_mapped_response() {
        Long id = 42L;
        Post fetched = new Post("alice", "Hello", "World", FIXED_NOW).withId(id);
        when(postRepository.incrementViewCount(id)).thenReturn(1);
        when(postRepository.findById(id)).thenReturn(Optional.of(fetched));

        PostResponse response = postService.find(id);

        org.mockito.InOrder order = inOrder(postRepository);
        order.verify(postRepository).incrementViewCount(id);
        order.verify(postRepository).findById(id);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.author()).isEqualTo("alice");
        assertThat(response.title()).isEqualTo("Hello");
        assertThat(response.content()).isEqualTo("World");
        assertThat(response.createdAt()).isEqualTo(FIXED_NOW);
        assertThat(response.updatedAt()).isEqualTo(FIXED_NOW);
    }

    @Test
    void find_throws_PostNotFoundException_when_increment_returns_zero() {
        Long id = 99L;
        when(postRepository.incrementViewCount(id)).thenReturn(0);

        assertThatThrownBy(() -> postService.find(id))
                .isInstanceOf(PostNotFoundException.class);

        verify(postRepository, never()).findById(any());
    }

    @Test
    void update_modifies_title_and_content_and_updatedAt_preserving_other_fields() {
        Long id = 42L;
        Post existing = new Post("alice", "Old", "Old body", FIXED_NOW).withId(id);
        when(postRepository.findById(id)).thenReturn(Optional.of(existing));

        PostResponse response = postService.update(id, new PostUpdateRequest("New", "New body"));

        assertThat(existing.getTitle()).isEqualTo("New");
        assertThat(existing.getContent()).isEqualTo("New body");
        assertThat(existing.getUpdatedAt()).isEqualTo(FIXED_NOW);
        assertThat(existing.getAuthor()).isEqualTo("alice");

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.title()).isEqualTo("New");
        assertThat(response.content()).isEqualTo("New body");
        assertThat(response.author()).isEqualTo("alice");
    }

    @Test
    void update_throws_PostNotFoundException_when_id_missing() {
        Long id = 99L;
        when(postRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.update(id, new PostUpdateRequest("a", "b")))
                .isInstanceOf(PostNotFoundException.class);
    }

    @Test
    void delete_sets_deletedAt_on_found_post() {
        Long id = 42L;
        Post existing = new Post("alice", "Hello", "World", FIXED_NOW).withId(id);
        when(postRepository.findById(id)).thenReturn(Optional.of(existing));

        postService.delete(id);

        assertThat(existing.getDeletedAt()).isEqualTo(FIXED_NOW);
    }

    @Test
    void delete_throws_PostNotFoundException_when_id_missing() {
        Long id = 99L;
        when(postRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.delete(id))
                .isInstanceOf(PostNotFoundException.class);
    }
}
