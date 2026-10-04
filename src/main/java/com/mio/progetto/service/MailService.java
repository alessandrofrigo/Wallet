package com.mio.progetto.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String mittente;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void inviaEmailResetPassword(String destinatario, String link) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mittente);
        message.setTo(destinatario);
        message.setSubject("Reimposta la tua password - Gestione Spese");
        message.setText(
                "Hai richiesto di reimpostare la password del tuo account Gestione Spese.\n\n" +
                "Clicca sul link seguente per scegliere una nuova password (valido per 1 ora):\n" +
                link + "\n\n" +
                "Se non hai richiesto tu questa operazione, ignora semplicemente questa email."
        );
        mailSender.send(message);
    }
}
