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
public class Utenti {

    private StringProperty nome;
    private StringProperty cognome;
    //private StringProperty telefono;
    private StringProperty email;
    //private StringProperty compleanno;
    private StringProperty password;
    private IntegerProperty idUtenti;  //wrapper

    //private static String attributoStaticoDiEsempio;

    /**
     * Default constructor.
     */
    public Utenti() {
        this(null, null);
    }

    public Utenti(String nome, String cognome,/* String telefono,*/ String email, /*String compleanno,*/ Integer idColleghi) {
        this.nome = new SimpleStringProperty(nome);
        this.cognome = new SimpleStringProperty(cognome);
       // this.telefono = new SimpleStringProperty(telefono);
        this.email = new SimpleStringProperty(email);
        //this.compleanno = new SimpleStringProperty(compleanno);
        if (idColleghi != null){
            this.idUtenti = new SimpleIntegerProperty(idColleghi);
        } else {
            this.idUtenti = null;
        }
        this.password = new SimpleStringProperty("");
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
        this.password = new SimpleStringProperty("password");
        //this.compleanno = new SimpleStringProperty("24-10-2017");
        this.idUtenti = null;
    }

    public Integer getIdUtenti(){
        if (idUtenti == null){
            idUtenti = new SimpleIntegerProperty(-1);
        }
        return idUtenti.get();
    }

    public void setIdUtenti(Integer idUtenti) {
        if (this.idUtenti == null){
            this.idUtenti = new SimpleIntegerProperty();
        }
        this.idUtenti.set(idUtenti);
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

    public String getPassword() {
        return password.get();
    }

    public void setPassword(String password) {
        this.password.set(password);
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
        return nome.getValue() + ", " + cognome.getValue() + ", " /*+ telefono.getValue() + ", " */ + email.getValue() + ", " + /*compleanno.getValue() + */", (" + idUtenti.getValue() + ")";
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

