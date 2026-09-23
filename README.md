# Gestione Spesa

Un'applicazione per la gestione delle spese personali che permette di registrare le transazioni finanziarie in un database locale e di analizzare l'andamento delle spese tramite grafici interattivi e un'interfaccia web.

## Descrizione del Progetto

L'applicativo si basa su un'architettura che prevede un backend sviluppato in Java con **Spring Boot** per esporre le API RESTful e la connessione ai dati. L'interfaccia utente (frontend) è stata realizzata in HTML, CSS e JavaScript per garantire l'interazione dinamica (es. consultazione delle spese suddivise per categorie, visualizzazione di grafici, e la gestione del login/autenticazione). I dati sono persistiti in un database **MySQL**.

## Specifiche e Tecnologie Utilizzate

### Backend
- **Linguaggio**: Java 17
- **Framework Principale**: Spring Boot 3.1.5 (Spring Web)
- **Gestione Dipendenze**: Maven

### Database
- **RDBMS**: MySQL
- **Driver**: MySQL Connector/J (8.0.33)

### Frontend
- **Tecnologie Base**: HTML5, CSS3, JavaScript (Vanilla JS/ES6)
- **Funzionalità**: Rendering dinamico delle liste di spesa, estrazione dei nomi delle categorie, e visualizzazione grafici.

## Feature Principali
- Connessione a un database locale MySQL precedentemente configurato.
- Inserimento, visualizzazione e storicizzazione delle spese.
- Gestione di associazioni spesa-categoria.
- Interfaccia grafica lato Web per analizzare l'andamento delle proprie spese in modo facile ed intuitivo.
