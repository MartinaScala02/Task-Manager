package it.unicas.project.template.address.model;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

/**
 * Model class for a Utenti.
 *
 * @author Mario Molinara
 */

//ciao Martina facciamo questa prova

public class Utenti {

    private StringProperty nome;
    private StringProperty cognome;
    private StringProperty username;
    //private StringProperty telefono;
    private StringProperty email;
    //private StringProperty compleanno;
    private StringProperty psw;
    private IntegerProperty idUtente;  //wrapper

    //private static String attributoStaticoDiEsempio;

    /**
     * Default constructor.
     */
    public Utenti() {
        this(null, null);
    }

    public Utenti(String nome, String cognome, String email, String psw, Integer idColleghi) {
        this.nome = new SimpleStringProperty(nome);
        this.cognome = new SimpleStringProperty(cognome);
        // this.telefono = new SimpleStringProperty(telefono);
        this.email = new SimpleStringProperty(email);
        //this.compleanno = new SimpleStringProperty(compleanno);
        if (idColleghi != null){
            this.idUtente = new SimpleIntegerProperty(idColleghi);
        } else {
            this.idUtente = null;
        }
        // impostare correttamente la password passata (evitare di inizializzare sempre vuoto)
        this.psw = new SimpleStringProperty(psw != null ? psw : "");
    }

    /**
     * Constructor with some initial data.
     *
     * @param nome
     * @param cognome
     */
    public Utenti(String nome, String cognome) {
        this.nome = new SimpleStringProperty(nome);
        this.cognome = new SimpleStringProperty(cognome);
        // Some initial dummy data, just for convenient testing.
        // this.telefono = new SimpleStringProperty("telefono");
        this.email = new SimpleStringProperty("email@email.com");
        this.psw = new SimpleStringProperty("psw");
        //this.compleanno = new SimpleStringProperty("24-10-2017");
        this.idUtente = null;
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

    public String getNome() {
        return nome.get();
    }

    public void setNome(String nome) {
        this.nome.set(nome);
    }

    public StringProperty nomeProperty() {
        return nome;
    }

    public String getCognome() {
        return cognome.get();
    }

    public void setCognome(String cognome) {
        this.cognome.set(cognome);
    }

    public StringProperty cognomeProperty() {
        return cognome;
    }

    /* public String getTelefono() {
        return telefono.get();
    }

    public void setTelefono(String telefono) {
        this.telefono.set(telefono);
    }

    public StringProperty telefonoProperty() {
        return telefono;
    }*/

    public String getEmail() {
        return email.get();
    }

    public void setEmail(String email) {
        this.email.set(email);
    }

    public StringProperty emailProperty() {
        return email;
    }

    public String getPsw() {
        return psw.get();
    }

    public void setPsw(String psw) {
        this.psw.set(psw);
    }
    /*public String getCompleanno() {
        return compleanno.getValue();
    }

    public void setCompleanno(String compleanno) {
        this.compleanno.set(compleanno);
    }

    public StringProperty compleannoProperty() {
        return compleanno;
    }*/


    public String toString(){
        return nome.getValue() + ", " + cognome.getValue() + ", " /*+ telefono.getValue() + ", " */ + email.getValue() + ", " + /*compleanno.getValue() + */", (" + idUtente.getValue() + ")";
    }


    public static void main(String[] args) {



        // https://en.wikipedia.org/wiki/Observer_pattern
        Utenti collega = new Utenti();
        collega.setNome("Ciao");
        MyChangeListener myChangeListener = new MyChangeListener();
        collega.nomeProperty().addListener(myChangeListener);
        collega.setNome("Mario");


        //collega.compleannoProperty().addListener(myChangeListener);

        /*collega.compleannoProperty().addListener(
                (ChangeListener) (o, oldVal, newVal) -> System.out.println("Compleanno property has changed!"));

        collega.compleannoProperty().addListener(
                (o, old, newVal)-> System.out.println("Compleanno property has changed! (Lambda implementation)")
        );*/


        // collega.setCompleanno("30-10-1971");



        // Use Java Collections to create the List.
        List<Utenti> list = new ArrayList<>();

        // Now add observability by wrapping it with ObservableList.
        ObservableList<Utenti> observableList = FXCollections.observableList(list);
        observableList.addListener(
                (ListChangeListener) change -> System.out.println("Detected a change! ")
        );

        Utenti c1 = new Utenti();
        Utenti c2 = new Utenti();

        c1.nomeProperty().addListener(
                (o, old, newValue)->System.out.println("Ciao")
        );

        c1.setNome("Pippo");

        // Changes to the observableList WILL be reported.
        // This line will print out "Detected a change!"
        observableList.add(c1);

        // Changes to the underlying list will NOT be reported
        // Nothing will be printed as a result of the next line.
        observableList.add(c2);


        observableList.get(0).setNome("Nuovo valore");

        System.out.println("Size: "+observableList.size());

    }


}