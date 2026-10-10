package br.com.quipux.cadastropessoas.controller;

import java.util.Map;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lets the web UI check whether the user is logged in. */
@RestController
public class SessaoController {

    /**
     * Returns whether there is an active session and who the user is.
     * Reading the CSRF token here also makes the server send the XSRF-TOKEN cookie,
     * which the UI needs before it can call the login or any POST/DELETE endpoint.
     */
    @GetMapping("/api/me")
    public Map<String, Object> me(Authentication auth, CsrfToken csrf) {
        csrf.getToken();
        boolean logado = auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
        return Map.of("autenticado", logado, "usuario", logado ? auth.getName() : "");
    }
}
