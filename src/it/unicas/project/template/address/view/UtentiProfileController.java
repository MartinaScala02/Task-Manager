package it.unicas.project.template.address.view;

import it.unicas.project.template.address.model.dao.DAOException;
import it.unicas.project.template.address.model.dao.mysql.DAOUtenti;
import javafx.fxml.FXML;
import javafx.scene.Scene;

import javafx.stage.Stage;
import it.unicas.project.template.address.model.Utenti;
import it.unicas.project.template.address.MainApp;
import javafx.scene.layout.AnchorPane;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Label;

import java.io.IOException;


public class UtentiProfileController {

    @FXML
    private Label nomeLabel;
    @FXML
    private Label cognomeLabel;
    @FXML
    private Label emailLabel;
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
    private Utenti user;
    private MainApp mainApp;
    private boolean okClicked = false;

    public UtentiProfileController() {
    }



    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }


    private void showUserDetails(Utenti user) {
        if (user != null) {

            nomeLabel.setText(user.getNome());
            cognomeLabel.setText(user.getCognome());
            emailLabel.setText(user.getEmail());

        } else {
            // Utenti is null, remove all the text.
            nomeLabel.setText("");
            cognomeLabel.setText("");
            emailLabel.setText("");
        }
    }

    @FXML
    private void handleEditUtenti() {
        if (user == null) return;

        boolean okClicked = mainApp.showUtentiEditDialog(user);
        if (okClicked) {
            showUserDetails(user); // aggiorna la UI con i nuovi dati
        }
    }



    @FXML
    private void handleDelete() {
        if (user == null) return;

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.initOwner(dialogStage);
        confirmation.setTitle("Conferma eliminazione");
        confirmation.setHeaderText("Sei sicuro di voler eliminare questo profilo?");
        confirmation.setContentText("Questa operazione non può essere annullata.");

        if (confirmation.showAndWait().filter(response -> response == ButtonType.OK).isPresent()) {
            try {
                DAOUtenti.getInstance().delete(user);
                mainApp.setCurrentUser(null); // Rimuovi l'utente dalla sessione
                // Chiudi il dialog
                dialogStage.close();
                // Torna alla schermata di login
                System.exit(0);
//                mainApp.showUtentiLogin();



            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.initOwner(dialogStage);
                alert.setTitle("Errore eliminazione");
                alert.setHeaderText("Errore durante l'eliminazione dell'utente");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            }
        }
    }


    public void setUser(Utenti user) {
        this.user = user;

        nomeLabel.setText(user.getNome());
        cognomeLabel.setText(user.getCognome());
        emailLabel.setText(user.getEmail());
    }


    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public boolean isOkClicked() {
        return okClicked;
    }
}


