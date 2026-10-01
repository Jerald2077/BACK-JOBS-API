package com.uap.proiv.jobs.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uap.proiv.jobs.client.UserApiRepository;
import com.uap.proiv.jobs.support.FakeUserApi;

import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test end to end del endpoint POST /api/assign.
 *
 * A diferencia del test de integracion, aca la aplicacion arranca en un servidor web
 * real en un puerto aleatorio y se la consume por HTTP de verdad: pasa por el stack
 * completo (conexion TCP, parseo de la request, filtros, controller, services,
 * serializacion de la respuesta).
 *
 * Se usa el HttpClient del JDK en vez de TestRestTemplate porque en Spring Boot 4
 * TestRestTemplate se movio al modulo spring-boot-resttestclient y necesita ademas
 * spring-boot-restclient, que este proyecto no declara. El HttpClient del JDK es el
 * mismo que ya usa UserApiRepository, asi que no agrega dependencias.
 *
 * La unica pieza sustituida es la API externa de usuarios, reemplazada por un
 * MockWebServer para que el test sea reproducible y no dependa de reqres.in.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AssignEndToEndTest {

    static MockWebServer mockWebServer = FakeUserApi.start();

    @LocalServerPort
    int port;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

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

    private URI assignUri() {
        return URI.create("http://localhost:" + port + "/api/assign");
    }

    private HttpResponse<String> postAssign(String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(assignUri())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    @DisplayName("e2e POST /api/assign devuelve la asignacion completa sobre HTTP real")
    void assign_e2e_success() throws Exception {
        String body = """
                {
                    "requestNumber": 777,
                    "clientName": "Universidad Adventista del Plata"
                }
                """;

        HttpResponse<String> response = postAssign(body);

        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("application/json"),
                "La respuesta debe ser JSON, fue: " + response.headers().firstValue("Content-Type").orElse("(sin header)"));

        JsonNode json = objectMapper.readTree(response.body());

        // Datos del pedido reinyectados por el controller
        assertEquals("Universidad Adventista del Plata", json.get("Client").asText());
        assertEquals(777, json.get("Request_Number").asInt());

        // Un bloque por puesto de jobs.json
        JsonNode assign = json.get("Assign");
        assertNotNull(assign, "La respuesta debe traer la clave Assign");
        assertTrue(assign.isArray(), "Assign debe ser un array");
        assertEquals(7, assign.size());

        // Cada puesto recibe exactamente 'resources' usuarios, y todos distintos
        for (JsonNode bloque : assign) {
            JsonNode job = bloque.get("job");
            JsonNode usuarios = bloque.get("users");

            assertNotNull(job, "Cada bloque debe traer su job");
            assertNotNull(usuarios, "Cada bloque debe traer sus users");

            int recursos = (int) job.get("resources").asDouble();
            assertEquals(recursos, usuarios.size(),
                    "El puesto " + job.get("name").asText() + " debe recibir " + recursos + " usuarios");

            Set<Integer> ids = new HashSet<>();
            for (JsonNode usuario : usuarios) {
                int id = usuario.get("id").asInt();
                assertTrue(ids.add(id),
                        "El usuario " + id + " quedo asignado dos veces al mismo puesto");
                assertTrue(id >= 1 && id <= FakeUserApi.USUARIOS_PAGINA_1,
                        "El id " + id + " no viene de la API de usuarios simulada");
                assertFalse(usuario.get("first_name").asText().isBlank());
            }
        }

        // El puesto mas grande de jobs.json pide 10 recursos
        assertEquals("Full Stack Developer", assign.get(6).get("job").get("name").asText());
        assertEquals(10, assign.get(6).get("users").size());
    }

    @Test
    @DisplayName("e2e POST /api/assign con clientName vacio devuelve 400 por HTTP")
    void assign_e2e_validacion() throws Exception {
        String body = """
                {
                    "requestNumber": 1,
                    "clientName": ""
                }
                """;

        HttpResponse<String> response = postAssign(body);

        assertEquals(400, response.statusCode());
    }

    @Test
    @DisplayName("e2e POST /api/assign sin body devuelve 400 por HTTP")
    void assign_e2e_sinBody() throws Exception {
        HttpResponse<String> response = postAssign("");

        assertEquals(400, response.statusCode());
    }

    @Test
    @DisplayName("e2e GET /api/assign devuelve 405: el endpoint solo acepta POST")
    void assign_e2e_metodoNoPermitido() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(assignUri())
                .GET()
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(405, response.statusCode());
    }
}
