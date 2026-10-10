package br.com.quipux.cadastropessoas;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

/** Login, logout, CSRF and the rule that every API requires a session. */
@SpringBootTest
@AutoConfigureMockMvc
class AutenticacaoTests {

    @Autowired MockMvc mvc;

    @Test
    void apiSemLoginRetorna401() throws Exception {
        mvc.perform(get("/list")).andExpect(status().isUnauthorized());
        mvc.perform(get("/list/1")).andExpect(status().isUnauthorized());
        mvc.perform(get("/findNacionalityByPerson/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void cadastroSemLoginRetorna401() throws Exception {
        mvc.perform(post("/registrarName").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void exclusaoSemLoginRetorna401() throws Exception {
        mvc.perform(delete("/list/1").with(csrf())).andExpect(status().isUnauthorized());
    }

    @Test
    void paginaPrincipalSemLoginRedirecionaParaLogin() throws Exception {
        mvc.perform(get("/")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login.html"));
    }

    @Test
    void paginaDeLoginEhPublica() throws Exception {
        mvc.perform(get("/login.html")).andExpect(status().isOk());
    }

    @Test
    void iconesSaoPublicosParaATelaDeLogin() throws Exception {
        mvc.perform(get("/vendor/bootstrap-icons/bootstrap-icons.min.css")).andExpect(status().isOk());
    }

    @Test
    void loginComSenhaErradaRetorna401() throws Exception {
        mvc.perform(post("/login").with(csrf()).param("username", "admin").param("password", "errada"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void postSemTokenCsrfEhNegado() throws Exception {
        mvc.perform(post("/login").param("username", "admin").param("password", "test-only-password"))
            .andExpect(status().isForbidden());
    }

    @Test
    void loginValidoCriaSessaoQueLiberaAsApis() throws Exception {
        MockHttpSession sessao = (MockHttpSession) mvc.perform(
                post("/login").with(csrf()).param("username", "admin").param("password", "test-only-password"))
            .andExpect(status().isOk())
            .andReturn().getRequest().getSession(false);

        mvc.perform(get("/list").session(sessao)).andExpect(status().isOk());
        mvc.perform(get("/api/me").session(sessao))
            .andExpect(jsonPath("$.autenticado").value(true))
            .andExpect(jsonPath("$.usuario").value("admin"));
    }

    @Test
    void logoutEncerraASessao() throws Exception {
        MockHttpSession sessao = (MockHttpSession) mvc.perform(
                post("/login").with(csrf()).param("username", "admin").param("password", "test-only-password"))
            .andReturn().getRequest().getSession(false);

        mvc.perform(post("/logout").with(csrf()).session(sessao)).andExpect(status().isOk());
        mvc.perform(get("/list").session(sessao)).andExpect(status().isUnauthorized());
    }

    @Test
    void apiMeSemLoginInformaNaoAutenticado() throws Exception {
        mvc.perform(get("/api/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.autenticado").value(false));
    }
}
