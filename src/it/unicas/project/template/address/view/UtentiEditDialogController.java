package it.unicas.project.template.address.view;

import it.unicas.project.template.address.model.Utenti;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;
import it.unicas.project.template.address.model.dao.mysql.DAOUtenti;
import it.unicas.project.template.address.model.dao.DAOException;

/**
 * Controller per la finestra di dialogo di modifica dei dati utente.
 * <p>
 * Questa classe gestisce l'interazione con l'utente per la modifica delle informazioni
 * di un oggetto {@link Utenti} (nome, cognome, email, password).
 * Include una logica per visualizzare o nascondere la password tramite un pulsante toggle
 * e gestisce la validazione dei campi prima del salvataggio nel database.
 */
public class UtentiEditDialogController {

    @FXML
    private TextField nomeField;
    @FXML
    private TextField cognomeField;
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField PasswordField; // Campo per password mascherata
    @FXML
    private TextField pswVisibleField; // Campo per password visibile
    @FXML
    private ToggleButton showpswBtn; // Pulsante per mostrare/nascondere la password

    private Stage dialogStage;
    private Utenti user;
    private boolean okClicked = false;
    private boolean verifyLen = true;

    /**
     * Inizializza la classe controller. Questo metodo viene chiamato automaticamente
     * dopo che il file fxml è stato caricato.
     * <p>
     * Configura il binding bidirezionale tra il campo password mascherato e quello visibile
     * e gestisce la logica del pulsante "Mostra Password" per alternare la visibilità.
     */
    @FXML
    private void initialize() {
        // Sincronizza i due campi (testo condiviso)
        pswVisibleField.textProperty().bindBidirectional(PasswordField.textProperty());

        // All'avvio mostra solo il PasswordField
        pswVisibleField.setVisible(false);
        pswVisibleField.setManaged(false);

        // Toggle: quando selezionato mostra il TextField, altrimenti mostra il PasswordField
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
     * Imposta lo stage di questa finestra di dialogo.
     *
     * @param dialogStage Lo stage della finestra.
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    /**
     * Restituisce true se l'utente ha cliccato OK, false altrimenti.
     *
     * @return true se l'operazione è confermata.
     */
    public boolean isOkClicked() {
        return okClicked;
    }

    /**
     * Chiamato quando l'utente clicca su OK.
     * <p>
     * Verifica la validità dell'input, aggiorna l'oggetto {@link Utenti} con i nuovi dati
     * e tenta di salvare le modifiche nel database tramite {@link DAOUtenti}.
     * Se il salvataggio ha successo, chiude la finestra.
     */
    @FXML
    private void handleOk() {
        if (isInputValid(verifyLen)) {
            // Copia i valori dai campi di testo all'oggetto user
            user.setNome(nomeField.getText());
            user.setCognome(cognomeField.getText());
            user.setEmail(emailField.getText());
            user.setPsw(PasswordField.getText());

            try {
                DAOUtenti.getInstance().update(user); // Salva nel database (aggiorna)
            } catch (DAOException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.initOwner(dialogStage);
                alert.setTitle("Errore");
                alert.setHeaderText("Non è stato possibile salvare le modifiche");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
                return; // Esce senza chiudere il dialog in caso di errore
            }

            okClicked = true;
            dialogStage.close();
        }
    }

    /**
     * Chiamato quando l'utente clicca su Annulla.
     * Chiude la finestra senza salvare le modifiche.
     */
    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    /**
     * Valida l'input dell'utente nei campi di testo.
     *
     * @param verifyLen Se true, verifica anche che la lunghezza dei campi sia > 0.
     * @return true se l'input è valido, false altrimenti.
     */
    private boolean isInputValid(boolean verifyLen) {
        String errorMessage = ""; // Stringa per accumulare i messaggi di errore

        if (nomeField.getText() == null || (verifyLen && nomeField.getText().length() == 0)) {
            errorMessage += "Nome non valido!\n";
        }
        if (cognomeField.getText() == null || (verifyLen && cognomeField.getText().length() == 0)) {
            errorMessage += "Cognome non valido!\n";
        }
        if (emailField.getText() == null || (verifyLen && emailField.getText().length() == 0)) {
            errorMessage += "Email non valida!\n";
        }
        if (PasswordField.getText() == null || (verifyLen && PasswordField.getText().length() == 0)) {
            errorMessage += "Password non valida!\n";
        }

        if (errorMessage.length() == 0) {
            return true; // Non ci sono errori
        } else {
            // Se ci sono errori, mostra un alert
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(dialogStage);
            alert.setTitle("Campi non validi");
            alert.setHeaderText("Per favore, correggi i campi non validi.");
            alert.setContentText(errorMessage);

            alert.showAndWait();

            return false;
        }
    }

    /**
     * Imposta l'utente da modificare nella finestra di dialogo.
     * Popola i campi di testo con i dati attuali dell'utente.
     *
     * @param user L'oggetto Utenti da modificare.
     */
    public void setUser(Utenti user) {
        this.user = user; // Memorizza l'utente sul quale operare

        // Popola i campi di testo con i dati dell'utente
        nomeField.setText(user.getNome());
        cognomeField.setText(user.getCognome());
        emailField.setText(user.getEmail());
        PasswordField.setText(user.getPsw());
    }
}