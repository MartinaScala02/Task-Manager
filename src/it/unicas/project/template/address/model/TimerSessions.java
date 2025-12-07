package it.unicas.project.template.address.model;

import javafx.beans.property.*;
import java.time.LocalDateTime;

public class TimerSessions {

    private StringProperty nome;
    private ObjectProperty<LocalDateTime> inizio;
    private ObjectProperty<LocalDateTime> fine;
    private LongProperty durataSecondi;
    private IntegerProperty idSession;
    private IntegerProperty idTask;

    // Costruttore vuoto
    public TimerSessions() {
        this(null, null, null, 0, null, null);
    }

    // Costruttore completo
    public TimerSessions(String nome, LocalDateTime inizio, LocalDateTime fine, long durataSecondi, Integer idSession, Integer idTask) {
        this.nome = new SimpleStringProperty(nome);
        this.inizio = new SimpleObjectProperty<>(inizio);
        this.fine = new SimpleObjectProperty<>(fine);
        this.durataSecondi = new SimpleLongProperty(durataSecondi);
        this.idSession = new SimpleIntegerProperty(idSession != null ? idSession : -1);
        this.idTask = new SimpleIntegerProperty(idTask != null ? idTask : -1);
    }

    public int getIdSession() { return idSession.get(); }
    public void setIdSession(int idSession) { this.idSession.set(idSession); }
    public IntegerProperty idSessionProperty() { return idSession; }

    public int getIdTask() { return idTask.get(); }
    public void setIdTask(int idTask) { this.idTask.set(idTask); }
    public IntegerProperty idTaskProperty() { return idTask; }

    public String getNome() { return nome.get(); }
    public void setNome(String nome) { this.nome.set(nome); }
    public StringProperty nomeProperty() { return nome; }

    public LocalDateTime getInizio() { return inizio.get(); }
    public void setInizio(LocalDateTime inizio) { this.inizio.set(inizio); }
    public ObjectProperty<LocalDateTime> inizioProperty() { return inizio; }

    public LocalDateTime getFine() { return fine.get(); }
    public void setFine(LocalDateTime fine) { this.fine.set(fine); }
    public ObjectProperty<LocalDateTime> fineProperty() { return fine; }

    public long getDurataSecondi() { return durataSecondi.get(); }
    public void setDurataSecondi(long durataSecondi) { this.durataSecondi.set(durataSecondi); }
    public LongProperty durataSecondiProperty() { return durataSecondi; }

    //Restituisce la durata formattata come stringa "HH:mm:ss".
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