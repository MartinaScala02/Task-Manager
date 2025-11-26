package it.unicas.project.template.address.view;

import it.unicas.project.template.address.model.Utenti;
import it.unicas.project.template.address.util.DateUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;

/**
 * Dialog to edit details of a colleghi.
 *
 * @author Mario Molinara
 */
public class ColleghiEditDialogController {

    @FXML
    private TextField nomeField;
    @FXML
    private TextField cognomeField;
    //@FXML
    //private TextField telefonoField;
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField PasswordField;
    @FXML
    private TextField pswVisibleField;
    @FXML
    private ToggleButton showpswBtn;

    private Stage dialogStage;
    private Utenti colleghi;
    private boolean okClicked = false;
    private boolean verifyLen = true;

    /**
     * Initializes the controller class. This method is automatically called
     * after the fxml file has been loaded.
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
     * Sets the stage of this dialog.
     *
     * @param dialogStage
     */
    public void setDialogStage(Stage dialogStage, boolean verifyLen) {
        this.dialogStage = dialogStage;
        this.verifyLen = verifyLen;

        // Set the dialog icon.
        //this.dialogStage.getIcons().add(new Image("file:resources/images/edit.png"));
    }

    /**
     * Sets the colleghi to be edited in the dialog.
     *
     * @param colleghi
     */
    public void setColleghi(Utenti colleghi) {
        this.colleghi = colleghi;

        nomeField.setText(colleghi.getNome());
        cognomeField.setText(colleghi.getCognome());
        //telefonoField.setText(colleghi.getTelefono());
        emailField.setText(colleghi.getEmail());
        PasswordField.setText(colleghi.getPsw());
        // compleannoField.setText(colleghi.getCompleanno());
        // compleannoField.setPromptText("dd-mm-yyyy");
    }

    /**
     * Returns true if the user clicked OK, false otherwise.
     *
     * @return
     */
    public boolean isOkClicked() {
        return okClicked;
    }

    /**
     * Called when the user clicks ok.
     */
    @FXML
    private void handleOk() {
        if (isInputValid(verifyLen)) {
            colleghi.setNome(nomeField.getText());
            colleghi.setCognome(cognomeField.getText());
            //colleghi.setTelefono(telefonoField.getText());
            colleghi.setEmail(emailField.getText());
            /*if (compleannoField.getText() != null){
                colleghi.setCompleanno(compleannoField.getText());
            }*/
            colleghi.setPsw(PasswordField.getText());
            okClicked = true;
            dialogStage.close();
        }
    }

    /**
     * Called when the user clicks cancel.
     */
    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    /**
     * Validates the user input in the text fields.
     *
     * @return true if the input is valid
     */
    private boolean isInputValid(boolean verifyLen) {
        String errorMessage = "";

        if (nomeField.getText() == null || (verifyLen && nomeField.getText().length() == 0)) {
            errorMessage += "Nome non valido!\n";
        }
        if (cognomeField.getText() == null || (verifyLen && cognomeField.getText().length() == 0)) {
            errorMessage += "Cognome non valido!\n";
        }
        /*if (telefonoField.getText() == null || (verifyLen && telefonoField.getText().length() == 0)) {
            errorMessage += "No valid telephone number!\n";
        }*/

        if (emailField.getText() == null || (verifyLen && emailField.getText().length() == 0)) {
            errorMessage += "Email non valida!\n";
        }
        /*if (compleannoField.getText() == null && verifyLen){
            errorMessage += "No valid birthday!\n";
        }

        if (compleannoField.getText() != null && verifyLen){
            if (compleannoField.getText().length() == 0){
                errorMessage += "No valid birthday!\n";
            }
            if (!DateUtil.validDate(compleannoField.getText())) {
                errorMessage += "No valid birthday. Use the format dd-mm-yyyy!\n";
            }
        }*/
        if (PasswordField.getText() == null || (verifyLen && PasswordField.getText().length() == 0)) {
            errorMessage += "psw non valida!\n";
        }

        if (errorMessage.length() == 0) {
            return true;
        } else {
            // Show the error message.
            Alert alert = new Alert(AlertType.ERROR);
            alert.initOwner(dialogStage);
            alert.setTitle("Campi non validi");
            alert.setHeaderText("Per favore, correggi i campi non validi.");
            alert.setContentText(errorMessage);

            alert.showAndWait();

            return false;
        }
    }

}
