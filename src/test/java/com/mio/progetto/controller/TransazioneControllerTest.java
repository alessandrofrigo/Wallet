package com.mio.progetto.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mio.progetto.model.Categoria;
import com.mio.progetto.model.TipoTransazione;
import com.mio.progetto.model.TransazioneEntity;
import com.mio.progetto.model.UtenteEntity;
import com.mio.progetto.service.CustomUserDetails;
import com.mio.progetto.service.TransazioneService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {TransazioneController.class, com.mio.progetto.exception.GlobalExceptionHandler.class})
public class TransazioneControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransazioneService transazioneService;

    @Autowired
    private ObjectMapper objectMapper;

    private CustomUserDetails getMockUser() {
        UtenteEntity utente = new UtenteEntity(1, "testuser", "password", "ROLE_USER");
        return new CustomUserDetails(utente);
    }

    @Test
    void testGetAllTransazioniSuccess() throws Exception {
        CustomUserDetails mockUser = getMockUser();
        TransazioneEntity t = new TransazioneEntity(1, "Spesa", Categoria.CIBO, "Supermercato", TipoTransazione.USCITA, new BigDecimal("15.50"), LocalDate.now(), mockUser.getId());
        when(transazioneService.getAllTransazioniPaginated(mockUser.getId(), 0, 100)).thenReturn(List.of(t));

        mockMvc.perform(get("/api/transazioni")
                .with(user(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].descrizione").value("Spesa"))
                .andExpect(jsonPath("$[0].importo").value(15.50));
    }

    @Test
    void testGetAllTransazioniPaginatedSuccess() throws Exception {
        CustomUserDetails mockUser = getMockUser();
        TransazioneEntity t = new TransazioneEntity(1, "Spesa", Categoria.CIBO, "Supermercato", TipoTransazione.USCITA, new BigDecimal("15.50"), LocalDate.now(), mockUser.getId());
        when(transazioneService.getAllTransazioniPaginated(mockUser.getId(), 2, 20)).thenReturn(List.of(t));

        mockMvc.perform(get("/api/transazioni?page=2&size=20")
                .with(user(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].descrizione").value("Spesa"));
    }

    @Test
    void testInsertTransazioneSuccess() throws Exception {
        CustomUserDetails mockUser = getMockUser();
        TransazioneEntity t = new TransazioneEntity(0, "Pranzo", Categoria.CIBO, "Ristorante", TipoTransazione.USCITA, new BigDecimal("25.00"), LocalDate.now(), mockUser.getId());

        mockMvc.perform(post("/api/transazioni")
                .with(user(mockUser))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(t)))
                .andExpect(status().isCreated());

        verify(transazioneService, times(1)).insertTransazione(any(TransazioneEntity.class));
    }

    @Test
    void testInsertTransazioneValidationError_NegativeImporto() throws Exception {
        CustomUserDetails mockUser = getMockUser();
        // L'importo è 0.00, che è minore del minimo di 0.01
        TransazioneEntity t = new TransazioneEntity(0, "Pranzo", Categoria.CIBO, "Ristorante", TipoTransazione.USCITA, new BigDecimal("0.00"), LocalDate.now(), mockUser.getId());

        mockMvc.perform(post("/api/transazioni")
                .with(user(mockUser))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(t)))
                .andExpect(status().isBadRequest())
                // Verifica l'output dell'Exception Handler globale
                .andExpect(jsonPath("$.importo").value("L'importo deve essere maggiore di zero"));

        verify(transazioneService, never()).insertTransazione(any());
    }

    @Test
    void testInsertTransazioneCoherenceError_InvalidSubcategory() throws Exception {
        CustomUserDetails mockUser = getMockUser();
        // "Benzina" non appartiene alla categoria "CIBO"
        TransazioneEntity t = new TransazioneEntity(0, "Pranzo", Categoria.CIBO, "Benzina", TipoTransazione.USCITA, new BigDecimal("25.00"), LocalDate.now(), mockUser.getId());

        mockMvc.perform(post("/api/transazioni")
                .with(user(mockUser))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(t)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("non è valida per la categoria")));

        verify(transazioneService, never()).insertTransazione(any());
    }

    @Test
    void testDeleteTransazioneSuccess() throws Exception {
        CustomUserDetails mockUser = getMockUser();
        when(transazioneService.deleteTransazioneById(1, mockUser.getId())).thenReturn(1);

        mockMvc.perform(delete("/api/transazioni/1")
                .with(user(mockUser))
                .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDeleteTransazioniBeforeDateSuccess() throws Exception {
        CustomUserDetails mockUser = getMockUser();
        LocalDate targetDate = LocalDate.of(2026, 7, 4);

        mockMvc.perform(delete("/api/transazioni/data/2026-07-04")
                .with(user(mockUser))
                .with(csrf()))
                .andExpect(status().isNoContent());

        verify(transazioneService, times(1)).deleteTransazioniBeforeDate(targetDate, mockUser.getId());
    }

    @Test
    void testDeleteTransazioniBeforeDateTypeMismatchError() throws Exception {
        CustomUserDetails mockUser = getMockUser();

        mockMvc.perform(delete("/api/transazioni/data/invalid-date-format")
                .with(user(mockUser))
                .with(csrf()))
                .andExpect(status().isBadRequest())
                // Verifica l'eccezione intercettata dal GlobalExceptionHandler
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Formato data non valido")));

        verify(transazioneService, never()).deleteTransazioniBeforeDate(any(), anyInt());
    }
}
