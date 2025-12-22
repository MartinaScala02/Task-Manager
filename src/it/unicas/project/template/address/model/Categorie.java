package it.unicas.project.template.address.model;

import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

/**
 * Classe Model che rappresenta una Categoria nel sistema.
 * <p>
 * Utilizza i Property Wrapper di JavaFX (es. {@link StringProperty}, {@link IntegerProperty})
 * per consentire il binding automatico con l'interfaccia grafica e la gestione degli eventi.
 * </p>
 */
public class Categorie {
    private StringProperty nomeCategoria;
    private IntegerProperty idCategoria;
    private IntegerProperty idUtente;

    /**
     * Costruttore di default.
     * Inizializza un oggetto Categorie vuoto.
     */
    public Categorie() {
        this(null, null);
    }

    /**
     * Costruttore completo.
     *
     * @param nomeCategoria Il nome della categoria.
     * @param idCategoria   L'ID univoco della categoria (se null, viene gestito come non assegnato).
     */
    public Categorie(String nomeCategoria, Integer idCategoria) {
        this.nomeCategoria = new SimpleStringProperty(nomeCategoria);
        if (idCategoria != null) {
            this.idCategoria = new SimpleIntegerProperty(idCategoria);
        } else {
            this.idCategoria = null;
        }
    }

    /**
     * Constructor with some initial data.
     *
     * @param nomeCategoria Il nome della categoria.
     */

    public Categorie(String nomeCategoria) {
        this.nomeCategoria = new SimpleStringProperty(nomeCategoria);
        this.idCategoria = null;

    }

    /**
     * Restituisce l'ID della categoria.
     * Se l'ID non è stato inizializzato, viene restituito -1.
     *
     * @return L'ID categoria come Integer.
     */
    public Integer getIdCategoria(){
        if (idCategoria == null){
            idCategoria = new SimpleIntegerProperty(-1);
        }
        return idCategoria.get();
    }

    /**
     * Imposta l'ID della categoria.
     *
     * @param idCategoria Il nuovo ID.
     */
    public void setIdCategoria(Integer idCategoria) {
        if (this.idCategoria == null){
            this.idCategoria = new SimpleIntegerProperty();
        }
        this.idCategoria.set(idCategoria);
    }

    /**
     * Restituisce il nome della categoria.
     * @return Il nome.
     */
    public String getNomeCategoria() {
        return nomeCategoria.get();
    }

    /**
     * Imposta il nome della categoria.
     * @param nomeCategoria Il nuovo nome.
     */
    public void setNomeCategoria(String nomeCategoria) {
        this.nomeCategoria.set(nomeCategoria);
    }

    /**
     * Restituisce la property del nome categoria per il binding JavaFX.
     * @return L'oggetto StringProperty.
     */
    public StringProperty nomeCategoriaProperty() {
        return nomeCategoria;
    }

    /**
     * Restituisce una rappresentazione stringa dell'oggetto.
     *
     * @return Stringa contenente nome e ID categoria.
     */
    public String toString(){
        return nomeCategoria.getValue() + ", (" + idCategoria.getValue() + ")";
    }


    public void setIdUtente(Integer idUtente) {
        if (this.idUtente == null) {
            this.idUtente = new SimpleIntegerProperty();
        }
        this.idUtente.set(idUtente);
    }

    public String getIdUtente() {
        if (idUtente == null) return null;
        return String.valueOf(idUtente.get());
    }
}