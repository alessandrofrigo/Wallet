package com.mio.progetto.repository;

import com.mio.progetto.model.UtenteEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;

@Repository
public class UtenteRepository {

    private final JdbcTemplate jdbcTemplate;

    public UtenteRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<UtenteEntity> rowMapper = (ResultSet rs, int rowNum) -> {
        UtenteEntity u = new UtenteEntity();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setPassword(rs.getString("password"));
        u.setRuolo(rs.getString("ruolo"));
        return u;
    };

    public Optional<UtenteEntity> findByUsername(String username) {
        String sql = "SELECT * FROM utenti WHERE username = ?";
        List<UtenteEntity> utenti = jdbcTemplate.query(sql, rowMapper, username);
        if (utenti.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(utenti.get(0));
    }

    public int insert(UtenteEntity u) {
        String sql = "INSERT INTO utenti (username, password, ruolo) VALUES (?, ?, ?)";
        return jdbcTemplate.update(sql, u.getUsername(), u.getPassword(), u.getRuolo());
    }
}
