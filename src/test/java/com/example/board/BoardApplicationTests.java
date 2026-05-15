package com.example.board;

import com.example.board.support.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BoardApplicationTests {

    @Test
    void contextLoads() {
    }
}
