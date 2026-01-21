package com.example.catalogservice.controller;

import com.example.catalogservice.service.ProductSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductSyncController.class)
@ContextConfiguration(classes = {ProductSyncController.class, ProductSyncControllerTest.TestConfig.class})
@DisplayName("ProductSyncController 통합 테스트")
class ProductSyncControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductSyncService productSyncService;

    @BeforeEach
    void setUp() {
        Mockito.reset(productSyncService);
    }

    @Configuration
    static class TestConfig {
        @Bean
        public ProductSyncService productSyncService() {
            return Mockito.mock(ProductSyncService.class);
        }
    }

    @Test
    @DisplayName("POST /api/internal/sync/full - 성공 응답")
    void fullSync_Success() throws Exception {
        // Given
        int syncedCount = 150;
        when(productSyncService.fullSync()).thenReturn(syncedCount);

        // When & Then
        mockMvc.perform(post("/api/internal/sync/full")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.syncedCount", is(syncedCount)))
                .andExpect(jsonPath("$.message", is("Full sync completed successfully")));

        verify(productSyncService, times(1)).fullSync();
    }

    @Test
    @DisplayName("POST /api/internal/sync/full - 동기화된 상품이 없는 경우")
    void fullSync_NoProducts() throws Exception {
        // Given
        when(productSyncService.fullSync()).thenReturn(0);

        // When & Then
        mockMvc.perform(post("/api/internal/sync/full")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.syncedCount", is(0)))
                .andExpect(jsonPath("$.message", is("Full sync completed successfully")));

        verify(productSyncService, times(1)).fullSync();
    }

    @Test
    @DisplayName("POST /api/internal/sync/full - 대용량 동기화 성공")
    void fullSync_LargeDataSet() throws Exception {
        // Given
        int syncedCount = 10000;
        when(productSyncService.fullSync()).thenReturn(syncedCount);

        // When & Then
        mockMvc.perform(post("/api/internal/sync/full")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.syncedCount", is(syncedCount)))
                .andExpect(jsonPath("$.message", is("Full sync completed successfully")));

        verify(productSyncService, times(1)).fullSync();
    }

    @Test
    @DisplayName("POST /api/internal/sync/full - 응답 구조 검증")
    void fullSync_ResponseStructure() throws Exception {
        // Given
        when(productSyncService.fullSync()).thenReturn(42);

        // When & Then
        mockMvc.perform(post("/api/internal/sync/full"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.syncedCount").exists())
                .andExpect(jsonPath("$.syncedCount").isNumber())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    @DisplayName("GET 메서드는 지원하지 않음 - 405 Method Not Allowed")
    void fullSync_GetMethodNotAllowed() throws Exception {
        // When & Then
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/internal/sync/full"))
                .andDo(print())
                .andExpect(status().isMethodNotAllowed());

        verify(productSyncService, never()).fullSync();
    }

    @Test
    @DisplayName("PUT 메서드는 지원하지 않음 - 405 Method Not Allowed")
    void fullSync_PutMethodNotAllowed() throws Exception {
        // When & Then
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/internal/sync/full"))
                .andDo(print())
                .andExpect(status().isMethodNotAllowed());

        verify(productSyncService, never()).fullSync();
    }

    @Test
    @DisplayName("DELETE 메서드는 지원하지 않음 - 405 Method Not Allowed")
    void fullSync_DeleteMethodNotAllowed() throws Exception {
        // When & Then
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/internal/sync/full"))
                .andDo(print())
                .andExpect(status().isMethodNotAllowed());

        verify(productSyncService, never()).fullSync();
    }
}
