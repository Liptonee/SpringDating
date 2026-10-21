package org.petproject.dating_backend;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class DatingBackendApplicationTests {

	@Container
	@ServiceConnection
	protected static PostgreSQLContainer<?> postgres =
			new PostgreSQLContainer<>("postgres:16-alpine");

	@Container
	@ServiceConnection
	protected static RedisContainer redis =
			new RedisContainer("redis:7-alpine");

	@Test
	void contextLoads() {
	}

}
