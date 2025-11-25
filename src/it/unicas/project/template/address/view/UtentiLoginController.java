package it.unicas.project.template.address.view;

import it.unicas.project.template.address.model.Utenti;
import it.unicas.project.template.address.model.dao.mysql.ColleghiDAOMySQLImpl;
import it.unicas.project.template.address.model.dao.DAOException;
import it.unicas.project.template.address.MainApp;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField; //gestisce automaticamente la maschera
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;

public class UtentiLoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField passwordVisibleField;

    @FXML
    private ToggleButton showPasswordBtn;

    private MainApp mainApp;

    // Aggiunto: costruttore senza argomenti richiesto da FXMLLoader
    public UtentiLoginController() {
        // nulla qui; l'inizializzazione dipende da @FXML e da setMainApp()
    }

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }


    @FXML
    private void initialize() {
        // Sincronizza i due campi (testo condiviso)
        passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());

        // All'avvio mostra solo il PasswordField
        passwordVisibleField.setVisible(false);
        passwordVisibleField.setManaged(false);

        // Toggle: quando selezionato mostra il TextField, altrimenti mostra il PasswordField
        showPasswordBtn.selectedProperty().addListener((obs, oldV, newV) -> {
            if (newV) {
                passwordVisibleField.setVisible(true);
                passwordVisibleField.setManaged(true);
                passwordField.setVisible(false);
                passwordField.setManaged(false);
                passwordVisibleField.requestFocus();
                passwordVisibleField.positionCaret(passwordVisibleField.getText().length());
            } else {
                passwordField.setVisible(true);
                passwordField.setManaged(true);
                passwordVisibleField.setVisible(false);
                passwordVisibleField.setManaged(false);
                passwordField.requestFocus();
                passwordField.positionCaret(passwordField.getText().length());
            }
        });
    }


        // Metodo per gestire il login: verifica email e password confrontandoli con i dati in memoria (mainApp.getColleghiData())
    @FXML
    private void handleLogin() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.initOwner(mainApp.getPrimaryStage());
            alert.setTitle("Input mancante");
            alert.setHeaderText("Email o password mancanti");
            alert.setContentText("Inserisci email e password.");
            alert.showAndWait();
            return;
        }

        boolean authenticated = false;
        for (Utenti u : mainApp.getColleghiData()) {
            if (u.getEmail() != null && u.getEmail().equalsIgnoreCase(email)
                    && u.getPassword() != null && u.getPassword().equals(password)) {
                authenticated = true;

                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.initOwner(mainApp.getPrimaryStage());
                success.setTitle("Login effettuato");
                success.setHeaderText(null);
                String displayName = (u.getNome() != null && !u.getNome().isEmpty()) ? u.getNome() : u.getEmail();
                success.setContentText("Benvenuto, " + displayName + "!");
                success.showAndWait();

                // Azioni aggiuntive possono essere inserite qui (es. notificare MainApp dell'utente loggato)

                // Pulisco i campi
                emailField.clear();
                passwordField.clear();
                break;
            }
        }

        if (!authenticated) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(mainApp.getPrimaryStage());
            alert.setTitle("Login fallito");
            alert.setHeaderText("Email o password errati");
            alert.setContentText("Verifica le credenziali e riprova.");
            alert.showAndWait();
        }
    }


    @FXML
    private void handleRegister(){
            Utenti tempColleghi = new Utenti();
            boolean okClicked = mainApp.showColleghiEditDialog(tempColleghi, true);

            if (okClicked) {
                try {
                    ColleghiDAOMySQLImpl.getInstance().insert(tempColleghi);
                    mainApp.getColleghiData().add(tempColleghi);
                    //colleghiTableView.getItems().add(tempColleghi);
                } catch (DAOException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.initOwner(mainApp.getPrimaryStage());
                    alert.setTitle("Error during DB interaction");
                    alert.setHeaderText("Error during insert ...");
                    alert.setContentText(e.getMessage());
                    alert.showAndWait();
                }
            }
        }



}

