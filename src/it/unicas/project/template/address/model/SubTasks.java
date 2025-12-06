package it.unicas.project.template.address.model;

import javafx.beans.property.*;

public class SubTasks {
    private StringProperty titolo;
    private StringProperty descrizione;
    private BooleanProperty completamento;
    private IntegerProperty idSubTask;
    private IntegerProperty idTask;

    /**
     * Costruttore di default.
     */
    public SubTasks() {
        this(null, null, false, null, null);
    }

    /**
     * Costruttore COMPLETO usato dal DAOTasks.
     * Ordine parametri: titolo, descrizione, completamento, idSubTask, idTask
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

    public Integer getIdSubTask() {
        if (idSubTask == null) return -1;
        return idSubTask.get();
    }
    public void setIdSubTask(Integer idSubTask) {
        if (this.idSubTask == null) this.idSubTask = new SimpleIntegerProperty();
        this.idSubTask.set(idSubTask);
    }

    public Integer getIdTask() {
        if (idTask == null) return -1;
        return idTask.get();
    }
    public void setIdTask(Integer idTask) {
        if (this.idTask == null) this.idTask = new SimpleIntegerProperty();
        this.idTask.set(idTask);
    }

    public String getTitolo() { return titolo.get(); }
    public void setTitolo(String titolo) { this.titolo.set(titolo); }
    public StringProperty titoloProperty() { return titolo; }

    public String getDescrizione() { return descrizione.get(); }
    public void setDescrizione(String descrizione) { this.descrizione.set(descrizione); }
    public StringProperty descrizioneProperty() { return descrizione; }

    public Boolean getCompletamento() { return completamento.get(); }
    public void setCompletamento(Boolean completamento) { this.completamento.set(completamento); }
    public BooleanProperty completamentoProperty() { return completamento; }

    @Override
    public String toString() {
        return titolo.get();
    }


}
