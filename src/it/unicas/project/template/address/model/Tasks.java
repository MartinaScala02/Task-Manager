package it.unicas.project.template.address.model;

import javafx.beans.property.*;

/**
 * Classe Model che rappresenta un'attività (Task) all'interno del sistema.
 * <p>
 * Utilizza i Property Wrapper di JavaFX (es. {@link StringProperty}, {@link IntegerProperty})
 * per consentire il binding automatico con l'interfaccia grafica (TableView, TextField, ecc.)
 * e la gestione degli eventi di modifica tramite Listener.
 * </p>
 */
public class Tasks {

    private StringProperty titolo;
    private StringProperty descrizione;
    private StringProperty scadenza;
    private StringProperty priorita;
    private StringProperty data_creazione;
    private ObjectProperty<Boolean> completamento;
    private IntegerProperty idTask;
    private IntegerProperty idUtente;
    private IntegerProperty idCategoria;
    private StringProperty durataTotaleDisplay;

    /**
     * Costruttore di default.
     * Inizializza un oggetto Tasks con valori nulli.
     */
    public Tasks() {

        this(null, null, null, null, null, null, null, null, null);
    }

    /**
     * Costruttore completo per inizializzare tutte le proprietà del Task.
     * Utilizzato principalmente dal DAO quando si recuperano i dati dal database.
     *
     * @param titolo         Il titolo del task.
     * @param descrizione    La descrizione del task.
     * @param scadenza       La data di scadenza (formato stringa).
     * @param priorita       Il livello di priorità (es. ALTA, MEDIA, BASSA).
     * @param data_creazione La data di creazione del task.
     * @param completamento  Stato del task (true = completato, false = aperto).
     * @param idTask         ID univoco del task (PK). Se null, viene impostato a -1.
     * @param idUtente       ID dell'utente proprietario del task (FK).
     * @param idCategoria    ID della categoria associata (FK). Se null, impostato a -1.
     */
    public Tasks(String titolo, String descrizione, String scadenza, String priorita, String data_creazione, Boolean completamento, Integer idTask, Integer idUtente, Integer idCategoria) {
        this.titolo = new SimpleStringProperty(titolo);
        this.descrizione = new SimpleStringProperty(descrizione);
        this.scadenza = new SimpleStringProperty(scadenza);
        this.priorita = new SimpleStringProperty(priorita);
        this.data_creazione = new SimpleStringProperty(data_creazione);
        this.completamento = new SimpleObjectProperty<>(completamento);
        this.durataTotaleDisplay = new SimpleStringProperty("00:00:00");

        if (idTask != null) {
            this.idTask = new SimpleIntegerProperty(idTask);
        } else {
            this.idTask = new SimpleIntegerProperty(-1); // o null se preferisci
        }

        if (idUtente != null) {
            this.idUtente = new SimpleIntegerProperty(idUtente);
        } else {
            this.idUtente = new SimpleIntegerProperty(-1);
        }

        // Gestione idCategoria
        if (idCategoria != null) {
            this.idCategoria = new SimpleIntegerProperty(idCategoria);
        } else {
            this.idCategoria = new SimpleIntegerProperty(-1); // Indica "nessuna categoria"
        }
    }

    /**
     * Restituisce l'ID della categoria.
     * @return L'ID categoria o null se non impostato.
     */
    public Integer getIdCategoria() {
        if (idCategoria == null) return null;
        return idCategoria.get();
    }

    /**
     * Imposta l'ID della categoria.
     * @param idCategoria Il nuovo ID categoria.
     */
    public void setIdCategoria(Integer idCategoria) {
        if (this.idCategoria == null) {
            this.idCategoria = new SimpleIntegerProperty();
        }
        // Gestiamo il caso in cui passiamo null
        if (idCategoria == null) {
            this.idCategoria.set(-1);
        } else {
            this.idCategoria.set(idCategoria);
        }
    }

    /**
     * Restituisce la property dell'ID categoria.
     * @return L'oggetto IntegerProperty.
     */
    public IntegerProperty idCategoriaProperty() {
        return idCategoria;
    }


    public Integer getIdTask() {
        if (idTask == null) return -1;
        return idTask.get();
    }
    public void setIdTask(Integer idTask) {
        if (this.idTask == null) this.idTask = new SimpleIntegerProperty();
        this.idTask.set(idTask);
    }

    public Integer getIdUtente() {
        if (idUtente == null) return -1;
        return idUtente.get();
    }
    public void setIdUtente(Integer idUtente) {
        if (this.idUtente == null) this.idUtente = new SimpleIntegerProperty();
        this.idUtente.set(idUtente);
    }

    public String getTitolo() { return titolo.get(); }
    public void setTitolo(String titolo) { this.titolo.set(titolo); }
    public StringProperty titoloProperty() { return titolo; }

    public String getDescrizione() { return descrizione.get(); }
    public void setDescrizione(String descrizione) { this.descrizione.set(descrizione); }
    public StringProperty descrizioneProperty() { return descrizione; }

    public String getScadenza() { return scadenza.get(); }
    public void setScadenza(String scadenza) { this.scadenza.set(scadenza); }
    public StringProperty scadenzaProperty() { return scadenza; }

    public String getPriorita() { return priorita.get(); }
    public void setPriorita(String priorita) { this.priorita.set(priorita); }
    public StringProperty prioritaProperty() { return priorita; }

    public Boolean getCompletamento() { return completamento.get(); }
    public void setCompletamento(Boolean completamento) { this.completamento.set(completamento); }
    public ObjectProperty<Boolean> completamentoProperty() { return completamento; }

    public String getData_creazione() { return data_creazione.get(); }
    public void setData_creazione(String data_creazione) { this.data_creazione.set(data_creazione); }
    public StringProperty data_creazioneProperty() { return data_creazione; }

    /**
     * Restituisce la stringa formattata della durata totale.
     * @return Durata in formato "HH:mm:ss".
     */
    public String getDurataTotaleDisplay() { return durataTotaleDisplay.get(); }

    /**
     * Imposta la stringa formattata della durata totale.
     * @param durata La durata come stringa.
     */
    public void setDurataTotaleDisplay(String durata) { this.durataTotaleDisplay.set(durata); }

    /**
     * Restituisce la property della durata totale per la visualizzazione.
     * @return L'oggetto StringProperty.
     */
    public StringProperty durataTotaleDisplayProperty() { return durataTotaleDisplay; }

    @Override
    public String toString() {
        return titolo.get();
    }


}