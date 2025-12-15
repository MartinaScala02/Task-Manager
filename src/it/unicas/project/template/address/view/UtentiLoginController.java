package it.unicas.project.template.address.view;

import it.unicas.project.template.address.model.Utenti;
import it.unicas.project.template.address.model.dao.mysql.DAOUtenti;
import it.unicas.project.template.address.model.dao.DAOException;
import it.unicas.project.template.address.MainApp;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField; //gestisce automaticamente la maschera
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;

import java.util.List;

public class UtentiLoginController {

    @FXML
    private TextField emailField;

    //uso due campi per la password: uno mascherato (PasswordField) e uno visibile (TextField)
    @FXML
    private PasswordField PasswordField; //campo password mascherato

    @FXML
    private TextField pswVisibleField; //campo password visibile

    @FXML
    private ToggleButton showpswBtn; //pulsante per mostrare/nascondere la password

    private MainApp mainApp;


    public UtentiLoginController() {
    }

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp; // Riferimento all'applicazione principale
    }


    //metodo che viene chiamato automaticamente dopo il caricamento del file FXML
    @FXML
    private void initialize() {
        // Sincronizza i due campi (significa che i due campi condividono lo stesso testo)
        pswVisibleField.textProperty().bindBidirectional(PasswordField.textProperty());

        //all'avvio mostra solo il PasswordField
        pswVisibleField.setVisible(false); //non visibile
        pswVisibleField.setManaged(false); //non occupa spazio nel layout

//per gestire la visibilità della password -> selezionato mostra il campo di testo normale, altrimenti mostra il PasswordField
        showpswBtn.selectedProperty().addListener((obs, oldV, newV) -> {
            if (newV) {
                //se viene cliccato il togglebutton la password diventa visibile
                pswVisibleField.setVisible(true);
                pswVisibleField.setManaged(true);
                PasswordField.setVisible(false);
                PasswordField.setManaged(false);
                pswVisibleField.requestFocus(); //mette il cursore lampeggiante nel campo di testo visibile (la tastiera scrive lì)
                pswVisibleField.positionCaret(pswVisibleField.getText().length()); //posiziona il cursore alla fine del testo
            } else {
                //
                PasswordField.setVisible(true);
                PasswordField.setManaged(true);
                pswVisibleField.setVisible(false);
                pswVisibleField.setManaged(false);
                PasswordField.requestFocus();
                PasswordField.positionCaret(PasswordField.getText().length());
            }
        });
    }

    //per gestire il login (metodo collegato al pulsante di login nell'FXML)
    @FXML
    private void handleLogin() {

        //si prendono le credenziali inserite dall'utente da tastiera
        String emailInserita = emailField.getText();
        String passwordInserita = PasswordField.getText();

        //dà errore se uno dei due campi è vuoto
        if (emailInserita == null || emailInserita.isEmpty() || passwordInserita == null || passwordInserita.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR); //viene mostrato un popup di errore
            alert.setContentText("Inserisci email e password.");
            alert.showAndWait();
            return;
        }

        //per evitare che l'app crashi se ci sono problemi con il database
        try {

            Utenti userDaCercare = new Utenti(); //crea un oggetto utente temporaneo per cercare nel database (si crea un contenitore dei dati di ricerca)
            //per passare i dati di ricerca al DAO
            userDaCercare.setEmail(emailInserita);
            userDaCercare.setPsw(passwordInserita);



            List<Utenti> risultato = DAOUtenti.getInstance().select(userDaCercare); //si cerca l'utente nel database e si ottiene la lista dei risultati

            if (!risultato.isEmpty()) { //se la lista non è vuota, il login ha avuto successo

                Utenti utenteLoggato = risultato.get(0); //prende il primo utente dalla lista (dovrebbe esserci solo un utente con quelle credenziali)
                MainApp.setCurrentUser(utenteLoggato); //imposta l'utente loggato nell'app principale ->l'app sa chi è l'utente corrente


                //messaggio di benvenuto
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Successo");
                alert.setHeaderText(null);
                alert.setContentText("Benvenuto " + utenteLoggato.getNome() + "!");
                alert.showAndWait();

                mainApp.showMainScreen();

            } else {
                //se la lista è vuota, il login è fallito
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Errore");
                alert.setHeaderText("Login Fallito");
                alert.setContentText("Email o Password errate.");
                alert.showAndWait();
            }

        } catch (DAOException e) {
            e.printStackTrace();
        }
    }


    @FXML
    private void handleRegister(){
            Utenti tempUtenti = new Utenti(); //crea un oggetto utente temporaneo per passarlo al dialog di registrazione
            boolean okClicked = mainApp.showUtentiEditDialog(tempUtenti); //mostra il dialog di registrazione e aspetta che l'utente prema OK o Annulla

            if (okClicked) {
                try {
                    DAOUtenti.getInstance().insert(tempUtenti); //inserisce il nuovo utente nel database
                    mainApp.getColleghiData().add(tempUtenti); //aggiorna la lista degli utenti nell'app principale

                } catch (DAOException e) { //se c'è un errore durante l'inserimento nel database, mostra un messaggio di errore
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.initOwner(mainApp.getPrimaryStage());
                    alert.setTitle("Errore");
                    alert.setHeaderText("Errore durante la registrazione");
                    alert.setContentText(e.getMessage());
                    alert.showAndWait();
                }
            }
        }

}

