package com.uap.proiv.jobs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uap.proiv.jobs.client.UserApiRepository;
import com.uap.proiv.jobs.support.FakeUserApi;

import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test de integracion de AssignController.
 *
 * Levanta el contexto completo: AssignController -> UserJobAssignedServiceImpl ->
 * JobServiceImpl (jobs.json real) + UserServiceImpl + AssignedServiceImpl. Lo unico
 * que se sustituye es el UserApiRepository, apuntado a un MockWebServer que hace de
 * API externa de usuarios, asi el test no depende de reqres.in ni de la red.
 *
 * jobs.json pide hasta 10 recursos para un puesto, por lo que la API simulada debe
 * entregar al menos 10 usuarios o AssignedServiceImpl falla con IndexOutOfBounds.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class AssignControllerIntegrationTest {

    static MockWebServer mockWebServer = FakeUserApi.start();

    @Autowired
    MockMvc mockMvc;

    @AfterAll
    static void tearDown() throws IOException {
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
            return new UserApiRepository(
                    httpClient,
                    objectMapper,
                    mockWebServer.url("/api/users").toString(),
                    FakeUserApi.API_KEY);
        }
    }

    @Test
    @DisplayName("POST /api/assign integracion: asigna usuarios a cada puesto de jobs.json")
    void assign_success() throws Exception {
        String body = """
                {
                    "requestNumber": 123,
                    "clientName": "UAP"
                }
                """;

        mockMvc.perform(post("/api/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                // el controller reinyecta los datos del pedido en la respuesta
                .andExpect(jsonPath("$.Client").value("UAP"))
                .andExpect(jsonPath("$.Request_Number").value(123))
                // un UserJobAssigned por cada puesto de jobs.json
                .andExpect(jsonPath("$.Assign").isArray())
                .andExpect(jsonPath("$.Assign.length()").value(7))
                .andExpect(jsonPath("$.Assign[0].job.id").value(1))
                .andExpect(jsonPath("$.Assign[0].job.name").value("Data Engineer"))
                // cada puesto recibe tantos usuarios como 'resources' declare
                .andExpect(jsonPath("$.Assign[0].users.length()").value(3))
                .andExpect(jsonPath("$.Assign[2].job.name").value("Backend Engineer"))
                .andExpect(jsonPath("$.Assign[2].users.length()").value(5))
                .andExpect(jsonPath("$.Assign[3].job.name").value("QA Engineer"))
                .andExpect(jsonPath("$.Assign[3].users.length()").value(2))
                .andExpect(jsonPath("$.Assign[6].job.name").value("Full Stack Developer"))
                .andExpect(jsonPath("$.Assign[6].users.length()").value(10))
                // los usuarios asignados vienen de la API externa simulada
                .andExpect(jsonPath("$.Assign[0].users[0].id").isNumber())
                .andExpect(jsonPath("$.Assign[0].users[0].first_name").isString());
    }

    @Test
    @DisplayName("POST /api/assign integracion - validacion: sin clientName retorna 400")
    void assign_clientNameVacio_retorna400() throws Exception {
        String body = """
                {
                    "requestNumber": 123,
                    "clientName": ""
                }
                """;

        // clientName esta anotado con @NotEmpty, la validacion corre antes del handler
        mockMvc.perform(post("/api/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/assign integracion - validacion: sin requestNumber retorna 400")
    void assign_requestNumberNull_retorna400() throws Exception {
        String body = """
                {
                    "clientName": "UAP"
                }
                """;

        mockMvc.perform(post("/api/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/assign integracion - camino de excepcion: body malformado retorna 400")
    void assign_bodyMalformado_retorna400() throws Exception {
        mockMvc.perform(post("/api/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ esto no es json }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/assign no esta mapeado: solo se acepta POST")
    void assign_metodoNoPermitido() throws Exception {
        mockMvc.perform(get("/api/assign"))
                .andExpect(status().isMethodNotAllowed());
    }
}
