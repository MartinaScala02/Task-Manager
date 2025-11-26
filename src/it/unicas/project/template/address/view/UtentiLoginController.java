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
    private PasswordField PasswordField;

    @FXML
    private TextField pswVisibleField;

    @FXML
    private ToggleButton showpswBtn;

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


        // Metodo per gestire il login: verifica email e psw confrontandoli con i dati in memoria (mainApp.getColleghiData())
    @FXML
    private void handleLogin() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String psw = PasswordField.getText() == null ? "" : PasswordField.getText();

        if (email.isEmpty() || psw.isEmpty()) {
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
                    && u.getPsw() != null && u.getPsw().equals(psw)) {
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
                PasswordField.clear();
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

