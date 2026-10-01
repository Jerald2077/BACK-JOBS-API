package com.uap.proiv.jobs.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test de integracion de JobController.
 *
 * Levanta el contexto completo de Spring sin mocks: JobController -> JobServiceImpl ->
 * JobApiRepository, que lee src/main/resources/jobs.json. Los datos esperados son los
 * de ese archivo, por lo que si jobs.json cambia este test debe actualizarse.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class JobControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/job/all integracion JobController + JobService + JobApiRepository (jobs.json)")
    void getAllJobs_desdeJobsJson() throws Exception {
        mockMvc.perform(get("/api/job/all"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(7))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Data Engineer"))
                .andExpect(jsonPath("$[0].salary").value(5000.0))
                .andExpect(jsonPath("$[0].hours").value(530.0))
                .andExpect(jsonPath("$[0].resources").value(3.0))
                .andExpect(jsonPath("$[6].id").value(8))
                .andExpect(jsonPath("$[6].name").value("Full Stack Developer"))
                .andExpect(jsonPath("$[6].resources").value(10.0));
    }

    @Test
    @DisplayName("GET /api/job/{id} integracion: devuelve el trabajo real de jobs.json")
    void getJobById_existente() throws Exception {
        mockMvc.perform(get("/api/job/5"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("QA Engineer"))
                .andExpect(jsonPath("$.salary").value(3600.0))
                .andExpect(jsonPath("$.resources").value(2.0));
    }

    @Test
    @DisplayName("GET /api/job/{id} integracion: los ids 3 y 9 no existen en jobs.json")
    void getJobById_idSaltadoEnElArchivo() throws Exception {
        // jobs.json no tiene id 3 (salta de 2 a 4). El service hace orElseThrow()
        // y el controller traduce la NoSuchElementException a 500.
        mockMvc.perform(get("/api/job/3"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("No value present"));
    }

    @Test
    @DisplayName("GET /api/job/{id} integracion - camino de excepcion: id inexistente retorna 500")
    void getJobById_inexistente() throws Exception {
        mockMvc.perform(get("/api/job/999"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("No value present"));
    }

    @Test
    @DisplayName("GET /api/job/{id} integracion: id no numerico retorna 4xx")
    void getJobById_idNoNumerico() throws Exception {
        mockMvc.perform(get("/api/job/abc"))
                .andExpect(status().is4xxClientError());
    }
}
