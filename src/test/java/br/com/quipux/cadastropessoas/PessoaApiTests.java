package br.com.quipux.cadastropessoas;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import br.com.quipux.cadastropessoas.repository.PessoaRepository;

/** End-to-end tests of the people endpoints (validation rules, status codes) on an in-memory database. */
@SpringBootTest
@AutoConfigureMockMvc
class PessoaApiTests {

    @Autowired MockMvc mvc;
    @Autowired PessoaRepository repository;

    @BeforeEach
    void limpar() {
        repository.deleteAll();
    }

    // Simulates an already logged-in user
    private static final org.springframework.test.web.servlet.request.RequestPostProcessor ADMIN = user("admin").roles("ADMIN");

    private static String json(String doc, String email) {
        return "{\"userName\":\"Maria\",\"userSurname\":\"Silva\",\"userDocument\":\"" + doc
            + "\",\"userEmail\":\"" + email + "\"}";
    }

    private Long criar(String doc) throws Exception {
        String corpo = mvc.perform(post("/registrarName").with(ADMIN).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(json(doc, "maria@teste.com")))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return Long.valueOf(corpo.replaceAll(".*\"userId\":(\\d+).*", "$1"));
    }

    @Test
    void registraPessoaValida() throws Exception {
        mvc.perform(post("/registrarName").with(ADMIN).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(json("12345678901", "maria@teste.com")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.userName").value("Maria"));
    }

    @Test
    void clienteNaoConsegueDefinirNacionalidadeNoCadastro() throws Exception {
        mvc.perform(post("/registrarName").with(ADMIN).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"userName\":\"Maria\",\"userSurname\":\"Silva\",\"userDocument\":\"12345678901\","
                    + "\"userEmail\":\"maria@teste.com\",\"nacionalidade\":\"Atlantida\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.nacionalidade").doesNotExist());
    }

    @Test
    void rejeitaEmailInvalido() throws Exception {
        mvc.perform(post("/registrarName").with(ADMIN).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(json("12345678901", "nao-e-email")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campos.userEmail").exists());
    }

    @Test
    void rejeitaDocumentoForaDoPadrao() throws Exception {
        mvc.perform(post("/registrarName").with(ADMIN).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(json("abc", "maria@teste.com")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campos.userDocument").exists());
    }

    @Test
    void rejeitaCamposVazios() throws Exception {
        mvc.perform(post("/registrarName").with(ADMIN).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campos.userName").exists());
    }

    @Test
    void rejeitaDocumentoDuplicado() throws Exception {
        criar("12345678901");
        mvc.perform(post("/registrarName").with(ADMIN).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(json("12345678901", "outra@teste.com")))
            .andExpect(status().isConflict());
    }

    @Test
    void rejeitaEmailDuplicado() throws Exception {
        criar("12345678901");
        mvc.perform(post("/registrarName").with(ADMIN).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(json("98765432100", "MARIA@teste.com")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.erro").value(org.hamcrest.Matchers.containsString("e-mail")));
    }

    @Test
    void listaPessoas() throws Exception {
        criar("12345678901");
        mvc.perform(get("/list").with(ADMIN))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void buscaPorId() throws Exception {
        Long id = criar("12345678901");
        mvc.perform(get("/list/" + id).with(ADMIN))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userSurname").value("Silva"));
    }

    @Test
    void buscaIdInexistenteRetorna404() throws Exception {
        mvc.perform(get("/list/9999").with(ADMIN)).andExpect(status().isNotFound());
    }

    @Test
    void buscaIdNaoNumericoRetorna400() throws Exception {
        mvc.perform(get("/list/abc").with(ADMIN))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void exclusaoAutenticadaRemovePessoa() throws Exception {
        Long id = criar("12345678901");
        mvc.perform(delete("/list/" + id).with(ADMIN).with(csrf()))
            .andExpect(status().isOk());
        mvc.perform(get("/list/" + id).with(ADMIN)).andExpect(status().isNotFound());
    }

    @Test
    void exclusaoIdInexistenteRetorna404() throws Exception {
        mvc.perform(delete("/list/9999").with(ADMIN).with(csrf()))
            .andExpect(status().isNotFound());
    }

    @Test
    void exclusaoIdNaoNumericoRetorna400() throws Exception {
        mvc.perform(delete("/list/abc").with(ADMIN).with(csrf()))
            .andExpect(status().isBadRequest());
    }

    @Test
    void nacionalidadeDePessoaInexistenteRetorna404() throws Exception {
        mvc.perform(get("/findNacionalityByPerson/9999").with(ADMIN)).andExpect(status().isNotFound());
    }

    @Test
    void nacionalidadeIdNaoNumericoRetorna400() throws Exception {
        mvc.perform(get("/findNacionalityByPerson/abc").with(ADMIN)).andExpect(status().isBadRequest());
    }

    @Test
    void nacionalidadeComApiExternaIndisponivelRetorna502() throws Exception {
        Long id = criar("12345678901");
        // the test configuration points the external API to an unreachable port
        mvc.perform(get("/findNacionalityByPerson/" + id).with(ADMIN)).andExpect(status().isBadGateway());
    }
}
