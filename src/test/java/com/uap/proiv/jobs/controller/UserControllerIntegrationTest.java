package com.uap.proiv.jobs.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;


import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uap.proiv.jobs.client.UserApiRepository;

//import okhttp3.MediaType;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import org.springframework.http.MediaType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc

public class UserControllerIntegrationTest {
    

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UserApiRepository userApiRepository;

    static MockWebServer mockWebServer = startMockWebServer();

    private static MockWebServer startMockWebServer() {
        MockWebServer server = new MockWebServer();
        try {
            server.start();
            return server;
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @AfterAll
    public static void tearDown() throws IOException {
        mockWebServer.close();
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public UserApiRepository userApiRepository(ObjectMapper objectMapper) {
            HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
            String baseUrl = mockWebServer.url("/api/users").toString();
            String apiKey = "free_user_3HYTiqu2JKQ4TfGq884xW5mqfrd";
            
            return new UserApiRepository(httpClient, objectMapper, baseUrl, apiKey);
        }
    }

    @Test
    @DisplayName("GET api/user/id/{id} integracion UserController, UserService, UserRepository, mock de la API externa")
    void getUserById() throws Exception {
        String jsonResponse = """
            {
                "id": 2,
                "email": "Juan.perez@gmail.com",
                "first_name": "Juan",
                "last_name": "Perez",
                "avatar": "https://reqres.in/img/faces/2-image.jpg"
            }
            """;
        mockWebServer.enqueue(new MockResponse()
            .setBody(jsonResponse)
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
        );
            
        mockMvc.perform(get("/api/user/id/2"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(2))
            .andExpect(jsonPath("$.email").value("Juan.perez@gmail.com"))
            .andExpect(jsonPath("$.first_name").value("Juan"))
            .andExpect(jsonPath("$.last_name").value("Perez"))
            .andExpect(jsonPath("$.avatar").value("https://reqres.in/img/faces/2-image.jpg"));

            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertEquals("application/json", recordedRequest.getHeader("Accept"));
            assertEquals("free_user_3HYTiqu2JKQ4TfGq884xW5mqfrd", recordedRequest.getHeader("X-API-KEY"));

    }

    @Test
    @DisplayName("POST /api/user/update integracion UserController, UserService, UserRepository, mock api externa")
    void updateUser() throws Exception {
        String updateResponse = """
                {
                    "name": "",
                    "job": "",
                    "updatedAt": ""
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(updateResponse)
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
        );  
        
        String userJson = """
                {
                    "id": 1,
                    "first_name": "Carlos",
                    "last_name": "Perez"
                }
                """;

        mockMvc.perform(post("/api/user/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(userJson))
                .andExpect(status().isOk())
                .andExpect(content().string("User created successfully"));

            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertEquals("application/json", recordedRequest.getHeader("Accept"));
            assertEquals("application/json", recordedRequest.getHeader("Content-Type"));
            assertEquals("free_user_3HYTiqu2JKQ4TfGq884xW5mqfrd", recordedRequest.getHeader("X-API-KEY"));
    }
}