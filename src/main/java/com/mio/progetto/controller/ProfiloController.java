package com.mio.progetto.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mio.progetto.repository.UtenteRepository;
import com.mio.progetto.service.CustomUserDetails;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import com.mio.progetto.model.UtenteEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.security.crypto.password.PasswordEncoder;

@RestController 
@RequestMapping ("/api/profilo")
public class ProfiloController {

    private final UtenteRepository utenteRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfiloController(UtenteRepository utenteRepository, PasswordEncoder passwordEncoder) {
        this.utenteRepository = utenteRepository;
        this.passwordEncoder = passwordEncoder;
    }


    public static class ProfiloUpdateRequest {
        @Size(min = 3, max = 50, message = "L'username deve contenere tra i 3 e i 50 caratteri")
        @Pattern(regexp = "^[^\\s@]*$", message = "L'username non può contenere spazi o '@'")
        public String username;

        @Size(max = 255, message = "L'email deve contenere al massimo 255 caratteri")
        @Pattern(regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+", message = "L'email non è valida")
        public String email;
    }

    @PutMapping
    public ResponseEntity<String> ProfiloUpdate(@RequestBody @Valid ProfiloUpdateRequest request, @AuthenticationPrincipal CustomUserDetails userDetails, HttpServletRequest httpRequest) {
        int id = userDetails.getId();
        
        // Update the username in the database
        //devo trovare l'utente con quell'id e poi aggiornare il suo username
        Optional<UtenteEntity> currentUser = utenteRepository.findById(id);
        
        if (!currentUser.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body("Utente non trovato.");
        }
        else{
            UtenteEntity user = currentUser.get();
            //Adesso capiamo cosa aggiornare, se l'username o l'email
            if(request.username != null && request.email != null){
                //controllo unicità di entrambi
                Optional<UtenteEntity> userWithSameUsername = utenteRepository.findByUsername(request.username);
                Optional<UtenteEntity> userWithSameEmail = utenteRepository.findByEmail(request.email);

                boolean usernameGiaPreso = userWithSameUsername.isPresent() && userWithSameUsername.get().getId() != id;
                boolean emailGiaPresa = userWithSameEmail.isPresent() && userWithSameEmail.get().getId() != id;

                if (usernameGiaPreso || emailGiaPresa) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Username e Email già in uso.");
                }
                utenteRepository.updateProfilo(id, request.username, request.email);
                UtenteEntity utenteAggiornato = utenteRepository.findById(id).get(); // rileggi i dati freschi dopo l'update
                CustomUserDetails nuovoUserDetails = new CustomUserDetails(utenteAggiornato);

                Authentication nuovaAuth = new UsernamePasswordAuthenticationToken(
                        nuovoUserDetails, null, nuovoUserDetails.getAuthorities());

                SecurityContext securityContext = SecurityContextHolder.getContext();
                securityContext.setAuthentication(nuovaAuth);

                HttpSession session = httpRequest.getSession(true);
                session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);
                return ResponseEntity.status(HttpStatus.OK)
                    .body("Profilo aggiornato con successo!");

            }
            if(request.username != null){
                //cambia solo username e controllo unicità
                Optional<UtenteEntity> userWithSameUsername = utenteRepository.findByUsername(request.username);
                if(userWithSameUsername.isPresent() && userWithSameUsername.get().getId() != id){
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Username già in uso.");
                }
                else{                   
                    utenteRepository.updateProfilo(id, request.username, user.getEmail());
                    UtenteEntity utenteAggiornato = utenteRepository.findById(id).get(); // rileggi i dati freschi dopo l'update
                    CustomUserDetails nuovoUserDetails = new CustomUserDetails(utenteAggiornato);

                    Authentication nuovaAuth = new UsernamePasswordAuthenticationToken(
                            nuovoUserDetails, null, nuovoUserDetails.getAuthorities());

                    SecurityContext securityContext = SecurityContextHolder.getContext();
                    securityContext.setAuthentication(nuovaAuth);

                    HttpSession session = httpRequest.getSession(true);
                    session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);

                    return ResponseEntity.status(HttpStatus.OK)
                        .body("Profilo aggiornato con successo!");
                }
            }
            else if(request.email != null){
                //cambia solo email e controllo unicità
                Optional<UtenteEntity> userWithSameEmail = utenteRepository.findByEmail(request.email);
                if(userWithSameEmail.isPresent() && userWithSameEmail.get().getId() != id){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Email già in uso.");
                }
                else{
                    utenteRepository.updateProfilo(id, user.getUsername(), request.email);
                    UtenteEntity utenteAggiornato = utenteRepository.findById(id).get(); // rileggi i dati freschi dopo l'update
                    CustomUserDetails nuovoUserDetails = new CustomUserDetails(utenteAggiornato);

                    Authentication nuovaAuth = new UsernamePasswordAuthenticationToken(
                            nuovoUserDetails, null, nuovoUserDetails.getAuthorities());

                    SecurityContext securityContext = SecurityContextHolder.getContext();
                    securityContext.setAuthentication(nuovaAuth);

                    HttpSession session = httpRequest.getSession(true);
                    session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);

                    return ResponseEntity.status(HttpStatus.OK)
                        .body("Profilo aggiornato con successo!");
                }
            }
            else{
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Nessun campo da aggiornare.");
            }
        } 
    }

    public static class CambioPasswordRequest {
        @NotBlank (message = "La password attuale non può essere vuota")
        public String vecchiaPassword;

        @NotBlank (message = "La nuova password non può essere vuota")
        @Size(min = 8, max = 100, message = "La nuova password deve contenere tra gli 8 e i 100 caratteri")
        public String nuovaPassword;
    }

    @PutMapping("password")
    public ResponseEntity<String> CambioPassword(@RequestBody @Valid CambioPasswordRequest request, @AuthenticationPrincipal CustomUserDetails userDetails, HttpServletRequest httpRequest) {
        //TODO: process PUT request
        int id = userDetails.getId();
        
        // Update the username in the database
        //devo trovare l'utente con quell'id e poi aggiornare il suo username
        Optional<UtenteEntity> currentUser = utenteRepository.findById(id);
        
        if (!currentUser.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body("Utente non trovato.");
        }

        //utente esiste quindi procedo ad aggiornare la password
        UtenteEntity user = currentUser.get();
        if (!passwordEncoder.matches(request.vecchiaPassword, user.getPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("La password attuale non è corretta.");
        }
        String nuovaPasswordHash = passwordEncoder.encode(request.nuovaPassword);
        utenteRepository.updatePassword(id, nuovaPasswordHash);
        return ResponseEntity.status(HttpStatus.OK).body("Password aggiornata con successo!");
    }

    public static class DeleteAccountRequest {
        @NotBlank(message = "La password non può essere vuota")
        public String password;
    }

    @DeleteMapping 
    public ResponseEntity<String> DeleteAccount(@RequestBody @Valid DeleteAccountRequest request, @AuthenticationPrincipal CustomUserDetails userDetails, HttpServletRequest httpRequest) {
        int id = userDetails.getId();
        
        Optional<UtenteEntity> currentUser = utenteRepository.findById(id);
        
        if (!currentUser.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body("Utente non trovato.");
        }

        UtenteEntity user = currentUser.get();
        if (!passwordEncoder.matches(request.password, user.getPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("La password non è corretta.");
        }
        utenteRepository.deleteById(id);
        // Invalidate the session and clear the security context
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();

        return ResponseEntity.status(HttpStatus.OK).body("Account eliminato con successo!");
    }
}
