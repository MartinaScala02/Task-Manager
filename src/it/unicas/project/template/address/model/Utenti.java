package it.unicas.project.template.address.model;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

/**
 * Model class for a Utenti.
 */
public class Utenti {

    //invece che usare i tipi normali (String, int, ecc), uso i Property Wrapper di JavaFX -> permettono di "ascoltare" i cambiamenti delle proprietà (listeners) e possono essere legate a componenti GUI (bindings)
    private StringProperty nome;
    private StringProperty cognome;
    private StringProperty email;
    private StringProperty psw;
    private IntegerProperty idUtente;  //wrapper


    /**
     * Default constructor.
     */
    // Costruttore vuoto
    public Utenti() {
        this(null, null, null);
    }

    //costruttore completo -> inizializza tutte le proprietà
    public Utenti(String nome, String cognome, String email, String psw, Integer idColleghi) {
        this.nome = new SimpleStringProperty(nome);
        this.cognome = new SimpleStringProperty(cognome);
        this.email = new SimpleStringProperty(email);
        this.psw = new SimpleStringProperty(psw);
        this.idUtente = new SimpleIntegerProperty(idColleghi);

        //l'id non può mai essere nul
//        if (idColleghi != null){
//            this.idUtente = new SimpleIntegerProperty(idColleghi);
//        } else {
//            this.idUtente = null;
//        }
    }

    /**
     * Constructor with some initial data.
     *
     * @param nome
     * @param cognome
     * @param psw
     */

    //costruttore con dati iniziali -> per testing (creazione rapida di utenti)
    public Utenti(String nome, String cognome, String psw) {
        this.nome = new SimpleStringProperty(nome);
        this.cognome = new SimpleStringProperty(cognome);
        this.email = new SimpleStringProperty("email@email.com");
        this.psw = new SimpleStringProperty(psw);
        this.idUtente = null;
    }

    //getter e setter

    public Integer getIdUtente(){
        if (idUtente == null){
            idUtente = new SimpleIntegerProperty(-1); //se l'id è nullo, lo imposto a -1
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
        return psw.getValue();
    }

    public void setPsw(String psw) {
        this.psw.set(psw);
    }

    public StringProperty pswProperty() {
        return psw;
    }


    //serve a stampare l'utente in modo leggibile -> DOMANDA-serve veramente????
    @Override
    public String toString(){ //tostring è un metodo ereditato da Object, lo sovrascriviamo per stampare in modo leggibile
        return nome.getValue() + ", " + cognome.getValue() + ", " + email.getValue() + ", " + psw.getValue() + ", (" + idUtente.getValue() + ")";
    }


    public static void main(String[] args) {

        Utenti utente = new Utenti(); //creo un nuovo utente vuoto


        utente.setNome("Ciao"); //imposto il nome a "Ciao"
        MyChangeListener myChangeListener = new MyChangeListener();
        utente.nomeProperty().addListener(myChangeListener); //aggiungo un listener alla proprietà nome -> ogni volta che cambia, viene chiamato il metodo changed di MyChangeListener
        utente.setNome("Mario"); //cambio il nome -> viene stampato il messaggio del listener


        utente.pswProperty().addListener(myChangeListener); //aggiungo un listener alla proprietà psw

        //cambio la password -> viene stampato il messaggio del listener
        utente.pswProperty().addListener(
                (ChangeListener) (o, oldVal, newVal) -> System.out.println("La password property è cambiata!"));

        utente.pswProperty().addListener(
                (o, old, newVal)-> System.out.println("La password è cambiata da "+old+" a "+newVal)
        );


        utente.setPsw("12345"); //cambio la password




        List<Utenti> list = new ArrayList<>();

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
        observableList.add(c1);
        observableList.add(c2);


        observableList.get(0).setNome("Nuovo valore");

        System.out.println("Size: "+observableList.size());

    }


}
