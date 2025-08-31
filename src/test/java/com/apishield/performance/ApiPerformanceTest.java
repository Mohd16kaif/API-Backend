package com.apishield.performance;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiPerformanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testHealthEndpointPerformance() throws Exception {
        int numberOfRequests = 100;
        int numberOfThreads = 10;

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

        long startTime = System.currentTimeMillis();

        CompletableFuture<Void>[] futures = IntStream.range(0, numberOfRequests)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    try {
                        mockMvc.perform(get("/actuator/health"))
                                .andExpect(status().isOk());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }, executor))
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures).join();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.printf("Completed %d requests in %d ms%n", numberOfRequests, duration);
        System.out.printf("Average response time: %.2f ms%n", (double) duration / numberOfRequests);
        System.out.printf("Requests per second: %.2f%n", (double) numberOfRequests * 1000 / duration);

        executor.shutdown();

        // Assert reasonable performance (adjust thresholds as needed)
        assert duration < 10000 : "Health endpoint took too long: " + duration + "ms";
        assert (double) numberOfRequests * 1000 / duration > 50 : "Too few requests per second";
    }
}
