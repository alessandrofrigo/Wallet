package com.mio.progetto.service;

import com.mio.progetto.model.Categoria;
import com.mio.progetto.model.TipoTransazione;
import com.mio.progetto.model.TransazioneEntity;
import com.mio.progetto.repository.TransazioneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransazioneServiceTest {

    @Mock
    private TransazioneRepository transazioneRepository;

    private TransazioneService transazioneService;

    @BeforeEach
    void setUp() {
        transazioneService = new TransazioneService(transazioneRepository);
    }

    @Test
    void testGetAllTransazioni() {
        int utenteId = 1;
        TransazioneEntity t = new TransazioneEntity(1, "Spesa", Categoria.CIBO, "Supermercato", TipoTransazione.USCITA, new BigDecimal("15.50"), LocalDate.now(), utenteId);
        when(transazioneRepository.findAll(utenteId)).thenReturn(List.of(t));

        List<TransazioneEntity> result = transazioneService.getAllTransazioni(utenteId);

        assertEquals(1, result.size());
        assertEquals("Spesa", result.get(0).getDescrizione());
        verify(transazioneRepository, times(1)).findAll(utenteId);
    }

    @Test
    void testGetAllTransazioniPaginated() {
        int utenteId = 1;
        int page = 0;
        int size = 10;
        TransazioneEntity t = new TransazioneEntity(1, "Spesa", Categoria.CIBO, "Supermercato", TipoTransazione.USCITA, new BigDecimal("15.50"), LocalDate.now(), utenteId);
        when(transazioneRepository.findAllPaginated(utenteId, page, size)).thenReturn(List.of(t));

        List<TransazioneEntity> result = transazioneService.getAllTransazioniPaginated(utenteId, page, size);

        assertEquals(1, result.size());
        assertEquals("Spesa", result.get(0).getDescrizione());
        verify(transazioneRepository, times(1)).findAllPaginated(utenteId, page, size);
    }

    @Test
    void testInsertTransazione() {
        TransazioneEntity t = new TransazioneEntity(1, "Spesa", Categoria.CIBO, "Supermercato", TipoTransazione.USCITA, new BigDecimal("15.50"), LocalDate.now(), 1);
        
        transazioneService.insertTransazione(t);

        verify(transazioneRepository, times(1)).insert(t);
    }

    @Test
    void testDeleteTransazioneById() {
        int id = 10;
        int utenteId = 2;
        when(transazioneRepository.deleteById(id, utenteId)).thenReturn(1);

        int rows = transazioneService.deleteTransazioneById(id, utenteId);

        assertEquals(1, rows);
        verify(transazioneRepository, times(1)).deleteById(id, utenteId);
    }

    @Test
    void testDeleteTransazioniBeforeDate() {
        LocalDate date = LocalDate.now();
        int utenteId = 1;
        when(transazioneRepository.deleteBeforeDate(date, utenteId)).thenReturn(5);

        int deleted = transazioneService.deleteTransazioniBeforeDate(date, utenteId);

        assertEquals(5, deleted);
        verify(transazioneRepository, times(1)).deleteBeforeDate(date, utenteId);
    }
}
