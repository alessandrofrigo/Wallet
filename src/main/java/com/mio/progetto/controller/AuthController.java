package com.mio.progetto.controller;

import com.mio.progetto.model.UtenteEntity;
import com.mio.progetto.repository.UtenteRepository;
import com.mio.progetto.security.LimitatoreTentativi;
import com.mio.progetto.service.CustomUserDetails;
import com.mio.progetto.service.MailService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Duration DURATA_VALIDITA_TOKEN_RESET = Duration.ofHours(1);

    private final AuthenticationManager authenticationManager;
    private final UtenteRepository utenteRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final LimitatoreTentativi loginLimiter = new LimitatoreTentativi(5, Duration.ofMinutes(5));
    private final LimitatoreTentativi forgotPasswordLimiter = new LimitatoreTentativi(3, Duration.ofMinutes(15));

    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    public AuthController(AuthenticationManager authenticationManager, UtenteRepository utenteRepository, PasswordEncoder passwordEncoder, MailService mailService) {
        this.authenticationManager = authenticationManager;
        this.utenteRepository = utenteRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
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

        String ip = request.getRemoteAddr();
        if (loginLimiter.isBloccato(ip)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Troppi tentativi falliti. Riprova tra qualche minuto.");
        }

        // Risposta unificata (401 "Credenziali non valide.") sia per utente inesistente che
        // per password errata, per non rivelare quali username/email sono registrati.
        if (!utenteRepository.findByUsernameOrEmail(loginRequest.username, loginRequest.username).isPresent()){
            loginLimiter.registraFallimento(ip);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Credenziali non valide.");
        }
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.username, loginRequest.password)
            );

            loginLimiter.registraSuccesso(ip);

            // Salva l'autenticazione nel contesto di sicurezza per la sessione corrente
            SecurityContext securityContext = SecurityContextHolder.getContext();
            securityContext.setAuthentication(authentication);

            // Crea la sessione HTTPServlet per far sì che il client riceva il cookie JSESSIONID
            HttpSession session = request.getSession(true);
            request.changeSessionId();
            session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);

            return ResponseEntity.ok("Login effettuato con successo");
        } catch (Exception e) {
            loginLimiter.registraFallimento(ip);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenziali non valide.");
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
        @Size(min = 8, max = 100, message = "La password deve contenere tra i 8 e i 100 caratteri")
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

    public static class ForgotPasswordRequest {
        @NotBlank(message = "Email non può essere vuota")
        @Pattern(regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+", message = "L'email non è valida")
        public String email;
    }

    private static final String MESSAGGIO_FORGOT_PASSWORD_GENERICO =
            "Se l'indirizzo è registrato, riceverai a breve un'email con le istruzioni per reimpostare la password.";

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest, HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        if (forgotPasswordLimiter.isBloccato(ip)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Troppe richieste. Riprova tra qualche minuto.");
        }
        forgotPasswordLimiter.registraFallimento(ip);

        String email = StringUtils.lowerCase(StringUtils.trimToNull(forgotPasswordRequest.email));
        utenteRepository.findByEmail(email).ifPresent(utente -> {
            byte[] tokenBytes = new byte[32];
            SECURE_RANDOM.nextBytes(tokenBytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
            String tokenHash = sha256Hex(token);

            utenteRepository.impostaResetToken(utente.getId(), tokenHash, Instant.now().plus(DURATA_VALIDITA_TOKEN_RESET));

            String link = frontendBaseUrl + "/index.html?resetToken=" + token;
            try {
                mailService.inviaEmailResetPassword(email, link);
            } catch (Exception e) {
                // Un errore di invio non deve mai trasparire nella risposta: altrimenti si
                // reintroduce la possibilità di distinguere email registrate da quelle inesistenti.
                // Lo logghiamo solo lato server per poterlo diagnosticare.
                log.error("Invio email di reset password fallito", e);
            }
        });

        // Risposta sempre identica, email trovata o meno, per evitare user enumeration.
        return ResponseEntity.ok(MESSAGGIO_FORGOT_PASSWORD_GENERICO);
    }

    public static class ResetPasswordRequest {
        @NotBlank(message = "Token mancante")
        public String token;

        @NotBlank(message = "Password non può essere vuota")
        @Size(min = 8, max = 100, message = "La password deve contenere tra i 8 e i 100 caratteri")
        public String nuovaPassword;
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest resetPasswordRequest) {
        String tokenHash = sha256Hex(resetPasswordRequest.token);

        return utenteRepository.findByResetTokenValido(tokenHash)
                .map(utente -> {
                    utenteRepository.updatePassword(utente.getId(), passwordEncoder.encode(resetPasswordRequest.nuovaPassword));
                    utenteRepository.pulisciResetToken(utente.getId());
                    return ResponseEntity.ok("Password aggiornata. Ora puoi accedere.");
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Link non valido o scaduto."));
    }

    private static String sha256Hex(String valore) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(valore.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 non disponibile", e);
        }
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
