package com.mio.progetto.repository;

import com.mio.progetto.model.UtenteEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
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
        u.setEmail(rs.getString("email"));
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

    public Optional<UtenteEntity> findByEmail( String email) {
        String sql = "SELECT * FROM utenti WHERE email = ?";
        List<UtenteEntity> utenti = jdbcTemplate.query(sql, rowMapper, email);
        if (utenti.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(utenti.get(0));
    }

    public Optional<UtenteEntity> findByUsernameOrEmail(String username, String email) {
        String sql = "SELECT * FROM utenti WHERE username = ? OR email = ?";
        List<UtenteEntity> utenti = jdbcTemplate.query(sql, rowMapper, username, email);
        if (utenti.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(utenti.get(0));
    }
    public Optional<UtenteEntity> findById(int id) {
        String sql = "SELECT * FROM utenti WHERE id = ?";
        List<UtenteEntity> utenti = jdbcTemplate.query(sql, rowMapper, id);
        if (utenti.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(utenti.get(0));
    }
    public Optional<UtenteEntity> updateProfilo(int id, String username, String email) {
        String sql = "UPDATE utenti SET username = ?, email = ? WHERE id = ?";
        int rowsAffected = jdbcTemplate.update(sql, username, email, id);
        if (rowsAffected > 0) {
            return findById(id);
        }
        return Optional.empty();
    }

    public Optional<UtenteEntity> updatePassword(int id, String password) {
        String sql = "UPDATE utenti SET password = ? WHERE id = ?";
        int rowAffected = jdbcTemplate.update(sql,password,id);
        if(rowAffected > 0){
            return findById(id);
        }
        return Optional.empty();
    }

    public Optional<UtenteEntity> deleteById(int id){
        String sql = "DELETE FROM utenti WHERE id = ?";
        int rowsAffected = jdbcTemplate.update(sql, id);
        if(rowsAffected > 0){
            return Optional.empty();
        }
        return Optional.empty();
    }

    public int insert(UtenteEntity u) {
        String sql = "INSERT INTO utenti (username, email, password, ruolo) VALUES (?, ?, ?, ?)";
        return jdbcTemplate.update(sql, u.getUsername(), u.getEmail(), u.getPassword(), u.getRuolo());
    }

    public void impostaResetToken(int id, String tokenHash, Instant scadenza) {
        String sql = "UPDATE utenti SET reset_token_hash = ?, reset_token_expiry = ? WHERE id = ?";
        jdbcTemplate.update(sql, tokenHash, Timestamp.from(scadenza), id);
    }

    public Optional<UtenteEntity> findByResetTokenValido(String tokenHash) {
        String sql = "SELECT * FROM utenti WHERE reset_token_hash = ? AND reset_token_expiry > NOW()";
        List<UtenteEntity> utenti = jdbcTemplate.query(sql, rowMapper, tokenHash);
        if (utenti.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(utenti.get(0));
    }

    public void pulisciResetToken(int id) {
        String sql = "UPDATE utenti SET reset_token_hash = NULL, reset_token_expiry = NULL WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
}
