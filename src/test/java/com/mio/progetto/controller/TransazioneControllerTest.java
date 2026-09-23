package com.mio.progetto.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mio.progetto.model.Categoria;
import com.mio.progetto.model.PaginaTransazioni;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
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
        UtenteEntity utente = new UtenteEntity(1, "testuser", "testmail@example.com", "password", "ROLE_USER");
        return new CustomUserDetails(utente);
    }

    @Test
    void testGetAllTransazioniSuccess() throws Exception {
        CustomUserDetails mockUser = getMockUser();
        TransazioneEntity t = new TransazioneEntity(1, "Spesa", Categoria.CIBO, "Supermercato", TipoTransazione.USCITA, new BigDecimal("15.50"), LocalDate.now(), mockUser.getId());
        PaginaTransazioni pagina = new PaginaTransazioni(List.of(t), 0, 100, 1);
        when(transazioneService.getTransazioniFiltrate(mockUser.getId(), null, null, null, null, null, "data", "desc", 0, 100))
                .thenReturn(pagina);

        mockMvc.perform(get("/api/transazioni")
                .with(user(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenuto[0].descrizione").value("Spesa"))
                .andExpect(jsonPath("$.contenuto[0].importo").value(15.50))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(100))
                .andExpect(jsonPath("$.totale").value(1))
                .andExpect(jsonPath("$.totalePagine").value(1));
    }

    @Test
    void testGetAllTransazioniPaginatedSuccess() throws Exception {
        CustomUserDetails mockUser = getMockUser();
        TransazioneEntity t = new TransazioneEntity(1, "Spesa", Categoria.CIBO, "Supermercato", TipoTransazione.USCITA, new BigDecimal("15.50"), LocalDate.now(), mockUser.getId());
        PaginaTransazioni pagina = new PaginaTransazioni(List.of(t), 2, 20, 45);
        when(transazioneService.getTransazioniFiltrate(mockUser.getId(), null, null, null, null, null, "data", "desc", 2, 20))
                .thenReturn(pagina);

        mockMvc.perform(get("/api/transazioni?page=2&size=20")
                .with(user(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenuto[0].descrizione").value("Spesa"))
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.totalePagine").value(3));
    }

    @Test
    void testGetAllTransazioniConFiltriEOrdinamento() throws Exception {
        CustomUserDetails mockUser = getMockUser();
        TransazioneEntity t = new TransazioneEntity(1, "Pranzo", Categoria.CIBO, "Ristorante", TipoTransazione.USCITA, new BigDecimal("25.00"), LocalDate.of(2026, 1, 15), mockUser.getId());
        PaginaTransazioni pagina = new PaginaTransazioni(List.of(t), 0, 100, 1);
        when(transazioneService.getTransazioniFiltrate(mockUser.getId(), "CIBO", "USCITA", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), "pranzo", "importo", "asc", 0, 100))
                .thenReturn(pagina);

        mockMvc.perform(get("/api/transazioni")
                .param("categoria", "CIBO")
                .param("tipo", "USCITA")
                .param("dataDa", "2026-01-01")
                .param("dataA", "2026-01-31")
                .param("testo", "pranzo")
                .param("sortBy", "importo")
                .param("sortDir", "asc")
                .with(user(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenuto[0].descrizione").value("Pranzo"));

        verify(transazioneService, times(1)).getTransazioniFiltrate(mockUser.getId(), "CIBO", "USCITA", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), "pranzo", "importo", "asc", 0, 100);
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
    void testUpdateTransazioneSuccess() throws Exception {
        CustomUserDetails mockUser = getMockUser();
        TransazioneEntity t = new TransazioneEntity(1, "Pranzo aggiornato", Categoria.CIBO, "Ristorante", TipoTransazione.USCITA, new BigDecimal("30.00"), LocalDate.now(), mockUser.getId());
        when(transazioneService.updateTransazione(eq(1), any(TransazioneEntity.class), eq(mockUser.getId()))).thenReturn(1);

        mockMvc.perform(put("/api/transazioni/1")
                .with(user(mockUser))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(t)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descrizione").value("Pranzo aggiornato"));

        verify(transazioneService, times(1)).updateTransazione(eq(1), any(TransazioneEntity.class), eq(mockUser.getId()));
    }

    @Test
    void testUpdateTransazioneNotFound() throws Exception {
        CustomUserDetails mockUser = getMockUser();
        TransazioneEntity t = new TransazioneEntity(99, "Pranzo", Categoria.CIBO, "Ristorante", TipoTransazione.USCITA, new BigDecimal("30.00"), LocalDate.now(), mockUser.getId());
        when(transazioneService.updateTransazione(eq(99), any(TransazioneEntity.class), eq(mockUser.getId()))).thenReturn(0);

        mockMvc.perform(put("/api/transazioni/99")
                .with(user(mockUser))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(t)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateTransazioneCoherenceError_TipoCategoriaIncoerenti() throws Exception {
        CustomUserDetails mockUser = getMockUser();
        // Tipo ENTRATA ma categoria diversa da ENTRATE
        TransazioneEntity t = new TransazioneEntity(1, "Stipendio", Categoria.CIBO, "Supermercato", TipoTransazione.ENTRATA, new BigDecimal("1000.00"), LocalDate.now(), mockUser.getId());

        mockMvc.perform(put("/api/transazioni/1")
                .with(user(mockUser))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(t)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("deve avere la categoria ENTRATE")));

        verify(transazioneService, never()).updateTransazione(anyInt(), any(), anyInt());
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
