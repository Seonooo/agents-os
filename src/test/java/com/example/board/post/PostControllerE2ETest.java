package com.example.board.post;

import com.example.board.support.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.headers.ResponseHeadersSnippet;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.payload.RequestFieldsSnippet;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;
import org.springframework.restdocs.request.PathParametersSnippet;
import org.springframework.restdocs.request.QueryParametersSnippet;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@Import(TestcontainersConfiguration.class)
class PostControllerE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PostRepository postRepository;

    @BeforeEach
    void clearDatabase() {
        postRepository.deleteAll();
    }

    // ----- POST /posts -----

    @Test
    void valid_request_returns_201_with_location_and_body() throws Exception {
        PostCreateRequest request = new PostCreateRequest("alice", "Hello", "World");

        mockMvc.perform(post("/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("/posts/\\d+")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.author").value("alice"))
                .andExpect(jsonPath("$.title").value("Hello"))
                .andExpect(jsonPath("$.content").value("World"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.viewCount").value(0))
                .andDo(document("post-create-happy",
                        postCreateRequestFields(),
                        locationHeader(),
                        postResponseFields()));
    }

    @Test
    void missing_author_returns_400_with_field_name() throws Exception {
        PostCreateRequest request = new PostCreateRequest(null, "Hello", "World");

        expectValidationFailure(perform(request))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("author")))
                .andDo(document("post-create-missing-author",
                        postCreateRequestFields(),
                        errorResponseFields()));
    }

    @Test
    void whitespace_only_title_returns_400() throws Exception {
        PostCreateRequest request = new PostCreateRequest("alice", "   ", "World");

        expectValidationFailure(perform(request))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("title")))
                .andDo(document("post-create-whitespace-title",
                        postCreateRequestFields(),
                        errorResponseFields()));
    }

    @Test
    void missing_content_returns_400() throws Exception {
        PostCreateRequest request = new PostCreateRequest("alice", "Hello", null);

        expectValidationFailure(perform(request))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("content")))
                .andDo(document("post-create-missing-content",
                        postCreateRequestFields(),
                        errorResponseFields()));
    }

    @Test
    void oversized_author_returns_400() throws Exception {
        PostCreateRequest request = new PostCreateRequest("a".repeat(31), "Hello", "World");

        expectValidationFailure(perform(request))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("author")))
                .andDo(document("post-create-oversized-author",
                        postCreateRequestFields(),
                        errorResponseFields()));
    }

    @Test
    void oversized_title_returns_400() throws Exception {
        PostCreateRequest request = new PostCreateRequest("alice", "t".repeat(81), "World");

        expectValidationFailure(perform(request))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("title")))
                .andDo(document("post-create-oversized-title",
                        postCreateRequestFields(),
                        errorResponseFields()));
    }

    @Test
    void oversized_content_returns_400() throws Exception {
        PostCreateRequest request = new PostCreateRequest("alice", "Hello", "c".repeat(5001));

        expectValidationFailure(perform(request))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("content")))
                .andDo(document("post-create-oversized-content",
                        postCreateRequestFields(),
                        errorResponseFields()));
    }

    // ----- GET /posts (list) -----

    @Test
    void get_returns_200_with_empty_page_when_no_posts() throws Exception {
        mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andDo(document("post-list-empty",
                        paginationQueryParameters(),
                        pageResponseFields()));
    }

    @Test
    void get_returns_posts_in_created_at_desc_id_desc_order() throws Exception {
        Instant t1 = Instant.parse("2026-05-15T10:00:00Z");
        Instant t2 = Instant.parse("2026-05-15T11:00:00Z");
        Instant t3 = Instant.parse("2026-05-15T12:00:00Z");
        postRepository.saveAll(List.of(
                new Post("alice", "First", "Body1", t1),
                new Post("bob", "Second", "Body2", t2),
                new Post("carol", "Third", "Body3", t3)
        ));

        mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[0].title").value("Third"))
                .andExpect(jsonPath("$.content[1].title").value("Second"))
                .andExpect(jsonPath("$.content[2].title").value("First"))
                .andExpect(jsonPath("$.content[0].viewCount").value(0))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andDo(document("post-list-multi-ordered",
                        paginationQueryParameters(),
                        pageResponseFields()));
    }

    @Test
    void get_with_negative_page_returns_400() throws Exception {
        mockMvc.perform(get("/posts").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/posts"))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("page")))
                .andDo(document("post-list-negative-page",
                        paginationQueryParameters(),
                        errorResponseFields()));
    }

    @Test
    void get_with_size_zero_returns_400() throws Exception {
        mockMvc.perform(get("/posts").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("size")))
                .andDo(document("post-list-size-zero",
                        paginationQueryParameters(),
                        errorResponseFields()));
    }

    @Test
    void get_with_size_over_max_returns_400() throws Exception {
        mockMvc.perform(get("/posts").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("size")))
                .andDo(document("post-list-size-over-max",
                        paginationQueryParameters(),
                        errorResponseFields()));
    }

    @Test
    void get_with_non_numeric_page_returns_400() throws Exception {
        mockMvc.perform(get("/posts").param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andDo(document("post-list-non-numeric-page",
                        paginationQueryParameters()));
    }

    @Test
    void get_with_custom_size_returns_partial_content_and_correct_total_pages() throws Exception {
        postRepository.saveAll(List.of(
                new Post("alice", "First", "Body1", Instant.parse("2026-05-15T10:00:00Z")),
                new Post("bob", "Second", "Body2", Instant.parse("2026-05-15T11:00:00Z"))
        ));

        mockMvc.perform(get("/posts").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Second"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andDo(document("post-list-paginated",
                        paginationQueryParameters(),
                        pageResponseFields()));
    }

    // ----- GET /posts?q= (search by title) -----

    @Test
    void get_with_query_filters_by_title_case_insensitive() throws Exception {
        postRepository.saveAll(List.of(
                new Post("alice", "Spring Boot 4", "release", Instant.parse("2026-05-15T10:00:00Z")),
                new Post("bob", "Hello World", "intro", Instant.parse("2026-05-15T11:00:00Z")),
                new Post("carol", "Quarkus Notes", "framework", Instant.parse("2026-05-15T12:00:00Z"))
        ));

        mockMvc.perform(get("/posts").param("q", "spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Spring Boot 4"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andDo(document("post-list-search-match",
                        searchQueryParameters(),
                        pageResponseFields()));
    }

    @Test
    void get_with_query_returns_empty_page_when_no_match() throws Exception {
        postRepository.save(new Post("alice", "Spring", "x", Instant.parse("2026-05-15T10:00:00Z")));

        mockMvc.perform(get("/posts").param("q", "zzz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andDo(document("post-list-search-no-match",
                        searchQueryParameters(),
                        pageResponseFields()));
    }

    @Test
    void get_with_empty_query_returns_all_posts_like_unfiltered() throws Exception {
        postRepository.saveAll(List.of(
                new Post("alice", "First", "Body1", Instant.parse("2026-05-15T10:00:00Z")),
                new Post("bob", "Second", "Body2", Instant.parse("2026-05-15T11:00:00Z"))
        ));

        mockMvc.perform(get("/posts").param("q", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void get_with_query_does_not_match_content_only_title() throws Exception {
        postRepository.saveAll(List.of(
                new Post("alice", "Title A", "the word spring appears in content", Instant.parse("2026-05-15T10:00:00Z")),
                new Post("bob", "Spring Title", "unrelated content", Instant.parse("2026-05-15T11:00:00Z"))
        ));

        mockMvc.perform(get("/posts").param("q", "spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Spring Title"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void get_with_oversized_query_returns_400() throws Exception {
        mockMvc.perform(get("/posts").param("q", "a".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("q")))
                .andDo(document("post-list-search-oversized-q",
                        searchQueryParameters(),
                        errorResponseFields()));
    }

    @Test
    void get_with_q_at_exact_max_length_succeeds() throws Exception {
        // @Size(max=100) boundary: exactly 100 chars must pass. Guards off-by-one regressions.
        mockMvc.perform(get("/posts").param("q", "a".repeat(100)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void get_with_korean_query_matches_korean_title_locks_in_current_behavior() throws Exception {
        // Locks in: Korean substring search works (Korean has no case; LOWER is a no-op).
        // Task Context Q-F decided "영문만 case-insensitive 가정" — this test documents the
        // de facto Korean behavior so a future contributor sees it before changing.
        postRepository.saveAll(List.of(
                new Post("alice", "한국어 게시판", "x", Instant.parse("2026-05-15T10:00:00Z")),
                new Post("bob", "English Title", "y", Instant.parse("2026-05-15T11:00:00Z"))
        ));

        mockMvc.perform(get("/posts").param("q", "한국어"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("한국어 게시판"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void get_with_sql_injection_attempt_q_is_treated_as_literal_string() throws Exception {
        // JPA parameter binding ensures user input is bound as a parameter, never concatenated.
        // Locks in: an attacker-style q returns no matches (no title contains that literal),
        // and crucially does NOT return all posts via SQL truthy-tautology.
        postRepository.saveAll(List.of(
                new Post("alice", "Safe Title", "x", Instant.parse("2026-05-15T10:00:00Z")),
                new Post("bob", "Another Post", "y", Instant.parse("2026-05-15T11:00:00Z"))
        ));

        mockMvc.perform(get("/posts").param("q", "' OR 1=1 --"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void get_with_q_and_no_size_uses_default_size_20_capping_per_request_payload() throws Exception {
        // Full-scan guardrail: ?q= without ?size still caps per-request payload at default 20.
        // (DB still scans the full table for LIKE matching — index introduction is out of MVP scope —
        // but client-bound payload is bounded.)
        List<Post> manyMatches = new java.util.ArrayList<>();
        for (int i = 0; i < 25; i++) {
            String minute = String.format("%02d", i);
            manyMatches.add(new Post("a", "spring " + i, "x",
                    Instant.parse("2026-05-15T10:" + minute + ":00Z")));
        }
        postRepository.saveAll(manyMatches);

        mockMvc.perform(get("/posts").param("q", "spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.content", hasSize(20)))
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void get_with_query_paginates_filtered_results() throws Exception {
        postRepository.saveAll(List.of(
                new Post("a", "Spring 1", "x", Instant.parse("2026-05-15T10:00:00Z")),
                new Post("b", "Spring 2", "y", Instant.parse("2026-05-15T11:00:00Z")),
                new Post("c", "Spring 3", "z", Instant.parse("2026-05-15T12:00:00Z")),
                new Post("d", "Quarkus", "irrelevant", Instant.parse("2026-05-15T13:00:00Z"))
        ));

        mockMvc.perform(get("/posts").param("q", "spring").param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Spring 1"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void get_with_query_orders_matches_by_created_at_desc_id_desc() throws Exception {
        postRepository.saveAll(List.of(
                new Post("a", "Spring oldest", "x", Instant.parse("2026-05-15T10:00:00Z")),
                new Post("b", "Spring middle", "y", Instant.parse("2026-05-15T11:00:00Z")),
                new Post("c", "Spring newest", "z", Instant.parse("2026-05-15T12:00:00Z"))
        ));

        mockMvc.perform(get("/posts").param("q", "spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[0].title").value("Spring newest"))
                .andExpect(jsonPath("$.content[1].title").value("Spring middle"))
                .andExpect(jsonPath("$.content[2].title").value("Spring oldest"));
    }

    @Test
    void get_with_whitespace_only_query_returns_all_posts_like_unfiltered() throws Exception {
        postRepository.saveAll(List.of(
                new Post("a", "First", "x", Instant.parse("2026-05-15T10:00:00Z")),
                new Post("b", "Second", "y", Instant.parse("2026-05-15T11:00:00Z"))
        ));

        mockMvc.perform(get("/posts").param("q", "   "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void get_with_sql_wildcard_q_is_escaped_by_spring_data_and_matches_literal() throws Exception {
        // Spring Data JPA's `Containing` keyword auto-escapes %, _, \ so they are matched literally,
        // NOT as SQL LIKE wildcards. We discovered this at implementation time — Q-B "escape drop"
        // turned out to be moot: framework already handles it. This test locks in that contract.
        Post withPct = postRepository.save(new Post("a", "50% off sale", "x", Instant.parse("2026-05-15T11:00:00Z")));
        postRepository.save(new Post("b", "alpha", "y", Instant.parse("2026-05-15T10:00:00Z")));

        mockMvc.perform(get("/posts").param("q", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(withPct.getId()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void get_with_query_excludes_soft_deleted_posts() throws Exception {
        Post keep = postRepository.save(new Post("alice", "Spring Boot", "x", Instant.parse("2026-05-15T10:00:00Z")));
        Post drop = postRepository.save(new Post("bob", "Spring Cloud", "y", Instant.parse("2026-05-15T11:00:00Z")));

        mockMvc.perform(delete("/posts/{id}", drop.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/posts").param("q", "spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(keep.getId()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ----- GET /posts/{id} -----

    @Test
    void get_by_id_returns_200_and_increments_view_count_on_each_call() throws Exception {
        Post saved = postRepository.save(new Post("alice", "Hello", "World", Instant.parse("2026-05-15T10:00:00Z")));
        Long id = saved.getId();

        mockMvc.perform(get("/posts/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.author").value("alice"))
                .andExpect(jsonPath("$.title").value("Hello"))
                .andExpect(jsonPath("$.content").value("World"))
                .andExpect(jsonPath("$.viewCount").value(1))
                .andDo(document("post-get-happy",
                        idPathParameter(),
                        postResponseFields()));

        mockMvc.perform(get("/posts/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewCount").value(2));
    }

    @Test
    void get_by_id_returns_404_with_resource_not_found_when_id_missing() throws Exception {
        mockMvc.perform(get("/posts/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/posts/99999"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andDo(document("post-get-not-found",
                        idPathParameter(),
                        errorResponseFields()));
    }

    @Test
    void get_by_id_returns_404_for_negative_id() throws Exception {
        mockMvc.perform(get("/posts/{id}", -1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andDo(document("post-get-negative-id",
                        idPathParameter(),
                        errorResponseFields()));
    }

    @Test
    void get_by_id_returns_400_for_non_numeric_id() throws Exception {
        mockMvc.perform(get("/posts/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andDo(document("post-get-non-numeric-id",
                        idPathParameter()));
    }

    // ----- PUT /posts/{id} -----

    @Test
    void put_by_id_returns_200_with_updated_title_and_content() throws Exception {
        Post saved = postRepository.save(new Post("alice", "Original", "Body", Instant.parse("2026-05-15T10:00:00Z")));
        Long id = saved.getId();
        PostUpdateRequest req = new PostUpdateRequest("Updated", "New body");

        mockMvc.perform(put("/posts/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Updated"))
                .andExpect(jsonPath("$.content").value("New body"))
                .andExpect(jsonPath("$.author").value("alice"))
                .andExpect(jsonPath("$.viewCount").value(0))
                .andDo(document("post-update-happy",
                        idPathParameter(),
                        postUpdateRequestFields(),
                        postResponseFields()));
    }

    @Test
    void put_by_id_returns_404_when_post_not_found() throws Exception {
        PostUpdateRequest req = new PostUpdateRequest("x", "y");

        mockMvc.perform(put("/posts/{id}", 99999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andDo(document("post-update-not-found",
                        idPathParameter(),
                        postUpdateRequestFields(),
                        errorResponseFields()));
    }

    @Test
    void put_with_missing_title_returns_400() throws Exception {
        Post saved = postRepository.save(new Post("alice", "Original", "Body", Instant.parse("2026-05-15T10:00:00Z")));
        PostUpdateRequest req = new PostUpdateRequest(null, "only content");

        mockMvc.perform(put("/posts/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("title")))
                .andDo(document("post-update-missing-title",
                        idPathParameter(),
                        postUpdateRequestFields(),
                        errorResponseFields()));
    }

    @Test
    void put_with_whitespace_only_title_returns_400() throws Exception {
        Post saved = postRepository.save(new Post("alice", "Original", "Body", Instant.parse("2026-05-15T10:00:00Z")));
        PostUpdateRequest req = new PostUpdateRequest("   ", "x");

        mockMvc.perform(put("/posts/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("title")))
                .andDo(document("post-update-whitespace-title",
                        idPathParameter(),
                        postUpdateRequestFields(),
                        errorResponseFields()));
    }

    @Test
    void put_with_oversized_title_returns_400() throws Exception {
        Post saved = postRepository.save(new Post("alice", "Original", "Body", Instant.parse("2026-05-15T10:00:00Z")));
        PostUpdateRequest req = new PostUpdateRequest("t".repeat(81), "x");

        mockMvc.perform(put("/posts/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("title")))
                .andDo(document("post-update-oversized-title",
                        idPathParameter(),
                        postUpdateRequestFields(),
                        errorResponseFields()));
    }

    @Test
    void put_with_missing_content_returns_400() throws Exception {
        Post saved = postRepository.save(new Post("alice", "Original", "Body", Instant.parse("2026-05-15T10:00:00Z")));
        PostUpdateRequest req = new PostUpdateRequest("ok", null);

        mockMvc.perform(put("/posts/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("content")))
                .andDo(document("post-update-missing-content",
                        idPathParameter(),
                        postUpdateRequestFields(),
                        errorResponseFields()));
    }

    @Test
    void put_with_oversized_content_returns_400() throws Exception {
        Post saved = postRepository.save(new Post("alice", "Original", "Body", Instant.parse("2026-05-15T10:00:00Z")));
        PostUpdateRequest req = new PostUpdateRequest("ok", "c".repeat(5001));

        mockMvc.perform(put("/posts/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("content")))
                .andDo(document("post-update-oversized-content",
                        idPathParameter(),
                        postUpdateRequestFields(),
                        errorResponseFields()));
    }

    // ----- DELETE /posts/{id} (soft delete) -----

    @Test
    void delete_by_id_returns_204_and_soft_deletes_post() throws Exception {
        Post saved = postRepository.save(new Post("alice", "X", "Y", Instant.parse("2026-05-15T10:00:00Z")));
        Long id = saved.getId();

        mockMvc.perform(delete("/posts/{id}", id))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andDo(document("post-delete-success",
                        idPathParameter()));

        mockMvc.perform(get("/posts/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void delete_by_id_returns_404_when_post_not_found() throws Exception {
        mockMvc.perform(delete("/posts/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andDo(document("post-delete-not-found",
                        idPathParameter(),
                        errorResponseFields()));
    }

    @Test
    void delete_same_post_twice_returns_404_on_second_call() throws Exception {
        Post saved = postRepository.save(new Post("alice", "X", "Y", Instant.parse("2026-05-15T10:00:00Z")));

        mockMvc.perform(delete("/posts/{id}", saved.getId()))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/posts/{id}", saved.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andDo(document("post-delete-twice",
                        idPathParameter(),
                        errorResponseFields()));
    }

    @Test
    void soft_deleted_post_does_not_appear_in_list() throws Exception {
        Post p1 = postRepository.save(new Post("a", "Keep", "1", Instant.parse("2026-05-15T10:00:00Z")));
        Post p2 = postRepository.save(new Post("b", "Delete me", "2", Instant.parse("2026-05-15T11:00:00Z")));

        mockMvc.perform(delete("/posts/{id}", p2.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Keep"))
                .andDo(document("post-delete-soft-excluded-from-list",
                        paginationQueryParameters(),
                        pageResponseFields()));
    }

    // ----- Test helpers -----

    private ResultActions perform(PostCreateRequest request) throws Exception {
        return mockMvc.perform(post("/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private ResultActions expectValidationFailure(ResultActions actions) throws Exception {
        return actions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/posts"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // ----- REST Docs snippet helpers (shared field/parameter descriptors) -----

    private static RequestFieldsSnippet postCreateRequestFields() {
        return requestFields(
                fieldWithPath("author").description("작성자 이름 (1-30자, 필수)"),
                fieldWithPath("title").description("제목 (1-80자, 필수)"),
                fieldWithPath("content").description("내용 (1-5000자, 필수)")
        );
    }

    private static RequestFieldsSnippet postUpdateRequestFields() {
        return requestFields(
                fieldWithPath("title").description("제목 (1-80자, 필수)"),
                fieldWithPath("content").description("내용 (1-5000자, 필수)")
        );
    }

    private static ResponseFieldsSnippet postResponseFields() {
        return responseFields(
                fieldWithPath("id").description("Post의 고유 ID (BIGSERIAL)"),
                fieldWithPath("author").description("작성자"),
                fieldWithPath("title").description("제목"),
                fieldWithPath("content").description("내용"),
                fieldWithPath("createdAt").description("생성 시각 (ISO-8601 UTC)"),
                fieldWithPath("updatedAt").description("수정 시각 (ISO-8601 UTC)"),
                fieldWithPath("viewCount").description("조회수")
        );
    }

    private static ResponseFieldsSnippet pageResponseFields() {
        return responseFields(
                fieldWithPath("content").description("Post 배열"),
                fieldWithPath("content[].id").type(JsonFieldType.NUMBER).description("Post의 고유 ID").optional(),
                fieldWithPath("content[].author").type(JsonFieldType.STRING).description("작성자").optional(),
                fieldWithPath("content[].title").type(JsonFieldType.STRING).description("제목").optional(),
                fieldWithPath("content[].content").type(JsonFieldType.STRING).description("내용").optional(),
                fieldWithPath("content[].createdAt").type(JsonFieldType.STRING).description("생성 시각 (ISO-8601 UTC)").optional(),
                fieldWithPath("content[].updatedAt").type(JsonFieldType.STRING).description("수정 시각 (ISO-8601 UTC)").optional(),
                fieldWithPath("content[].viewCount").type(JsonFieldType.NUMBER).description("조회수").optional(),
                fieldWithPath("page").description("현재 페이지 번호 (0-indexed)"),
                fieldWithPath("size").description("페이지 크기"),
                fieldWithPath("totalElements").description("전체 항목 수"),
                fieldWithPath("totalPages").description("전체 페이지 수")
        );
    }

    private static QueryParametersSnippet paginationQueryParameters() {
        return queryParameters(
                parameterWithName("page").description("페이지 번호 (0 이상, 기본 0)").optional(),
                parameterWithName("size").description("페이지 크기 (1-100, 기본 20)").optional()
        );
    }

    private static QueryParametersSnippet searchQueryParameters() {
        return queryParameters(
                parameterWithName("page").description("페이지 번호 (0 이상, 기본 0)").optional(),
                parameterWithName("size").description("페이지 크기 (1-100, 기본 20)").optional(),
                parameterWithName("q").description("title 검색어 (1-100자, case-insensitive partial). 미지정·빈 문자열이면 전체 목록.").optional()
        );
    }

    private static PathParametersSnippet idPathParameter() {
        return pathParameters(
                parameterWithName("id").description("Post의 고유 ID")
        );
    }

    private static ResponseHeadersSnippet locationHeader() {
        return responseHeaders(
                headerWithName("Location").description("생성된 자원 URI (/posts/{id})")
        );
    }

    private static ResponseFieldsSnippet errorResponseFields() {
        return responseFields(
                fieldWithPath("code").description("에러 코드 (ErrorCode enum name)"),
                fieldWithPath("message").description("에러 메시지"),
                fieldWithPath("path").description("요청 path"),
                fieldWithPath("timestamp").description("발생 시각 (ISO-8601 UTC)"),
                fieldWithPath("errors").type(JsonFieldType.ARRAY).description("필드별 에러 목록 (검증 실패 시만)").optional(),
                fieldWithPath("errors[].field").type(JsonFieldType.STRING).description("위반 필드").optional(),
                fieldWithPath("errors[].message").type(JsonFieldType.STRING).description("위반 사유").optional()
        );
    }
}
