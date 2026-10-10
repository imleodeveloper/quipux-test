package br.com.quipux.cadastropessoas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * Session-based authentication: logging in creates a session on the server and the browser
 * only receives the JSESSIONID cookie (HttpOnly, SameSite=Strict). Nothing is kept in localStorage.
 * Because the cookie is sent automatically by the browser, CSRF protection stays enabled
 * (XSRF-TOKEN cookie + X-XSRF-TOKEN header).
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // The token cookie must be readable by the front-end JavaScript so it can echo it in a header
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .authorizeHttpRequests(auth -> auth
                // Public: the login page, the login call, the session check and the icon font used by the login page
                .requestMatchers("/login.html", "/login", "/api/me", "/favicon.ico", "/vendor/**").permitAll()
                // Everything else (all APIs and the main page) needs a logged-in user
                .anyRequest().authenticated()
            )
            // JSON-friendly login: answer 200 or 401 instead of redirecting
            .formLogin(form -> form
                .loginPage("/login.html")
                .loginProcessingUrl("/login")
                .successHandler((req, res, auth) -> res.setStatus(HttpStatus.OK.value()))
                .failureHandler((req, res, ex) -> res.setStatus(HttpStatus.UNAUTHORIZED.value())))
            .logout(logout -> logout
                .logoutUrl("/logout")
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler((req, res, auth) -> res.setStatus(HttpStatus.OK.value())))
            .exceptionHandling(ex -> ex.authenticationEntryPoint((req, res, e) -> {
                String path = req.getRequestURI();
                // Pages go to the login screen; API calls get a plain 401
                if (path.equals("/") || path.endsWith(".html")) {
                    res.sendRedirect("/login.html");
                } else {
                    res.setStatus(HttpStatus.UNAUTHORIZED.value());
                }
            }));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Single in-memory user. Username and password come from configuration (.env), never from code.
     * The password is hashed with BCrypt at startup.
     */
    @Bean
    public UserDetailsService userDetailsService(
            PasswordEncoder encoder,
            @Value("${app.security.username}") String username,
            @Value("${app.security.password}") String password) {
        return new InMemoryUserDetailsManager(
            User.withUsername(username).password(encoder.encode(password)).roles("ADMIN").build());
    }
}
