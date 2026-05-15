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
    public PageResponse<PostResponse> list(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, LIST_SORT);
        Page<PostResponse> mapped = postRepository.findAll(pageable).map(PostResponse::from);
        return PageResponse.from(mapped);
    }
}
