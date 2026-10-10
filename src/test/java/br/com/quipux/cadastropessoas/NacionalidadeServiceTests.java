package br.com.quipux.cadastropessoas;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import br.com.quipux.cadastropessoas.repository.PessoaRepository;
import br.com.quipux.cadastropessoas.service.NacionalidadeService;
import br.com.quipux.cadastropessoas.service.PessoaService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Tests the nationalize.io client against a fake local HTTP server (no internet needed). */
class NacionalidadeServiceTests {

    HttpServer server;
    String ultimaQuery;

    @BeforeEach
    void subir() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", ex -> {
            ultimaQuery = ex.getRequestURI().getQuery();
            byte[] body = ("{\"count\":10,\"name\":\"maria\",\"country\":["
                + "{\"country_id\":\"PT\",\"probability\":0.2},"
                + "{\"country_id\":\"BR\",\"probability\":0.6}]}").getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(200, body.length);
            ex.getResponseBody().write(body);
            ex.close();
        });
        server.start();
    }

    @AfterEach
    void parar() {
        server.stop(0);
    }

    @Test
    void retornaPaisDeMaiorProbabilidade() {
        Pessoa p = new Pessoa();
        p.setUserName("Maria");
        p.setUserSurname("Silva");
        PessoaRepository repo = mock(PessoaRepository.class);
        when(repo.findById(1L)).thenReturn(java.util.Optional.of(p));

        NacionalidadeService service = new NacionalidadeService(
            new PessoaService(repo), "http://127.0.0.1:" + server.getAddress().getPort());

        Map<String, Object> r = service.buscarNacionalidade(1L);

        assertEquals("BR", r.get("codigoPais"));
        assertEquals("Brasil", r.get("nacionalidade"));
        assertEquals("name=Maria", ultimaQuery);
        assertEquals("Brasil", p.getNacionalidade());
    }
}
