package com.uap.proiv.jobs.controller;

import com.uap.proiv.jobs.dto.Job;
import com.uap.proiv.jobs.service.JobService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test unitario de JobController con MockMvc en modo standalone.
 *
 * Corregido tras la actualizacion del fork ("Separacion Controllers"): JobController
 * ya no expone el listado de usuarios ni la asignacion de trabajos. Ahora solo tiene
 * GET /api/job/all y GET /api/job/{id}. El listado de usuarios paso a UserController
 * (GET /api/user/{page}) y la asignacion a AssignController (POST /api/assign).
 */
@ExtendWith(MockitoExtension.class)
public class JobControllerTest {

    @Mock
    JobService jobService;

    @InjectMocks
    JobController jobController;

    private MockMvc mockMvc;

    private List<Job> jobs;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(jobController).build();

        jobs = new ArrayList<>();

        Job job1 = new Job();
        job1.setId(1);
        job1.setName("Data Engineer");
        job1.setSalary(5000);
        job1.setHours(530);
        job1.setResources(3);
        jobs.add(job1);

        Job job2 = new Job();
        job2.setId(2);
        job2.setName("Fronted Engineer");
        job2.setSalary(6000);
        job2.setHours(450);
        job2.setResources(3);
        jobs.add(job2);
    }

    @Test
    @DisplayName("GET /api/job/all retorna la lista de trabajos")
    void getAllJobs_success() throws Exception {
        when(jobService.getAllJobs()).thenReturn(jobs);

        mockMvc.perform(get("/api/job/all"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Data Engineer"))
                .andExpect(jsonPath("$[0].salary").value(5000.0))
                .andExpect(jsonPath("$[1].name").value("Fronted Engineer"));

        verify(jobService, times(1)).getAllJobs();
    }

    @Test
    @DisplayName("GET /api/job/all con lista vacia retorna un array vacio")
    void getAllJobs_listaVacia() throws Exception {
        when(jobService.getAllJobs()).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/api/job/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(jobService, times(1)).getAllJobs();
    }

    @Test
    @DisplayName("GET /api/job/all - camino de excepcion del service retorna 500")
    void getAllJobs_serviceException() throws Exception {
        when(jobService.getAllJobs()).thenThrow(new RuntimeException("Error al leer jobs.json"));

        mockMvc.perform(get("/api/job/all"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error al leer jobs.json"));

        verify(jobService, times(1)).getAllJobs();
    }

    @Test
    @DisplayName("GET /api/job/{id} retorna el trabajo pedido")
    void getJobById_success() throws Exception {
        when(jobService.getJobById(2)).thenReturn(jobs.get(1));

        mockMvc.perform(get("/api/job/2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Fronted Engineer"))
                .andExpect(jsonPath("$.resources").value(3.0));

        verify(jobService, times(1)).getJobById(2);
    }

    @Test
    @DisplayName("GET /api/job/{id} - id inexistente: el service lanza NoSuchElement y retorna 500")
    void getJobById_noEncontrado() throws Exception {
        when(jobService.getJobById(999)).thenThrow(new NoSuchElementException("No value present"));

        mockMvc.perform(get("/api/job/999"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("No value present"));

        verify(jobService, times(1)).getJobById(999);
    }

    @Test
    @DisplayName("GET /api/job/{id} con id no numerico no matchea el handler")
    void getJobById_idInvalido() throws Exception {
        mockMvc.perform(get("/api/job/abc"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Tras la actualizacion del fork, /api/job/users/{page} y /api/job/assign ya no existen")
    void rutasMigradasYaNoPertenecenAJobController() throws Exception {
        // El listado de usuarios vive ahora en UserController: GET /api/user/{page}.
        // Sobre JobController son dos segmentos que no matchean ningun patron: 404.
        mockMvc.perform(get("/api/job/users/1"))
                .andExpect(status().isNotFound());

        // La asignacion vive ahora en AssignController: POST /api/assign.
        // Sobre JobController la ruta /api/job/assign si matchea el patron
        // GET /api/job/{id}, por eso el resultado es 405 (metodo no permitido)
        // y no 404: el path existe, el verbo POST no.
        mockMvc.perform(post("/api/job/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestNumber\":123,\"clientName\":\"Name\"}"))
                .andExpect(status().isMethodNotAllowed());
    }
}
