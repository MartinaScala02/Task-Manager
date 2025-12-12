package it.unicas.project.template.address;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException; // IMPORT AGGIUNTO
import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;

import it.unicas.project.template.address.model.Tasks;
import it.unicas.project.template.address.model.Utenti;
import it.unicas.project.template.address.model.dao.DAOException;
import it.unicas.project.template.address.model.dao.mysql.DAOUtenti;
import it.unicas.project.template.address.model.dao.mysql.DAOMySQLSettings;
import it.unicas.project.template.address.view.*;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class MainApp extends Application {

    private Stage primaryStage;
    private BorderPane rootLayout;
    private static Utenti currentUser = null;

    // Riferimento al controller principale per aggiornare la grafica
    private MainScreenController mainScreenController;

    public static Utenti getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(Utenti user) {
        currentUser = user;
    }

    public MainApp() {
    }

    private ObservableList<Utenti> colleghiData = FXCollections.observableArrayList();

    public ObservableList<Utenti> getColleghiData() {
        return colleghiData;
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("Task Manager Avanzato");
        this.primaryStage.getIcons().add(new Image("file:resources/images/App_Icon.png"));

        initRootLayout();
        initData();
        showUtentiLogin();

        primaryStage.show();
    }

    public void initData() {
        try {
            List<Utenti> list = DAOUtenti.getInstance().select(null);
            colleghiData.clear();
            colleghiData.addAll(list);
            System.out.println("InitData completato: " + colleghiData.size() + " utenti caricati dal database.");
        } catch (DAOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Errore Database");
            alert.setHeaderText("Impossibile caricare i dati");
            alert.setContentText("Dettagli errore: " + e.getMessage());
            alert.showAndWait();
            e.printStackTrace();
        }
    }

    public void initRootLayout() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("view/RootLayout.fxml"));
            rootLayout = (BorderPane) loader.load();

            Scene scene = new Scene(rootLayout);
            primaryStage.setScene(scene);

            primaryStage.setOnCloseRequest(windowEvent -> {
                windowEvent.consume();
                handleExit();
            });

            RootLayoutController controller = loader.getController();
            controller.setMainApp(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleExit() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Chiusura applicazione");
        alert.setHeaderText("EXIT");
        alert.setContentText("Sei sicuro di voler uscire?");

        ButtonType buttonTypeOne = new ButtonType("Conferma");
        ButtonType buttonTypeCancel = new ButtonType("Annulla", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(buttonTypeOne, buttonTypeCancel);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == buttonTypeOne){
            // --- FIX: Gestione dell'eccezione SQL ---
            try {
                DAOMySQLSettings.closeStatement(DAOMySQLSettings.getStatement());
            } catch (SQLException e) {
                e.printStackTrace();
            }
            System.exit(0);
        }
    }



    public void showUtentiLogin(){
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("view/UtentiLogin.fxml"));
            rootLayout.setCenter(loader.load());
            UtentiLoginController controller = loader.getController();
            controller.setMainApp(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean showSettingsEditDialog(DAOMySQLSettings daoMySQLSettings){
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("view/SettingsEditDialog.fxml"));

            Stage dialogStage = new Stage();
            dialogStage.setTitle("DAO settings");
            dialogStage.initModality((Modality.WINDOW_MODAL));
            dialogStage.initOwner(primaryStage);
            Scene scene = new Scene(loader.load());
            dialogStage.setScene(scene);

            SettingsEditDialogController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setSettings(daoMySQLSettings);
            dialogStage.getIcons().add(new Image("file:resources/images/edit.png"));
            dialogStage.showAndWait();

            return controller.isOkClicked();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }



    public boolean showUtentiEditDialog(Utenti user) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("view/UtentiEditDialog.fxml"));
            AnchorPane page = (AnchorPane) loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Edit Utenti");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(primaryStage);
            Scene scene = new Scene(page);
            dialogStage.setScene(scene);

            UtentiEditDialogController controller = loader.getController();
            controller.setDialogStage(dialogStage);

            // Imposta l'utente
            controller.setUser(user);

            dialogStage.getIcons().add(new Image("file:resources/images/edit.png"));
            dialogStage.showAndWait();

            boolean okClicked = controller.isOkClicked();

            // Aggiorna il nome nell'header se salvato
            if (okClicked && mainScreenController != null) {
                mainScreenController.refreshUserInfo();
            }

            return okClicked;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean showTasksEditDialog(Tasks task) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/it/unicas/project/template/address/view/TaskEditDialog.fxml"));
            AnchorPane page = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Edit Task");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(primaryStage);
            Scene scene = new Scene(page);
            dialogStage.setScene(scene);

            TaskEditDialogController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setTask(task);
            controller.setMainApp(this);

            dialogStage.getIcons().add(new Image("file:resources/images/edit.png"));
            dialogStage.showAndWait();

            return controller.isOkClicked();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }



    public void showMainScreen() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("view/MainScreen.fxml"));
            rootLayout.setCenter(loader.load());

            // Salviamo il controller per aggiornare l'header dopo le modifiche
            this.mainScreenController = loader.getController();
            this.mainScreenController.setMainApp(this);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean showUtentiProfile(Utenti user) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("view/UtentiProfile.fxml"));
            AnchorPane page = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Profilo Utente");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(primaryStage);
            Scene scene = new Scene(page);
            dialogStage.setScene(scene);

            UtentiProfileController controller = loader.getController();
            controller.setDialogStage(dialogStage);

            controller.setUser(user);
            controller.setMainApp(this);

            dialogStage.getIcons().add(new Image("file:resources/images/edit.png"));
            dialogStage.showAndWait();

            // Aggiorna header
            if (mainScreenController != null) {
                mainScreenController.refreshUserInfo();
            }

            return controller.isOkClicked();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public File getColleghiFilePath() {
        Preferences prefs = Preferences.userNodeForPackage(MainApp.class);
        String filePath = prefs.get("filePath", null);
        if (filePath != null) {
            return new File(filePath);
        } else {
            return null;
        }
    }

    public void setColleghiFilePath(File file) {
        Preferences prefs = Preferences.userNodeForPackage(MainApp.class);
        if (file != null) {
            prefs.put("filePath", file.getPath());
            primaryStage.setTitle("AddressApp - " + file.getName());
        } else {
            prefs.remove("filePath");
            primaryStage.setTitle("AddressApp");
        }
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        MainApp.launch(args);
    }

    // Metodo di compatibilità se serve
    public boolean showTaskEditDialog(Tasks task) {
        return showTasksEditDialog(task);
    }

    class MyEventHandler implements EventHandler<WindowEvent> {
        @Override
        public void handle(WindowEvent windowEvent) {
            windowEvent.consume();
        }
    }

    public void showTasksStatistics(int idUtente) {
        try {
            // 1. Carica il file FXML
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("view/TasksStatistics.fxml"));

            // Assicurati che il tipo di root corrisponda a quello del tuo FXML
            Scene scene = new Scene(loader.load());

            // 2. Configura il nuovo Stage
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Statistiche Task Utente");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            // Assicurati che primaryStage sia l'owner
            if (primaryStage != null) {
                dialogStage.initOwner(primaryStage);
            }

            // 3. Imposta la Scene e l'icona (opzionale)
            dialogStage.setScene(scene);
            // Puoi usare un'icona specifica per le statistiche se ne hai una
            dialogStage.getIcons().add(new Image("file:resources/images/statistics.png"));

            // 4. Ottieni il controller e CARICA I DATI
            TasksStatisticsController controller = loader.getController();
            // Chiama il metodo che popola i grafici usando l'ID utente
            controller.loadStatistics(idUtente);

            // 5. Mostra la finestra
            dialogStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            // Implementa una gestione degli errori più amichevole
            // showAlert("Errore", "Impossibile caricare la finestra delle statistiche.");
        }
    }
}