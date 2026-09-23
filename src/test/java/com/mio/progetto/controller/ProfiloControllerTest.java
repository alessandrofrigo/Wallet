package com.mio.progetto.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mio.progetto.model.UtenteEntity;
import com.mio.progetto.repository.UtenteRepository;
import com.mio.progetto.service.CustomUserDetails;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.ArgumentMatchers.anyString;

@WebMvcTest(controllers = {ProfiloController.class, com.mio.progetto.exception.GlobalExceptionHandler.class})
public class ProfiloControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UtenteRepository utenteRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PasswordEncoder passwordEncoder;

    private CustomUserDetails getMockUser() {
        UtenteEntity utente = new UtenteEntity(1, "testuser", "testmail@example.com", "password", "ROLE_USER");
        return new CustomUserDetails(utente);
    }

   @Test
    void testDeleteAccountSuccess() throws Exception {
        CustomUserDetails mockUser = getMockUser();
        UtenteEntity utente = new UtenteEntity(1, "testuser", "testmail@example.com", "passwordHashata", "ROLE_USER");

        when(utenteRepository.findById(1)).thenReturn(Optional.of(utente));
        when(passwordEncoder.matches("passwordGiusta", "passwordHashata")).thenReturn(true);

        ProfiloController.DeleteAccountRequest body = new ProfiloController.DeleteAccountRequest();
        body.password = "passwordGiusta";

        mockMvc.perform(delete("/api/profilo")
                .with(user(mockUser))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        verify(utenteRepository, times(1)).deleteById(1);
    }

    @Test
    void testDeleteAccountWrongPassword() throws Exception {
        CustomUserDetails mockUser = getMockUser();
        UtenteEntity utente = new UtenteEntity(1, "testuser", "testmail@example.com", "passwordHashata", "ROLE_USER");

        when(utenteRepository.findById(1)).thenReturn(Optional.of(utente));
        when(passwordEncoder.matches("passwordSbagliata", "passwordHashata")).thenReturn(false);

        ProfiloController.DeleteAccountRequest body = new ProfiloController.DeleteAccountRequest();
        body.password = "passwordSbagliata";

        mockMvc.perform(delete("/api/profilo")
                .with(user(mockUser))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        verify(utenteRepository, never()).deleteById(anyInt());
    }
    
    @Test 
    void testDeleteAccountUserNotFound() throws Exception {
        CustomUserDetails mockUser = getMockUser();

        when(utenteRepository.findById(1)).thenReturn(Optional.empty());

        ProfiloController.DeleteAccountRequest body = new ProfiloController.DeleteAccountRequest();
        body.password = "passwordGiusta";

        mockMvc.perform(delete("/api/profilo")
                .with(user(mockUser))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());

        verify(utenteRepository, never()).deleteById(anyInt());
    }

    @Test 
    void testCambiaPasswordSuccess() throws Exception {
        CustomUserDetails mockUser = getMockUser();
        UtenteEntity utente = new UtenteEntity(1, "testuser", "testmail@example.com", "passwordHashata", "ROLE_USER");

        when(utenteRepository.findById(1)).thenReturn(Optional.of(utente));
        when(passwordEncoder.matches("passwordVecchia", "passwordHashata")).thenReturn(true);
        when(passwordEncoder.encode("passwordNuova")).thenReturn("nuovaPasswordHashata");

        ProfiloController.CambioPasswordRequest body = new ProfiloController.CambioPasswordRequest();
        body.vecchiaPassword = "passwordVecchia";
        body.nuovaPassword = "passwordNuova";

        mockMvc.perform(put("/api/profilo/password")
                .with(user(mockUser))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
        verify(utenteRepository, times(1)).updatePassword(1, "nuovaPasswordHashata");
    }

    @Test 
    void testCambiaPasswordWrongOldPassword() throws Exception {
        CustomUserDetails mockUser = getMockUser();
        UtenteEntity utente = new UtenteEntity(1, "testuser", "testmail@example.com", "passwordHashata", "ROLE_USER");

        when(utenteRepository.findById(1)).thenReturn(Optional.of(utente));
        when(passwordEncoder.matches("passwordVecchiaSbagliata", "passwordHashata")).thenReturn(false);

        ProfiloController.CambioPasswordRequest body = new ProfiloController.CambioPasswordRequest();
        body.vecchiaPassword = "passwordVecchiaSbagliata";
        body.nuovaPassword = "passwordNuova";

        mockMvc.perform(put("/api/profilo/password")
                .with(user(mockUser))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        verify(utenteRepository, never()).updatePassword(anyInt(), anyString());
    }

    @Test 
    void testProfiloUpdateSuccess() throws Exception {
        CustomUserDetails mockUser = getMockUser();
        UtenteEntity utente = new UtenteEntity(1, "testuser", "testmail@example.com", "passwordHashata", "ROLE_USER");

        when(utenteRepository.findById(1)).thenReturn(Optional.of(utente));

        ProfiloController.ProfiloUpdateRequest body = new ProfiloController.ProfiloUpdateRequest();
        body.username = "nuovoUsername";

        mockMvc.perform(put("/api/profilo")
                .with(user(mockUser))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        verify(utenteRepository, times(1)).updateProfilo(1, "nuovoUsername", "testmail@example.com");
    }
}