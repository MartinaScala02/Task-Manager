package it.unicas.project.template.address.model;

import javafx.beans.property.*;

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
     */
    public Tasks() {
        this(null, null, null, null, null, false, null, null, null);
    }

    /**
     * Costruttore COMPLETO usato dal DAOTasks.
     * Ordine parametri: titolo, descrizione, scadenza, priorita, data_creazione, completamento, idTask, idUtente, idCategoria
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

    // Getters e Setters per idCategoria
    public Integer getIdCategoria() {
        if (idCategoria == null) return null;
        return idCategoria.get();
    }

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

    public String getDurataTotaleDisplay() { return durataTotaleDisplay.get(); }
    public void setDurataTotaleDisplay(String durata) { this.durataTotaleDisplay.set(durata); }
    public StringProperty durataTotaleDisplayProperty() { return durataTotaleDisplay; }

    @Override
    public String toString() {
        return titolo.get();
    }


}
