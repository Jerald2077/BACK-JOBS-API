package com.uap.proiv.jobs.support;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import java.io.IOException;

/**
 * API externa de usuarios simulada, usada por los tests de integracion y e2e de
 * /api/assign.
 *
 * Responde por query param 'page' en vez de usar una cola de respuestas, porque
 * UserJobAssignedServiceImpl.assign() pide paginas hasta pasarse de totalPages y una
 * cola vacia dejaria el test colgado esperando una respuesta que nadie encolo.
 *
 * Entrega 12 usuarios en la pagina 1 y una pagina 2 vacia. Los 12 son necesarios:
 * jobs.json declara un puesto con 10 recursos y AssignedServiceImpl indexa la lista
 * de usuarios directamente.
 */
public final class FakeUserApi {

    public static final String API_KEY = "free_user_3HYTiqu2JKQ4TfGq884xW5mqfrd";

    /** Cantidad de usuarios que devuelve la pagina 1. */
    public static final int USUARIOS_PAGINA_1 = 12;

    private FakeUserApi() {
    }

    public static MockWebServer start() {
        MockWebServer server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath() == null ? "" : request.getPath();
                String body = path.contains("page=1") ? paginaConUsuarios() : paginaVacia();
                return new MockResponse()
                        .setResponseCode(200)
                        .addHeader("Content-Type", "application/json")
                        .setBody(body);
            }
        });
        try {
            server.start();
            return server;
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /** Pagina 1: 12 usuarios, total_pages 1. */
    public static String paginaConUsuarios() {
        StringBuilder data = new StringBuilder();
        for (int id = 1; id <= USUARIOS_PAGINA_1; id++) {
            if (id > 1) {
                data.append(",");
            }
            data.append(String.format(
                    """
                    {
                        "id": %d,
                        "email": "usuario%d@mail.com",
                        "first_name": "Nombre%d",
                        "last_name": "Apellido%d",
                        "avatar": "https://reqres.in/img/faces/%d-image.jpg"
                    }
                    """, id, id, id, id, id));
        }
        return String.format(
                """
                {
                    "page": 1,
                    "per_page": %d,
                    "total": %d,
                    "total_pages": 1,
                    "data": [%s]
                }
                """, USUARIOS_PAGINA_1, USUARIOS_PAGINA_1, data);
    }

    /**
     * Pagina 2 en adelante: vacia y con page mayor a totalPages, para que el bucle de
     * paginado de assign() corte.
     */
    public static String paginaVacia() {
        return """
                {
                    "page": 2,
                    "per_page": 12,
                    "total": 12,
                    "total_pages": 1,
                    "data": []
                }
                """;
    }
}
