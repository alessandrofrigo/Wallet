package com.mio.progetto.repository;

import com.mio.progetto.model.Categoria;
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
        t.setCategoria(Categoria.valueOf(rs.getString("categoria")));
        t.setSottocategoria(rs.getString("sottocategoria"));
        t.setImporto(rs.getBigDecimal("importo"));
        t.setData(rs.getDate("data").toLocalDate());
        t.setUtenteId(rs.getInt("utente_id"));
        return t;
    };

    public List<TransazioneEntity> findAll(int utenteId) {
        String sql = "SELECT * FROM transazioni WHERE utente_id = ?";
        List<TransazioneEntity> risultati = jdbcTemplate.query(sql, rowMapper, utenteId);
        log.info("Recuperate {} transazioni dal database per l'utente {}", risultati.size(), utenteId);
        return risultati;
    }

    public int insert(TransazioneEntity t) {
        String sql = "INSERT INTO transazioni (descrizione, importo, categoria, sottocategoria, data, utente_id) VALUES (?, ?, ?, ?, ?, ?)";
        int rows = jdbcTemplate.update(sql,
                t.getDescrizione(),
                t.getImporto(),
                t.getCategoria().name(),
                t.getSottocategoria(),
                t.getData(),
                t.getUtenteId());
        log.info("Transazione inserita con successo: {}", t.getDescrizione());
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

    public int deleteBeforeDate(String data, int utenteId) {
        String sql = "DELETE FROM transazioni WHERE data < ? AND utente_id = ?";
        int rows = jdbcTemplate.update(sql, data, utenteId);
        log.info("{} transazioni eliminate prima del {}", rows, data);
        return rows;
    }
}
