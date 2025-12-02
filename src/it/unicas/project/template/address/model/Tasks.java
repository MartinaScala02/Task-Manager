package it.unicas.project.template.address.model;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;

/**
 * Model class for a Tasks.
 */
public class Tasks {

    private StringProperty titolo;
    private StringProperty descrizione;
    private StringProperty categoria;
    private StringProperty scadenza;
    private StringProperty priorita;
    private StringProperty data_creazione;
    private BooleanProperty completamento;
    private IntegerProperty idTask;  //wrapper
    private IntegerProperty idUtente;

    //private static String attributoStaticoDiEsempio;

    /**
     * Default constructor.
     */
    public Tasks() {
        this(null, null, null, null, null, null, false, null, null);
    }

    public Tasks(String titolo, String descrizione, String categoria, String scadenza, String priorita, String data_creazione, Boolean completamento, Integer idTask, Integer idUtente) {
        this.titolo = new SimpleStringProperty(titolo);
        this.descrizione = new SimpleStringProperty(descrizione);
        this.categoria = new SimpleStringProperty(categoria);
        this.scadenza = new SimpleStringProperty(scadenza);
        this.priorita = new SimpleStringProperty(priorita);
        this.completamento = new SimpleBooleanProperty(completamento);
        this.data_creazione = new SimpleStringProperty(data_creazione);
        if (idTask != null){
            this.idTask = new SimpleIntegerProperty(idTask);
        } else {
            this.idTask = null;
        }

        if (idUtente != null) {
            this.idUtente = new SimpleIntegerProperty(idUtente);
        } else {
            this.idUtente = new SimpleIntegerProperty(-1);
        }
    }

    /**
     * Constructor with some initial data.
     *
     * @param titolo
     * @param descrizione
     * @param scadenza
     */
    public Tasks(String titolo, String descrizione, String scadenza) {
        this.titolo = new SimpleStringProperty(titolo);
        this.descrizione = new SimpleStringProperty(descrizione);
        this.scadenza = new SimpleStringProperty(scadenza);
        // Some initial dummy data, just for convenient testing.
        this.titolo = new SimpleStringProperty("La mia Task");
        this.descrizione = new SimpleStringProperty("Descrizione della mia task");
        this.scadenza = new SimpleStringProperty("01-01-2026");
        this.idTask = null;
        this.idUtente = null;
    }


    public Integer getIdTask(){
        if (idTask == null){
            idTask = new SimpleIntegerProperty(-1);
        }
        return idTask.get();
    }

    public void setIdTask(Integer idTask) {
        if (this.idTask == null){
            this.idTask = new SimpleIntegerProperty();
        }
        this.idTask.set(idTask);
    }

    public Integer getIdUtente(){
        if (idUtente == null){
            idUtente = new SimpleIntegerProperty(-1);
        }
        return idUtente.get();
    }

    public void setIdUtente(Integer idUtente) {
        if (this.idUtente == null){
            this.idUtente = new SimpleIntegerProperty();
        }
        this.idUtente.set(idUtente);
    }

    public String getTitolo() {
        return titolo.get();
    }

    public void setTitolo(String titolo) {
        this.titolo.set(titolo);
    }

    public StringProperty titoloProperty() {
        return titolo;
    }

    public String getDescrizione() {
        return descrizione.get();
    }

    public void setDescrizione(String descrizione) {
        this.descrizione.set(descrizione);
    }

    public StringProperty descrizioneProperty() {
        return descrizione;
    }

    public String getCategoria() {
        return categoria.get();
    }

    public void setCategoria(String categoria) {
        this.categoria.set(categoria);
    }

    public StringProperty categoriaProperty() {
        return categoria;
    }

    public String getScadenza() {
        return scadenza.get();
    }

    public void setScadenza(String scadenza) {
        this.scadenza.set(scadenza);
    }

    public StringProperty scadenzaProperty() {
        return scadenza;
    }

    public String getPriorita() {
        return priorita.getValue();
    }

    public void setPriorita(String priorita) {
        this.priorita.set(priorita);
    }

    public StringProperty prioritaProperty() {
        return priorita;
    }

    public Boolean getCompletamento() {
        return completamento.get();
    }

    public void setCompletamento(Boolean completamento) {
        this.completamento.set(completamento);
    }

    public BooleanProperty completamentoProperty() {
        return completamento;
    }

    public String getData_creazione() {
        return data_creazione.get();
    }

    public void setData_creazione(String data_creazione) {
        this.data_creazione.set(data_creazione);
    }

    public StringProperty data_creazioneProperty() {
        return data_creazione;
    }


    public String toString(){
        return titolo.getValue() + ", " + descrizione.getValue() + ", " + categoria.getValue() + ", " + scadenza.getValue() + ", " + priorita.getValue() + ", " + completamento.getValue() + ", " + data_creazione.getValue() + ", (" + idTask.getValue() + ")";
    }


    public static void main(String[] args) {



        // https://en.wikipedia.org/wiki/Observer_pattern
        Tasks collega = new Tasks();
        collega.setTitolo("Titolo");
        MyChangeListener myChangeListener = new MyChangeListener();
        collega.titoloProperty().addListener(myChangeListener);
        collega.setTitolo("Titolo Aggiornato");


        collega.data_creazioneProperty().addListener(myChangeListener);

        collega.data_creazioneProperty().addListener(
                (ChangeListener) (o, oldVal, newVal) -> System.out.println("data_creazione property has changed!"));

        collega.data_creazioneProperty().addListener(
                (o, old, newVal)-> System.out.println("data_creazione property has changed! (Lambda implementation)")
        );


        collega.setData_creazione("25-12-2025");



        // Use Java Collections to create the List.
        List<Tasks> list = new ArrayList<>();

        // Now add observability by wrapping it with ObservableList.
        ObservableList<Tasks> observableList = FXCollections.observableList(list);
        observableList.addListener(
                (ListChangeListener) change -> System.out.println("Detected a change! ")
        );

        Tasks t1 = new Tasks();
        Tasks t2 = new Tasks();

        t1.titoloProperty().addListener(
                (o, old, newValue)->System.out.println("Ciao")
        );

        t1.setTitolo("Titolo 1");

        // Changes to the observableList WILL be reported.
        // This line will print out "Detected a change!"
        observableList.add(t1);

        // Changes to the underlying list will NOT be reported
        // Nothing will be printed as a result of the next line.
        observableList.add(t2);


        observableList.get(0).setTitolo("Nuovo valore");

        System.out.println("Size: "+observableList.size());

    }


}
