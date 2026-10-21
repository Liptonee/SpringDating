package org.petproject.dating_backend.integration;

import org.junit.jupiter.api.Test;

class RateLimitIT extends BaseIntegrationTest {

    @Test
    void returns429_whenManyRequests() {

        for (int i = 0; i < 20; i++) {
            restTestClient.post()
                    .uri("api/auth/login")
                    .exchange()
                    .expectStatus().isBadRequest();
        }
        restTestClient.post()
                .uri("api/auth/login")
                .exchange()
                .expectStatus().isEqualTo(429);


    }

}
