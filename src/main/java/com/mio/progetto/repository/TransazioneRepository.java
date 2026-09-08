package com.mio.progetto.repository;

import com.mio.progetto.model.Categoria;
import com.mio.progetto.model.TipoTransazione;
import com.mio.progetto.model.TransazioneEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

@Repository
public class TransazioneRepository {

    private static final Logger log = LoggerFactory.getLogger(TransazioneRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public TransazioneRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<TransazioneEntity> rowMapper = (ResultSet rs, int rowNum) -> {
        TransazioneEntity t = new TransazioneEntity();
        t.setId(rs.getInt("id"));
        t.setDescrizione(rs.getString("descrizione"));
        
        String catStr = rs.getString("categoria");
        Categoria categoria = Categoria.ALTRO;
        if (catStr != null) {
            try {
                categoria = Categoria.valueOf(catStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Categoria non valida trovata nel database: {}. Impostata a ALTRO.", catStr);
            }
        }
        t.setCategoria(categoria);
        
        t.setSottocategoria(rs.getString("sottocategoria"));
        t.setImporto(rs.getBigDecimal("importo"));

        String tipoStr = rs.getString("tipo");
        TipoTransazione tipo = TipoTransazione.USCITA;
        if (tipoStr != null) {
            try {
                tipo = TipoTransazione.valueOf(tipoStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Tipo transazione non valido trovato nel database: {}. Impostato a USCITA.", tipoStr);
            }
        }
        t.setTipo(tipo);
        
        java.sql.Date sqlDate = rs.getDate("data");
        t.setData(sqlDate != null ? sqlDate.toLocalDate() : null);
        
        t.setUtenteId(rs.getInt("utente_id"));
        return t;
    };

    public List<TransazioneEntity> findAll(int utenteId) {
        String sql = "SELECT * FROM transazioni WHERE utente_id = ?";
        List<TransazioneEntity> risultati = jdbcTemplate.query(sql, rowMapper, utenteId);
        log.info("Recuperate {} transazioni dal database per l'utente {}", risultati.size(), utenteId);
        return risultati;
    }

    public List<TransazioneEntity> findAllPaginated(int utenteId, int page, int size) {
        String sql = "SELECT * FROM transazioni WHERE utente_id = ? LIMIT ? OFFSET ?";
        int offset = page * size;
        List<TransazioneEntity> risultati = jdbcTemplate.query(sql, rowMapper, utenteId, size, offset);
        log.info("Recuperate {} transazioni (pagina {}, dimensione {}) dal database per l'utente {}", risultati.size(), page, size, utenteId);
        return risultati;
    }

    public int insert(TransazioneEntity t) {
        String sql = "INSERT INTO transazioni (descrizione, importo, categoria, sottocategoria, tipo, data, utente_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        int rows = jdbcTemplate.update(sql,
                t.getDescrizione(),
                t.getImporto(),
                t.getCategoria().name(),
                t.getSottocategoria(),
                t.getTipo().name(),
                t.getData(),
                t.getUtenteId());
        log.info("Transazione ({}) inserita con successo: {}", t.getTipo(), t.getDescrizione());
        return rows;
    }

    public int deleteById(int id, int utenteId) {
        String sql = "DELETE FROM transazioni WHERE id = ? AND utente_id = ?";
        int rows = jdbcTemplate.update(sql, id, utenteId);
        if (rows > 0) {
            log.info("Transazione con ID {} eliminata", id);
        } else {
            log.warn("Nessuna transazione trovata con ID: {}", id);
        }
        return rows;
    }

    public int deleteByCategoria(String categoria, int utenteId) {
        String sql = "DELETE FROM transazioni WHERE categoria = ? AND utente_id = ?";
        int rows = jdbcTemplate.update(sql, categoria, utenteId);
        log.info("{} transazioni eliminate nella categoria: {}", rows, categoria);
        return rows;
    }

    public int deleteBeforeDate(LocalDate data, int utenteId) {
        String sql = "DELETE FROM transazioni WHERE data < ? AND utente_id = ?";
        int rows = jdbcTemplate.update(sql, data, utenteId);
        log.info("{} transazioni eliminate prima del {}", rows, data);
        return rows;
    }
}
