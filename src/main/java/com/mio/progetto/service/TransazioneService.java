package com.mio.progetto.service;

import com.mio.progetto.model.TransazioneEntity;
import com.mio.progetto.repository.TransazioneRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class TransazioneService {

    private static final Logger log = LoggerFactory.getLogger(TransazioneService.class);

    private final TransazioneRepository transazioneRepository;

    public TransazioneService(TransazioneRepository transazioneRepository) {
        this.transazioneRepository = transazioneRepository;
    }

    public List<TransazioneEntity> getAllTransazioni(int utenteId) {
        return transazioneRepository.findAll(utenteId);
    }

    public List<TransazioneEntity> getAllTransazioniPaginated(int utenteId, int page, int size) {
        return transazioneRepository.findAllPaginated(utenteId, page, size);
    }

    public void insertTransazione(TransazioneEntity transazioneEntity) {
        transazioneRepository.insert(transazioneEntity);
    }

    public int deleteTransazioneById(int id, int utenteId) {
        return transazioneRepository.deleteById(id, utenteId);
    }

    public int deleteTransazioniByCategoria(String categoria, int utenteId) {
        return transazioneRepository.deleteByCategoria(categoria, utenteId);
    }

    public int deleteTransazioniBeforeDate(LocalDate data, int utenteId) {
        return transazioneRepository.deleteBeforeDate(data, utenteId);
    }
}
