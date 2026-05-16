package com.example.board.post;

import com.example.board.support.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
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
                .andExpect(jsonPath("$.viewCount").value(0));
    }

    @Test
    void missing_author_returns_400_with_field_name() throws Exception {
        PostCreateRequest request = new PostCreateRequest(null, "Hello", "World");

        expectValidationFailure(perform(request))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("author")));
    }

    @Test
    void whitespace_only_title_returns_400() throws Exception {
        PostCreateRequest request = new PostCreateRequest("alice", "   ", "World");

        expectValidationFailure(perform(request))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("title")));
    }

    @Test
    void missing_content_returns_400() throws Exception {
        PostCreateRequest request = new PostCreateRequest("alice", "Hello", null);

        expectValidationFailure(perform(request))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("content")));
    }

    @Test
    void oversized_author_returns_400() throws Exception {
        PostCreateRequest request = new PostCreateRequest("a".repeat(31), "Hello", "World");

        expectValidationFailure(perform(request))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("author")));
    }

    @Test
    void oversized_title_returns_400() throws Exception {
        PostCreateRequest request = new PostCreateRequest("alice", "t".repeat(81), "World");

        expectValidationFailure(perform(request))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("title")));
    }

    @Test
    void oversized_content_returns_400() throws Exception {
        PostCreateRequest request = new PostCreateRequest("alice", "Hello", "c".repeat(5001));

        expectValidationFailure(perform(request))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("content")));
    }

    @Test
    void get_returns_200_with_empty_page_when_no_posts() throws Exception {
        mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
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
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void get_with_negative_page_returns_400() throws Exception {
        mockMvc.perform(get("/posts").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/posts"))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("page")));
    }

    @Test
    void get_with_size_zero_returns_400() throws Exception {
        mockMvc.perform(get("/posts").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("size")));
    }

    @Test
    void get_with_size_over_max_returns_400() throws Exception {
        mockMvc.perform(get("/posts").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("size")));
    }

    @Test
    void get_with_non_numeric_page_returns_400() throws Exception {
        mockMvc.perform(get("/posts").param("page", "abc"))
                .andExpect(status().isBadRequest());
    }

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
                .andExpect(jsonPath("$.viewCount").value(1));

        mockMvc.perform(get("/posts/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewCount").value(2));
    }

    @Test
    void get_by_id_returns_404_with_resource_not_found_when_id_missing() throws Exception {
        mockMvc.perform(get("/posts/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/posts/99999"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void get_by_id_returns_404_for_negative_id() throws Exception {
        mockMvc.perform(get("/posts/-1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void get_by_id_returns_400_for_non_numeric_id() throws Exception {
        mockMvc.perform(get("/posts/abc"))
                .andExpect(status().isBadRequest());
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
                .andExpect(jsonPath("$.totalPages").value(2));
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
                .andExpect(jsonPath("$.viewCount").value(0));
    }

    @Test
    void put_by_id_returns_404_when_post_not_found() throws Exception {
        PostUpdateRequest req = new PostUpdateRequest("x", "y");

        mockMvc.perform(put("/posts/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
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
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("title")));
    }

    @Test
    void put_with_whitespace_only_title_returns_400() throws Exception {
        Post saved = postRepository.save(new Post("alice", "Original", "Body", Instant.parse("2026-05-15T10:00:00Z")));
        PostUpdateRequest req = new PostUpdateRequest("   ", "x");

        mockMvc.perform(put("/posts/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("title")));
    }

    @Test
    void put_with_oversized_title_returns_400() throws Exception {
        Post saved = postRepository.save(new Post("alice", "Original", "Body", Instant.parse("2026-05-15T10:00:00Z")));
        PostUpdateRequest req = new PostUpdateRequest("t".repeat(81), "x");

        mockMvc.perform(put("/posts/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("title")));
    }

    @Test
    void put_with_missing_content_returns_400() throws Exception {
        Post saved = postRepository.save(new Post("alice", "Original", "Body", Instant.parse("2026-05-15T10:00:00Z")));
        PostUpdateRequest req = new PostUpdateRequest("ok", null);

        mockMvc.perform(put("/posts/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("content")));
    }

    @Test
    void put_with_oversized_content_returns_400() throws Exception {
        Post saved = postRepository.save(new Post("alice", "Original", "Body", Instant.parse("2026-05-15T10:00:00Z")));
        PostUpdateRequest req = new PostUpdateRequest("ok", "c".repeat(5001));

        mockMvc.perform(put("/posts/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("content")));
    }

    // ----- DELETE /posts/{id} (soft delete) -----

    @Test
    void delete_by_id_returns_204_and_soft_deletes_post() throws Exception {
        Post saved = postRepository.save(new Post("alice", "X", "Y", Instant.parse("2026-05-15T10:00:00Z")));
        Long id = saved.getId();

        mockMvc.perform(delete("/posts/{id}", id))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mockMvc.perform(get("/posts/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void delete_by_id_returns_404_when_post_not_found() throws Exception {
        mockMvc.perform(delete("/posts/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void delete_same_post_twice_returns_404_on_second_call() throws Exception {
        Post saved = postRepository.save(new Post("alice", "X", "Y", Instant.parse("2026-05-15T10:00:00Z")));

        mockMvc.perform(delete("/posts/{id}", saved.getId()))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/posts/{id}", saved.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
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
                .andExpect(jsonPath("$.content[0].title").value("Keep"));
    }

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
}
