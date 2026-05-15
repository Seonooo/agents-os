CREATE TABLE posts (
    id          BIGSERIAL PRIMARY KEY,
    author      VARCHAR(30)              NOT NULL,
    title       VARCHAR(80)              NOT NULL,
    content     VARCHAR(5000)            NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    view_count  BIGINT                   NOT NULL
);
