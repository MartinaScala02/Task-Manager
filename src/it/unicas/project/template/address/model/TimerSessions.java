package it.unicas.project.template.address.model;

import javafx.beans.property.*;
import java.time.LocalDateTime;

/**
 * Classe Model che rappresenta una sessione di timer.
 * <p>
 * Gestisce i dati relativi a un intervallo di tempo speso su un task.
 * Utilizza {@link LocalDateTime} incapsulato in {@link ObjectProperty} per gestire
 * le date in modo compatibile con JavaFX e il database.
 * </p>
 */
public class TimerSessions {

    private StringProperty nome;
    private ObjectProperty<LocalDateTime> inizio;
    private ObjectProperty<LocalDateTime> fine;
    private LongProperty durataSecondi;
    private IntegerProperty idSession;
    private IntegerProperty idTask;

    /**
     * Costruttore di default.
     * Inizializza un oggetto TimerSessions vuoto.
     */
    public TimerSessions() {
        this(null, null, null, 0, null, null);
    }


    /**
     * Costruttore completo per inizializzare una sessione.
     *
     * @param nome          Il nome o descrizione della sessione.
     * @param inizio        La data e ora di inizio (LocalDateTime).
     * @param fine          La data e ora di fine (LocalDateTime).
     * @param durataSecondi La durata totale in secondi.
     * @param idSession     L'ID della sessione (se null, viene impostato a -1).
     * @param idTask        L'ID del task associato (se null, viene impostato a -1).
     */
    public TimerSessions(String nome, LocalDateTime inizio, LocalDateTime fine, long durataSecondi, Integer idSession, Integer idTask) {
        this.nome = new SimpleStringProperty(nome);
        this.inizio = new SimpleObjectProperty<>(inizio);
        this.fine = new SimpleObjectProperty<>(fine);
        this.durataSecondi = new SimpleLongProperty(durataSecondi);
        this.idSession = new SimpleIntegerProperty(idSession != null ? idSession : -1);
        this.idTask = new SimpleIntegerProperty(idTask != null ? idTask : -1);
    }

    /**
     * Restituisce l'ID della sessione.
     * @return L'ID sessione.
     */
    public int getIdSession() { return idSession.get(); }

    /**
     * Imposta l'ID della sessione.
     * @param idSession Il nuovo ID.
     */
    public void setIdSession(int idSession) { this.idSession.set(idSession); }


    /**
     * Restituisce l'ID del task associato.
     * @return L'ID task.
     */
    public int getIdTask() { return idTask.get(); }

    /**
     * Imposta l'ID del task associato.
     * @param idTask Il nuovo ID task.
     */
    public void setIdTask(int idTask) { this.idTask.set(idTask); }


    /**
     * Restituisce il nome della sessione.
     * @return Il nome.
     */
    public String getNome() { return nome.get(); }

    /**
     * Imposta il nome della sessione.
     * @param nome Il nuovo nome.
     */
    public void setNome(String nome) { this.nome.set(nome); }


    /**
     * Restituisce la data di inizio.
     * @return Oggetto LocalDateTime di inizio.
     */
    public LocalDateTime getInizio() { return inizio.get(); }

    /**
     * Imposta la data di inizio.
     * @param inizio Nuovo LocalDateTime.
     */
    public void setInizio(LocalDateTime inizio) { this.inizio.set(inizio); }

    /**
     * Restituisce la data di fine.
     * @return Oggetto LocalDateTime di fine.
     */
    public LocalDateTime getFine() { return fine.get(); }

    /**
     * Imposta la data di fine.
     * @param fine Nuovo LocalDateTime.
     */
    public void setFine(LocalDateTime fine) { this.fine.set(fine); }


    /**
     * Restituisce la durata in secondi.
     * @return Durata (long).
     */
    public long getDurataSecondi() { return durataSecondi.get(); }


    /**
     * Restituisce la durata formattata come stringa "HH:mm:ss".
     * <p>
     * Calcola ore, minuti e secondi partendo dal valore in secondi.
     * </p>
     *
     * @return Stringa formattata (es. "01:30:15").
     */
    public String getDurataFormattata() {
        long totaleSecondi = getDurataSecondi();

        long ore = totaleSecondi / 3600;
        long resto = totaleSecondi % 3600;
        long minuti = resto / 60;
        long secondi = resto % 60;

        // Restituisce una stringa tipo "02:15:30"
        return String.format("%02d:%02d:%02d", ore, minuti, secondi);
    }
}