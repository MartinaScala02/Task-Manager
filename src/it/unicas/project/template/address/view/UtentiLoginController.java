package it.unicas.project.template.address.view;

import it.unicas.project.template.address.model.Utenti;
import it.unicas.project.template.address.model.dao.mysql.DAOUtenti;
import it.unicas.project.template.address.model.dao.DAOException;
import it.unicas.project.template.address.MainApp;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;

import java.util.List;

/**
 * Controller per la gestione della schermata di Login.
 * <p>
 * Questa classe gestisce l'interazione con l'utente per l'autenticazione al sistema.
 * Include funzionalità per l'inserimento delle credenziali, la gestione della visibilità
 * della password e la navigazione verso la registrazione di un nuovo utente.
 * </p>
 */
public class UtentiLoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField PasswordField;

    @FXML
    private TextField pswVisibleField;

    @FXML
    private ToggleButton showpswBtn;

    private MainApp mainApp;

    /**
     * Imposta il riferimento all'applicazione principale.
     * Necessario per consentire al controller di interagire con il flusso principale dell'applicazione.
     *
     * @param mainApp L'istanza dell'applicazione principale.
     */
    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    /**
     * Metodo di inizializzazione chiamato automaticamente dopo il caricamento del file FXML.
     * <p>
     * Configura il binding tra il campo password mascherato e quello visibile e imposta
     * il listener sul pulsante toggle per alternare la visualizzazione della password.
     * </p>
     */
    @FXML
    private void initialize() {
        pswVisibleField.textProperty().bindBidirectional(PasswordField.textProperty());

        pswVisibleField.setVisible(false);
        pswVisibleField.setManaged(false);

        showpswBtn.selectedProperty().addListener((obs, oldV, newV) -> {
            if (newV) {
                pswVisibleField.setVisible(true);
                pswVisibleField.setManaged(true);
                PasswordField.setVisible(false);
                PasswordField.setManaged(false);
                pswVisibleField.requestFocus();
                pswVisibleField.positionCaret(pswVisibleField.getText().length());
            } else {
                PasswordField.setVisible(true);
                PasswordField.setManaged(true);
                pswVisibleField.setVisible(false);
                pswVisibleField.setManaged(false);
                PasswordField.requestFocus();
                PasswordField.positionCaret(PasswordField.getText().length());
            }
        });
    }

    /**
     * Gestisce l'azione di login quando viene premuto il pulsante dedicato.
     * <p>
     * Recupera le credenziali inserite, valida l'input e interroga il database tramite {@link DAOUtenti}.
     * Se le credenziali sono corrette, imposta l'utente corrente nell'applicazione e mostra la schermata principale.
     * In caso di errore o credenziali non valide, mostra un messaggio di allerta.
     * </p>
     */
    @FXML
    private void handleLogin() {

        String emailInserita = emailField.getText();
        String passwordInserita = PasswordField.getText();

        if (emailInserita == null || emailInserita.isEmpty() || passwordInserita == null || passwordInserita.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Inserisci email e password.");
            alert.showAndWait();
            return;
        }

        try {

            Utenti userDaCercare = new Utenti();
            userDaCercare.setEmail(emailInserita);
            userDaCercare.setPsw(passwordInserita);

            List<Utenti> risultato = DAOUtenti.getInstance().select(userDaCercare);

            if (!risultato.isEmpty()) {

                Utenti utenteLoggato = risultato.get(0);
                MainApp.setCurrentUser(utenteLoggato);

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Successo");
                alert.setHeaderText(null);
                alert.setContentText("Benvenuto " + utenteLoggato.getNome() + "!");
                alert.showAndWait();

                mainApp.showMainScreen();

            } else {
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

    /**
     * Gestisce l'azione di registrazione di un nuovo utente.
     * <p>
     * Apre la finestra di dialogo per l'inserimento dei dati del nuovo utente.
     * Se l'operazione viene confermata, inserisce il nuovo utente nel database.
     * </p>
     */
    @FXML
    private void handleRegister(){
        Utenti tempUtenti = new Utenti();
        boolean okClicked = mainApp.showUtentiEditDialog(tempUtenti);

        if (okClicked) {
            try {
                DAOUtenti.getInstance().insert(tempUtenti);
                mainApp.getUtentiData().add(tempUtenti);

            } catch (DAOException e) {
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