package com.mio.progetto.controller;

import com.mio.progetto.model.UtenteEntity;
import com.mio.progetto.repository.UtenteRepository;
import com.mio.progetto.service.CustomUserDetails;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UtenteRepository utenteRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager, UtenteRepository utenteRepository, PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.utenteRepository = utenteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public static class LoginRequest {
        @NotBlank(message = "Username non può essere vuoto")
        public String username;

        @NotBlank(message = "Password non può essere vuota")
        public String password;

        //@NotBlank (message = "Email non può essere vuota")
        public String email;
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        
        // Se lo username non esiste, blocca il login e invita alla registrazione (404),
        // distinguendolo dal caso "password errata" (401).
        if (!utenteRepository.findByUsernameOrEmail(loginRequest.username, loginRequest.username).isPresent()){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Utente non registrato. Effettua prima la registrazione.");
        }
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.username, loginRequest.password)
            );

            // Salva l'autenticazione nel contesto di sicurezza per la sessione corrente
            SecurityContext securityContext = SecurityContextHolder.getContext();
            securityContext.setAuthentication(authentication);

            // Crea la sessione HTTPServlet per far sì che il client riceva il cookie JSESSIONID
            HttpSession session = request.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);

            return ResponseEntity.ok("Login effettuato con successo");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenziali non valide");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok("Logout effettuato");
    }

    public static class RegisterRequest {
        // @NotBlank(message = "Username non può essere vuoto")
        @Size(min = 3, max = 50, message = "L'username deve contenere tra i 3 e i 50 caratteri")
        @Pattern(regexp = "^[^\\s@]*$", message = "L'username non può contenere spazi o '@'")
        public String username;

        @NotBlank(message = "Password non può essere vuota")
        @Size(min = 4, max = 100, message = "La password deve contenere tra i 4 e i 100 caratteri")
        public String password;

        // @NotBlank(message = "Email non può essere vuota")
        @Size(max = 255, message = "L'email deve contenere al massimo 255 caratteri")
        @Pattern(regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+", message = "L'email non è valida")
        public String email;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest registerRequest) {

        registerRequest.username = StringUtils.trimToNull(registerRequest.username);
        registerRequest.email = StringUtils.lowerCase(StringUtils.trimToNull(registerRequest.email));

        if (utenteRepository.findByUsername(registerRequest.username).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Username già in uso");
        }
        if(utenteRepository.findByEmail(registerRequest.email).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email già in uso");
        }
        if(registerRequest.username == null && registerRequest.email == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Username o email non possono essere vuoti");
        }


        UtenteEntity nuovoUtente = new UtenteEntity();
        if(registerRequest.username != null) {
            nuovoUtente.setUsername(registerRequest.username);
        } 
        if(registerRequest.email != null) {
            nuovoUtente.setEmail(registerRequest.email);
        }
        // Hash della password prima di salvarla
        nuovoUtente.setPassword(passwordEncoder.encode(registerRequest.password));
        nuovoUtente.setRuolo("ROLE_USER");

        utenteRepository.insert(nuovoUtente);
        return ResponseEntity.status(HttpStatus.CREATED).body("Utente registrato con successo");
    }

    @GetMapping("/me")
    public ResponseEntity<UtenteEntity> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Optional<UtenteEntity> utente = utenteRepository.findByUsernameOrEmail(userDetails.getUsername(), userDetails.getUsername());
        return utente.map(u -> {
            // Non restituire mai la password al frontend!
            u.setPassword(null);
            return ResponseEntity.ok(u);
        }).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
