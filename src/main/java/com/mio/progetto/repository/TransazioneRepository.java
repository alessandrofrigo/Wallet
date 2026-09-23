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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    private static final Map<String, String> SORT_BY_COLONNE = Map.of(
            "data", "data",
            "importo", "importo",
            "categoria", "categoria",
            "descrizione", "descrizione"
    );

    private String costruisciWhere(int utenteId, String categoria, String tipo, LocalDate dataDa, LocalDate dataA, String testo, List<Object> parametri) {
        StringBuilder where = new StringBuilder(" WHERE utente_id = ?");
        parametri.add(utenteId);

        if (categoria != null && !categoria.isBlank()) {
            where.append(" AND categoria = ?");
            parametri.add(categoria);
        }
        if (tipo != null && !tipo.isBlank()) {
            where.append(" AND tipo = ?");
            parametri.add(tipo);
        }
        if (dataDa != null) {
            where.append(" AND data >= ?");
            parametri.add(dataDa);
        }
        if (dataA != null) {
            where.append(" AND data <= ?");
            parametri.add(dataA);
        }
        if (testo != null && !testo.isBlank()) {
            where.append(" AND LOWER(descrizione) LIKE LOWER(?)");
            parametri.add("%" + testo + "%");
        }
        return where.toString();
    }

    public long countFiltered(int utenteId, String categoria, String tipo, LocalDate dataDa, LocalDate dataA, String testo) {
        List<Object> parametri = new ArrayList<>();
        String where = costruisciWhere(utenteId, categoria, tipo, dataDa, dataA, testo, parametri);
        String sql = "SELECT COUNT(*) FROM transazioni" + where;
        Long totale = jdbcTemplate.queryForObject(sql, Long.class, parametri.toArray());
        return totale != null ? totale : 0L;
    }

    public List<TransazioneEntity> findAllFiltered(int utenteId, String categoria, String tipo, LocalDate dataDa, LocalDate dataA, String testo, String sortBy, String sortDir, int page, int size) {
        List<Object> parametri = new ArrayList<>();
        String where = costruisciWhere(utenteId, categoria, tipo, dataDa, dataA, testo, parametri);

        String colonnaOrdinamento = SORT_BY_COLONNE.getOrDefault(sortBy, "data");
        String direzioneOrdinamento = "ASC".equalsIgnoreCase(sortDir) ? "ASC" : "DESC";

        String sql = "SELECT * FROM transazioni" + where + " ORDER BY " + colonnaOrdinamento + " " + direzioneOrdinamento + " LIMIT ? OFFSET ?";
        parametri.add(size);
        parametri.add(page * size);

        List<TransazioneEntity> risultati = jdbcTemplate.query(sql, rowMapper, parametri.toArray());
        log.info("Recuperate {} transazioni filtrate (pagina {}, dimensione {}) per l'utente {}", risultati.size(), page, size, utenteId);
        return risultati;
    }

    public int update(int id, TransazioneEntity t, int utenteId) {
        String sql = "UPDATE transazioni SET descrizione = ?, importo = ?, categoria = ?, sottocategoria = ?, tipo = ?, data = ? WHERE id = ? AND utente_id = ?";
        int rows = jdbcTemplate.update(sql,
                t.getDescrizione(),
                t.getImporto(),
                t.getCategoria().name(),
                t.getSottocategoria(),
                t.getTipo().name(),
                t.getData(),
                id,
                utenteId);
        log.info("Transazione con ID {} aggiornata", id);
        return rows;
    }
}
