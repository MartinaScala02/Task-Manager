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


public class UtentiEditDialogController {

    @FXML
    private TextField nomeField;
    @FXML
    private TextField cognomeField;
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField PasswordField;
    @FXML
    private TextField pswVisibleField;
    @FXML
    private ToggleButton showpswBtn;

    private Stage dialogStage;
    private Utenti user;
    private boolean okClicked = false;
    private boolean verifyLen = true;

    @FXML
    private void initialize(){
        //sincronizza i due campi (testo condiviso)
        pswVisibleField.textProperty().bindBidirectional(PasswordField.textProperty());

        //all'avvio mostra solo il PasswordField
        pswVisibleField.setVisible(false);
        pswVisibleField.setManaged(false);

        //toggle: quando selezionato mostra il TextField, altrimenti mostra il PasswordField
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
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public boolean isOkClicked() { //chiamato quando l'utente ha premuto ok
        return okClicked;
    }

    @FXML
    private void handleOk() {
        if (isInputValid(verifyLen)) {
            //copia i valori dai campi di testo all'oggetto user
            user.setNome(nomeField.getText());
            user.setCognome(cognomeField.getText());
            user.setEmail(emailField.getText());
            user.setPsw(PasswordField.getText());

            try {
                DAOUtenti.getInstance().update(user); //salva nel database (aggiorna)
            } catch (DAOException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.initOwner(dialogStage);
                alert.setTitle("Errore");
                alert.setHeaderText("Non è stato possibile salvare le modifiche");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
                return; //esce senza chiudere il dialog
            }

            okClicked = true;
            dialogStage.close();
        }
    }


    @FXML
    private void handleCancel() { //chiude il dialog senza salvare
        dialogStage.close();
    }


    private boolean isInputValid(boolean verifyLen) { //ritorna true se i campi sono validi
        String errorMessage = ""; //stringa per accumulare i messaggi di errore

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
            errorMessage += "psw non valida!\n";
        }


        if (errorMessage.length() == 0) { //se errorMessage è vuota, tutti i campi sono validi
            return true; //non ci sono errori
        } else { //se ci sono errori, mostra un alert

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(dialogStage);
            alert.setTitle("Campi non validi");
            alert.setHeaderText("Per favore, correggi i campi non validi.");
            alert.setContentText(errorMessage);

            alert.showAndWait();

            return false;
        }
    }


    public void setUser(Utenti user) {
        this.user = user; //memorizza l'utente sul quale operare

        //popola i campi di testo con i dati dell'utente
        nomeField.setText(user.getNome());
        cognomeField.setText(user.getCognome());
        emailField.setText(user.getEmail());
        PasswordField.setText(user.getPsw());

    }

}
