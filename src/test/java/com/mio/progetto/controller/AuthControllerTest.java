package com.mio.progetto.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mio.progetto.model.UtenteEntity;
import com.mio.progetto.repository.UtenteRepository;
import com.mio.progetto.service.MailService;

@WebMvcTest(controllers = {AuthController.class, com.mio.progetto.exception.GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "app.frontend-base-url=http://localhost:8080")
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private UtenteRepository utenteRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private MailService mailService;

    private static final String MESSAGGIO_GENERICO =
            "Se l'indirizzo è registrato, riceverai a breve un'email con le istruzioni per reimpostare la password.";

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void testForgotPasswordEmailEsistente() throws Exception {
        UtenteEntity utente = new UtenteEntity(1, "testuser", "test@example.com", "passwordHashata", "ROLE_USER");
        when(utenteRepository.findByEmail("test@example.com")).thenReturn(Optional.of(utente));

        AuthController.ForgotPasswordRequest body = new AuthController.ForgotPasswordRequest();
        body.email = "test@example.com";

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        verify(utenteRepository, times(1)).impostaResetToken(anyInt(), anyString(), any(Instant.class));
        verify(mailService, times(1)).inviaEmailResetPassword(anyString(), anyString());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void testForgotPasswordEmailInesistente() throws Exception {
        when(utenteRepository.findByEmail("sconosciuto@example.com")).thenReturn(Optional.empty());

        AuthController.ForgotPasswordRequest body = new AuthController.ForgotPasswordRequest();
        body.email = "sconosciuto@example.com";

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(MESSAGGIO_GENERICO));

        verify(mailService, never()).inviaEmailResetPassword(anyString(), anyString());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void testForgotPasswordRateLimit() throws Exception {
        when(utenteRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        AuthController.ForgotPasswordRequest body = new AuthController.ForgotPasswordRequest();
        body.email = "sconosciuto@example.com";
        String json = objectMapper.writeValueAsString(body);

        // Le prime 3 richieste dallo stesso IP (127.0.0.1 di default in MockMvc) passano.
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/auth/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isOk());
        }

        // La quarta viene bloccata dal rate limiter.
        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void testResetPasswordTokenValido() throws Exception {
        UtenteEntity utente = new UtenteEntity(1, "testuser", "test@example.com", "vecchiaHashata", "ROLE_USER");
        when(utenteRepository.findByResetTokenValido(anyString())).thenReturn(Optional.of(utente));
        when(passwordEncoder.encode("nuovaPassword")).thenReturn("nuovaHashata");

        AuthController.ResetPasswordRequest body = new AuthController.ResetPasswordRequest();
        body.token = "token-qualsiasi";
        body.nuovaPassword = "nuovaPassword";

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        verify(utenteRepository, times(1)).updatePassword(1, "nuovaHashata");
        verify(utenteRepository, times(1)).pulisciResetToken(1);
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void testResetPasswordTokenNonValido() throws Exception {
        when(utenteRepository.findByResetTokenValido(anyString())).thenReturn(Optional.empty());

        AuthController.ResetPasswordRequest body = new AuthController.ResetPasswordRequest();
        body.token = "token-scaduto-o-inesistente";
        body.nuovaPassword = "nuovaPassword";

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        verify(utenteRepository, never()).updatePassword(anyInt(), anyString());
        verify(utenteRepository, never()).pulisciResetToken(anyInt());
    }
}
