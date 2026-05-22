package com.example.board.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class PostService {

    private static final Sort LIST_SORT = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final PostRepository postRepository;
    private final Clock clock;

    public PostService(PostRepository postRepository, Clock clock) {
        this.postRepository = postRepository;
        this.clock = clock;
    }

    @Transactional
    public PostResponse create(PostCreateRequest request) {
        Instant now = clock.instant();
        Post saved = postRepository.save(new Post(request.author(), request.title(), request.content(), now));
        return PostResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> list(int page, int size, String q) {
        Pageable pageable = PageRequest.of(page, size, LIST_SORT);
        Page<Post> repoPage = (q == null || q.isBlank())
                ? postRepository.findAll(pageable)
                : postRepository.findByTitleContainingIgnoreCase(q, pageable);
        return PageResponse.from(repoPage.map(PostResponse::from));
    }

    @Transactional
    public PostResponse find(Long id) {
        int updated = postRepository.incrementViewCount(id);
        if (updated == 0) {
            throw new PostNotFoundException(id);
        }
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new PostNotFoundException(id));
        return PostResponse.from(post);
    }

    @Transactional
    public PostResponse update(Long id, PostUpdateRequest request) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new PostNotFoundException(id));
        post.update(request.title(), request.content(), clock.instant());
        return PostResponse.from(post);
    }

    @Transactional
    public void delete(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new PostNotFoundException(id));
        post.delete(clock.instant());
    }
}
