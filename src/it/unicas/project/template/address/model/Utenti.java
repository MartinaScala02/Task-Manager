package it.unicas.project.template.address.model;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

/**
 * Classe Model che rappresenta un Utente all'interno del sistema.
 * <p>
 * Questa classe utilizza i Property Wrapper di JavaFX (es. {@link StringProperty}, {@link IntegerProperty})
 * al posto dei tipi primitivi standard. Questo approccio permette di:
 * </p>
 * <ul>
 * <li>Implementare il pattern Observer tramite Listener (per reagire ai cambiamenti dei valori).</li>
 * <li>Effettuare il binding bidirezionale con i componenti della GUI (es. TableView, TextField).</li>
 * </ul>
 */
public class Utenti {

    //invece che usare i tipi normali (String, int, ecc), uso i Property Wrapper di JavaFX -> permettono di "ascoltare" i cambiamenti delle proprietà (listeners) e possono essere legate a componenti GUI (bindings)
    private StringProperty nome;
    private StringProperty cognome;
    private StringProperty email;
    private StringProperty psw;
    private IntegerProperty idUtente;  //wrapper


    /**
     * Costruttore di default.
     * Inizializza un oggetto Utenti con valori nulli.
     */
    // Costruttore vuoto
    public Utenti() {
        this(null, null, null);
    }

    /**
     * Costruttore completo per inizializzare tutte le proprietà dell'utente.
     * Utilizzato solitamente quando si recuperano i dati dal database.
     *
     * @param nome       Il nome dell'utente.
     * @param cognome    Il cognome dell'utente.
     * @param email      L'indirizzo email.
     * @param psw        La password dell'utente.
     * @param idColleghi L'ID univoco dell'utente (spesso riferito come idColleghi).
     */
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
     * Costruttore parziale con dati iniziali.
     * Utile per il testing rapido o per la creazione di utenti provvisori.
     * Imposta un'email di default ("email@email.com") e id nullo.
     *
     * @param nome    Il nome dell'utente.
     * @param cognome Il cognome dell'utente.
     * @param psw     La password dell'utente.
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

    /**
     * Restituisce l'ID dell'utente.
     * Se la property idUtente non è stata inizializzata, la crea con valore di default -1.
     *
     * @return L'ID dell'utente come Integer.
     */
    public Integer getIdUtente(){
        if (idUtente == null){
            idUtente = new SimpleIntegerProperty(-1); //se l'id è nullo, lo imposto a -1
        }
        return idUtente.get();
    }

    /**
     * Imposta l'ID dell'utente.
     *
     * @param idUtente Il nuovo ID da impostare.
     */
    public void setIdUtente(Integer idUtente) {
        if (this.idUtente == null){
            this.idUtente = new SimpleIntegerProperty();
        }
        this.idUtente.set(idUtente);
    }

    /**
     * Restituisce il nome dell'utente.
     * @return Il nome.
     */
    public String getNome() {
        return nome.get();
    }

    /**
     * Imposta il nome dell'utente.
     * @param nome Il nuovo nome.
     */
    public void setNome(String nome) {
        this.nome.set(nome);
    }

    /**
     * Restituisce la property del nome per il binding JavaFX.
     * @return L'oggetto StringProperty associato al nome.
     */
    public StringProperty nomeProperty() {
        return nome;
    }

    /**
     * Restituisce il cognome dell'utente.
     * @return Il cognome.
     */
    public String getCognome() {
        return cognome.get();
    }

    /**
     * Imposta il cognome dell'utente.
     * @param cognome Il nuovo cognome.
     */
    public void setCognome(String cognome) {
        this.cognome.set(cognome);
    }

    /**
     * Restituisce la property del cognome per il binding JavaFX.
     * @return L'oggetto StringProperty associato al cognome.
     */
    public StringProperty cognomeProperty() {
        return cognome;
    }

    /**
     * Restituisce l'email dell'utente.
     * @return L'email.
     */
    public String getEmail() {
        return email.get();
    }

    /**
     * Imposta l'email dell'utente.
     * @param email La nuova email.
     */
    public void setEmail(String email) {
        this.email.set(email);
    }

    /**
     * Restituisce la property dell'email per il binding JavaFX.
     * @return L'oggetto StringProperty associato all'email.
     */
    public StringProperty emailProperty() {
        return email;
    }

    /**
     * Restituisce la password dell'utente.
     * @return La password.
     */
    public String getPsw() {
        return psw.getValue();
    }

    /**
     * Imposta la password dell'utente.
     * @param psw La nuova password.
     */
    public void setPsw(String psw) {
        this.psw.set(psw);
    }

    /**
     * Restituisce la property della password per il binding JavaFX.
     * @return L'oggetto StringProperty associato alla password.
     */
    public StringProperty pswProperty() {
        return psw;
    }

    /**
     * Restituisce una rappresentazione in stringa dell'oggetto Utente.
     * Utile per il debugging e il logging.
     *
     * @return Una stringa contenente i valori delle proprietà dell'utente.
     */
    @Override
    public String toString(){ //tostring è un metodo ereditato da Object, lo sovrascriviamo per stampare in modo leggibile
        return nome.getValue() + ", " + cognome.getValue() + ", " + email.getValue() + ", " + psw.getValue() + ", (" + idUtente.getValue() + ")";
    }

}