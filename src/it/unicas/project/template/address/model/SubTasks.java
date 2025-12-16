package it.unicas.project.template.address.model;

import javafx.beans.property.*;

/**
 * Classe Model che rappresenta un sotto-task collegato a un Task principale.
 * <p>
 * Questa classe utilizza i Property Wrapper di JavaFX (es. {@link StringProperty}, {@link BooleanProperty})
 * per permettere il binding bidirezionale con l'interfaccia grafica (es. checkbox in una lista).
 * </p>
 */
public class SubTasks {
    private StringProperty titolo;
    private StringProperty descrizione;
    private BooleanProperty completamento;
    private IntegerProperty idSubTask;
    private IntegerProperty idTask;

    /**
     * Costruttore di default.
     * Inizializza un oggetto SubTasks vuoto con valori di default.
     */
    public SubTasks() {
        this(null, null, false, null, null);
    }

    /**
     * Costruttore COMPLETO usato dal DAOTasks.
     * Ordine parametri: titolo, descrizione, completamento, idSubTask, idTask
     *
     * @param titolo        Il titolo del sotto-task.
     * @param descrizione   La descrizione.
     * @param completamento Lo stato di completamento (true = completato, false = da fare).
     * @param idSubTask     L'ID univoco del sotto-task. Se null, inizializzato a -1.
     * @param idTask        L'ID del task padre. Se null, inizializzato a -1.
     */
    public SubTasks(String titolo, String descrizione, Boolean completamento, Integer idSubTask, Integer idTask) {
        this.titolo = new SimpleStringProperty(titolo);
        this.descrizione = new SimpleStringProperty(descrizione);
        this.completamento = new SimpleBooleanProperty(completamento);

        if (idSubTask != null) {
            this.idSubTask = new SimpleIntegerProperty(idSubTask);
        } else {
            this.idSubTask = new SimpleIntegerProperty(-1);
        }

        if (idTask != null) {
            this.idTask = new SimpleIntegerProperty(idTask);
        } else {
            this.idTask = new SimpleIntegerProperty(-1); // o null se preferisci
        }

    }

    /**
     * Restituisce l'ID del sotto-task.
     * @return L'ID come intero primitivo.
     */
    public Integer getIdSubTask() {
        if (idSubTask == null) return -1;
        return idSubTask.get();
    }

    /**
     * Imposta l'ID del sotto-task.
     * @param idSubTask Il nuovo ID.
     */
    public void setIdSubTask(Integer idSubTask) {
        if (this.idSubTask == null) this.idSubTask = new SimpleIntegerProperty();
        this.idSubTask.set(idSubTask);
    }

    /**
     * Restituisce l'ID del task padre.
     * @return L'ID task come intero primitivo.
     */
    public Integer getIdTask() {
        if (idTask == null) return -1;
        return idTask.get();
    }

    /**
     * Imposta l'ID del task padre.
     * @param idTask Il nuovo ID task.
     */
    public void setIdTask(Integer idTask) {
        if (this.idTask == null) this.idTask = new SimpleIntegerProperty();
        this.idTask.set(idTask);
    }

    /**
     * Restituisce il titolo del sotto-task.
     * @return Il titolo.
     */
    public String getTitolo() { return titolo.get(); }

    /**
     * Imposta il titolo del sotto-task.
     * @param titolo Il nuovo titolo.
     */
    public void setTitolo(String titolo) { this.titolo.set(titolo); }

    /**
     * Restituisce la property del titolo per il binding JavaFX.
     * @return L'oggetto StringProperty.
     */
    public StringProperty titoloProperty() { return titolo; }

    /**
     * Restituisce la descrizione del sotto-task.
     * @return La descrizione.
     */
    public String getDescrizione() { return descrizione.get(); }

    /**
     * Imposta la descrizione del sotto-task.
     * @param descrizione La nuova descrizione.
     */
    public void setDescrizione(String descrizione) { this.descrizione.set(descrizione); }

    /**
     * Restituisce la property della descrizione per il binding JavaFX.
     * @return L'oggetto StringProperty.
     */
    public StringProperty descrizioneProperty() { return descrizione; }

    /**
     * Restituisce lo stato di completamento.
     * @return true se completato, false altrimenti.
     */
    public Boolean getCompletamento() { return completamento.get(); }

    /**
     * Imposta lo stato di completamento.
     * @param completamento Il nuovo stato (booleano).
     */
    public void setCompletamento(Boolean completamento) { this.completamento.set(completamento); }

    /**
     * Restituisce la property booleana di completamento per il binding (es. CheckBox).
     * @return L'oggetto BooleanProperty.
     */
    public BooleanProperty completamentoProperty() { return completamento; }

    /**
     * Restituisce una rappresentazione stringa dell'oggetto (il titolo).
     * @return Il titolo del sotto-task.
     */
    @Override
    public String toString() {
        return titolo.get();
    }


    /**
     * Metodo di utilità per impostare lo stato di completamento.
     * Equivalente a setCompletamento(boolean).
     * @param b Il nuovo stato.
     */
    public void setStato(boolean b) {
        this.completamento.set(b);
    }
}