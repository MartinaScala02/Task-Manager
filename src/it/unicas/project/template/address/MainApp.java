package it.unicas.project.template.address;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
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

/**
 * Classe principale dell'applicazione Task Manager.
 * <p>
 * Questa classe funge da punto di ingresso (Entry Point) per l'applicazione JavaFX e agisce da
 * coordinatore centrale per la navigazione tra le diverse schermate (Login, Dashboard, Dialoghi).
 * Gestisce inoltre il ciclo di vita dell'applicazione, la sessione dell'utente corrente e
 * l'inizializzazione del layout principale.
 */
public class MainApp extends Application {

    private Stage primaryStage;
    private BorderPane rootLayout;

    /** Utente attualmente loggato nel sistema. */
    private static Utenti currentUser = null;

    // Riferimento al controller principale per aggiornare la grafica
    private MainScreenController mainScreenController;

    /**
     * Restituisce l'utente attualmente loggato.
     * @return L'oggetto {@link Utenti} della sessione corrente.
     */
    public static Utenti getCurrentUser() {
        return currentUser;
    }

    /**
     * Imposta l'utente loggato nella sessione corrente.
     * @param user L'oggetto {@link Utenti} da impostare come attivo.
     */
    public static void setCurrentUser(Utenti user) {
        currentUser = user;
    }

    public MainApp() {
    }

    private ObservableList<Utenti> utentiData = FXCollections.observableArrayList();

    /**
     * Restituisce la lista osservabile degli utenti caricati.
     * @return ObservableList di Utenti.
     */
    public ObservableList<Utenti> getUtentiData() {
        return utentiData;
    }

    /**
     * Metodo di avvio dell'applicazione JavaFX.
     * Inizializza lo stage primario, carica il layout di base, recupera i dati iniziali
     * e mostra la schermata di login.
     *
     * @param primaryStage Lo stage principale fornito dalla piattaforma JavaFX.
     */
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

    /**
     * Carica i dati iniziali necessari all'avvio dell'applicazione (es. lista utenti).
     * Gestisce eventuali eccezioni di connessione al database.
     */
    public void initData() {
        try {
            List<Utenti> list = DAOUtenti.getInstance().select(null);
            utentiData.clear();
            utentiData.addAll(list);
            System.out.println("InitData completato: " + utentiData.size() + " utenti caricati dal database.");
        } catch (DAOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Errore Database");
            alert.setHeaderText("Impossibile caricare i dati");
            alert.setContentText("Dettagli errore: " + e.getMessage());
            alert.showAndWait();
            e.printStackTrace();
        }
    }

    /**
     * Inizializza il layout radice (Root Layout) che contiene la barra dei menu e il contenuto principale.
     * Imposta anche il gestore per la chiusura dell'applicazione.
     */
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

    /**
     * Gestisce la procedura di uscita dall'applicazione.
     * Mostra un alert di conferma e, se confermato, chiude le connessioni al database e termina il programma.
     */
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
            // Chiusura sicura delle risorse DB
            try {
                DAOMySQLSettings.closeStatement(DAOMySQLSettings.getStatement());
            } catch (SQLException e) {
                e.printStackTrace();
            }
            System.exit(0);
        }
    }

    /**
     * Carica e mostra la schermata di Login al centro del Root Layout.
     */
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

    /**
     * Apre una finestra di dialogo modale per modificare o creare un utente.
     *
     * @param user L'oggetto Utente da modificare.
     * @return true se l'utente ha cliccato OK, false altrimenti.
     */
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

            // Aggiorna il nome nell'header se salvato e se siamo nella dashboard
            if (okClicked && mainScreenController != null) {
                mainScreenController.refreshUserInfo();
            }

            return okClicked;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Apre una finestra di dialogo modale per modificare o creare un Task.
     *
     * @param task L'oggetto Task da modificare.
     * @return true se l'utente ha cliccato OK, false altrimenti.
     */
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

    /**
     * Carica e mostra la Dashboard principale (MainScreen) al centro del layout.
     * Inizializza il controller principale e imposta il riferimento all'applicazione.
     */
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

    /**
     * Apre la finestra del profilo utente in modalità modale.
     *
     * @param user L'utente di cui visualizzare il profilo.
     * @return true se sono state apportate modifiche confermate.
     */
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


    /**
     * Restituisce lo stage principale dell'applicazione.
     * @return Lo stage primario.
     */
    public Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * Apre la finestra delle statistiche (grafici) per un determinato utente.
     *
     * @param idUtente L'ID dell'utente di cui caricare le statistiche.
     */
    public void showTasksStatistics(int idUtente) {
        try {

            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("view/TasksStatistics.fxml"));

            Scene scene = new Scene(loader.load());

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Statistiche Task Utente");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            if (primaryStage != null) {
                dialogStage.initOwner(primaryStage);
            }


            dialogStage.setScene(scene);
            dialogStage.getIcons().add(new Image("file:resources/images/statistics.png"));

            TasksStatisticsController controller = loader.getController();
            controller.loadStatistics(idUtente);


            dialogStage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Apre la finestra dei promemoria per le scadenze imminenti.
     *
     * @param idUtente L'ID dell'utente loggato.
     * @param onCloseAction Callback da eseguire alla chiusura della finestra (es. nascondere badge).
     * @param onTaskRequest Callback da eseguire se l'utente seleziona un task (es. aprire dettagli).
     */
    public void showPromemoria(int idUtente, Runnable onCloseAction, java.util.function.Consumer<Tasks> onTaskRequest) {
        try {

            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("view/Promemoria.fxml"));
            AnchorPane page = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Scadenze Imminenti");
           // dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(primaryStage);
            dialogStage.setResizable(false);
            Scene scene = new Scene(page);
            dialogStage.setScene(scene);


            PromemoriaController controller = loader.getController();
            controller.setDialogStage(dialogStage);


            controller.setOnCloseCallback(onCloseAction);
            controller.setOnTaskSelected(onTaskRequest);

            controller.loadUrgentTasks(idUtente);

            dialogStage.setOnHidden(e -> {
                if (onCloseAction != null) onCloseAction.run();
            });


            dialogStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Errore");
            alert.setHeaderText("Impossibile aprire i promemoria");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}