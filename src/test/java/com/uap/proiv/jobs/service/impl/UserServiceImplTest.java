package com.uap.proiv.jobs.service.impl;

import com.uap.proiv.jobs.client.UserApiRepository;
import com.uap.proiv.jobs.dto.User;
import com.uap.proiv.jobs.dto.UserApiResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de UserServiceImpl: casos de exito y caminos de excepcion.
 * UserApiRepository se mockea, por lo que no se hace ninguna llamada HTTP real.
 */
@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    UserApiRepository userApiRepository;

    @InjectMocks
    UserServiceImpl userServiceImpl;

    private UserApiResponse userApiResponse;
    private List<User> users;

    @BeforeEach
    void setup() {
        users = new ArrayList<>();

        User user1 = new User();
        user1.setId(1);
        user1.setEmail("juan@mail.com");
        user1.setFirstName("Juan");
        user1.setLastName("Perez");
        user1.setAvatar("https://reqres.in/img/faces/1-image.jpg");
        users.add(user1);

        User user2 = new User();
        user2.setId(2);
        user2.setEmail("ana@mail.com");
        user2.setFirstName("Ana");
        user2.setLastName("Gomez");
        user2.setAvatar("https://reqres.in/img/faces/2-image.jpg");
        users.add(user2);

        User user3 = new User();
        user3.setId(3);
        user3.setEmail("luis@mail.com");
        user3.setFirstName("Luis");
        user3.setLastName("Diaz");
        user3.setAvatar("https://reqres.in/img/faces/3-image.jpg");
        users.add(user3);

        userApiResponse = new UserApiResponse();
        userApiResponse.setPage(1);
        userApiResponse.setPerPage(3);
        userApiResponse.setTotal(3);
        userApiResponse.setTotalPages(1);
        userApiResponse.setData(users);
    }

    // ---------- search(page) ----------

    @Test
    @DisplayName("search() retorna la pagina del repositorio y asigna jobId incremental desde 1")
    void search_success_asignaJobIdIncremental() {
        when(userApiRepository.getUsers(1)).thenReturn(userApiResponse);

        UserApiResponse result = userServiceImpl.search(1);

        assertNotNull(result);
        assertEquals(1, result.getPage());
        assertEquals(3, result.getTotal());
        assertEquals(1, result.getTotalPages());
        assertEquals(3, result.getData().size());

        // El service numera los jobId de forma incremental sobre la pagina recibida
        assertEquals(1, result.getData().get(0).getJobId());
        assertEquals(2, result.getData().get(1).getJobId());
        assertEquals(3, result.getData().get(2).getJobId());

        // No muta el resto de los datos del usuario
        assertEquals("Juan", result.getData().get(0).getFirstName());
        assertEquals("ana@mail.com", result.getData().get(1).getEmail());

        verify(userApiRepository, times(1)).getUsers(1);
    }

    @Test
    @DisplayName("search() propaga la pagina pedida al repositorio")
    void search_success_respetaLaPaginaPedida() {
        userApiResponse.setPage(2);
        when(userApiRepository.getUsers(2)).thenReturn(userApiResponse);

        UserApiResponse result = userServiceImpl.search(2);

        assertEquals(2, result.getPage());
        verify(userApiRepository, times(1)).getUsers(2);
        verify(userApiRepository, never()).getUsers(1);
    }

    @Test
    @DisplayName("search() con pagina sin usuarios retorna data vacia y no falla")
    void search_success_paginaVacia() {
        userApiResponse.setData(new ArrayList<>());
        userApiResponse.setTotal(0);
        when(userApiRepository.getUsers(9)).thenReturn(userApiResponse);

        UserApiResponse result = userServiceImpl.search(9);

        assertNotNull(result);
        assertTrue(result.getData().isEmpty());
        verify(userApiRepository, times(1)).getUsers(9);
    }

    @Test
    @DisplayName("search() - camino de excepcion: propaga el fallo del repositorio")
    void search_repositoryException_sePropaga() {
        when(userApiRepository.getUsers(1))
                .thenThrow(new RuntimeException("Error al conectar con la API de usuarios: timeout"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userServiceImpl.search(1));

        assertEquals("Error al conectar con la API de usuarios: timeout", ex.getMessage());
        verify(userApiRepository, times(1)).getUsers(1);
    }

    @Test
    @DisplayName("search() - camino de excepcion: si la API responde sin data lanza NullPointerException")
    void search_dataNull_lanzaNullPointerException() {
        UserApiResponse sinData = new UserApiResponse();
        sinData.setPage(1);
        sinData.setData(null);
        when(userApiRepository.getUsers(1)).thenReturn(sinData);

        // El service recorre getData() sin validar null, por lo que el fallo emerge aca.
        assertThrows(NullPointerException.class, () -> userServiceImpl.search(1));

        verify(userApiRepository, times(1)).getUsers(1);
    }

    // ---------- searchById(id) ----------

    @Test
    @DisplayName("searchById() retorna el usuario y le asigna jobId 1")
    void searchById_success_asignaJobId1() {
        when(userApiRepository.getUserById(2)).thenReturn(users.get(1));

        User result = userServiceImpl.searchById(2);

        assertNotNull(result);
        assertEquals(2, result.getId());
        assertEquals("Ana", result.getFirstName());
        assertEquals("Gomez", result.getLastName());
        assertEquals("ana@mail.com", result.getEmail());
        assertEquals(1, result.getJobId());

        verify(userApiRepository, times(1)).getUserById(2);
    }

    @Test
    @DisplayName("searchById() devuelve la misma instancia que entrega el repositorio")
    void searchById_success_devuelveLaMismaInstancia() {
        when(userApiRepository.getUserById(1)).thenReturn(users.get(0));

        User result = userServiceImpl.searchById(1);

        assertSame(users.get(0), result);
        verify(userApiRepository, times(1)).getUserById(1);
    }

    @Test
    @DisplayName("searchById() - camino de excepcion: usuario inexistente lanza NullPointerException")
    void searchById_usuarioInexistente_lanzaNullPointerException() {
        // getUserById filtra en memoria y devuelve null con orElse(null);
        // el service hace setJobId sobre ese null sin validarlo.
        when(userApiRepository.getUserById(999)).thenReturn(null);

        assertThrows(NullPointerException.class, () -> userServiceImpl.searchById(999));

        verify(userApiRepository, times(1)).getUserById(999);
    }

    @Test
    @DisplayName("searchById() - camino de excepcion: propaga el fallo del repositorio")
    void searchById_repositoryException_sePropaga() {
        when(userApiRepository.getUserById(1))
                .thenThrow(new RuntimeException("Error en ReqRes API. Codigo: 401"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userServiceImpl.searchById(1));

        assertEquals("Error en ReqRes API. Codigo: 401", ex.getMessage());
        verify(userApiRepository, times(1)).getUserById(1);
    }

    // ---------- update(user) ----------

    @Test
    @DisplayName("update() delega en el repositorio sin transformar el usuario")
    void update_success_delegaEnRepository() {
        User user = users.get(0);

        userServiceImpl.update(user);

        verify(userApiRepository, times(1)).updateUser(user);
    }

    @Test
    @DisplayName("update() - camino de excepcion: envuelve el fallo del repositorio en RuntimeException")
    void update_repositoryException_envuelveElError() {
        User user = users.get(0);
        doThrow(new RuntimeException("Error en ReqRes API. Codigo: 500"))
                .when(userApiRepository).updateUser(any(User.class));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userServiceImpl.update(user));

        assertTrue(ex.getMessage().startsWith("Error al crear el usuario: "),
                "Se esperaba el mensaje envuelto por el service, pero fue: " + ex.getMessage());
        assertNotNull(ex.getCause(), "El service debe conservar la causa original");
        assertEquals("Error en ReqRes API. Codigo: 500", ex.getCause().getMessage());

        verify(userApiRepository, times(1)).updateUser(user);
    }
}
