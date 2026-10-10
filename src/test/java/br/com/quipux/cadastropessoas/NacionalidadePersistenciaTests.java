package br.com.quipux.cadastropessoas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.sun.net.httpserver.HttpServer;

import br.com.quipux.cadastropessoas.repository.PessoaRepository;

/** Checks that a looked-up nationality is saved in the database and shows up in the list. */
@SpringBootTest
@AutoConfigureMockMvc
class NacionalidadePersistenciaTests {

    static HttpServer apiFalsa;

    static {
        try {
            apiFalsa = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            apiFalsa.createContext("/", ex -> {
                byte[] body = "{\"name\":\"x\",\"country\":[{\"country_id\":\"JP\",\"probability\":0.9}]}"
                    .getBytes(StandardCharsets.UTF_8);
                ex.getResponseHeaders().add("Content-Type", "application/json");
                ex.sendResponseHeaders(200, body.length);
                ex.getResponseBody().write(body);
                ex.close();
            });
            apiFalsa.start();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("app.nationalize.url", () -> "http://127.0.0.1:" + apiFalsa.getAddress().getPort());
    }

    @AfterAll
    static void parar() {
        apiFalsa.stop(0);
    }

    @Autowired MockMvc mvc;
    @Autowired PessoaRepository repository;

    @Test
    void consultaSalvaNacionalidadeNoBanco() throws Exception {
        repository.deleteAll();
        Pessoa p = new Pessoa();
        p.setUserName("Hiro");
        p.setUserSurname("Tanaka");
        p.setUserDocument("77788899900");
        p.setUserEmail("hiro@teste.com");
        p = repository.save(p);
        assertNull(p.getNacionalidade());

        mvc.perform(get("/findNacionalityByPerson/" + p.getUserId()).with(user("admin")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nacionalidade").value("Japão"));

        assertEquals("Japão", repository.findById(p.getUserId()).orElseThrow().getNacionalidade());
        mvc.perform(get("/list").with(user("admin")))
            .andExpect(jsonPath("$[0].nacionalidade").value("Japão"));
    }
}
