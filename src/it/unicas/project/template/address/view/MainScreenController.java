package it.unicas.project.template.address.view;

import it.unicas.project.template.address.MainApp;
import it.unicas.project.template.address.model.*;
import it.unicas.project.template.address.model.dao.DAOException;
import it.unicas.project.template.address.model.dao.mysql.DAOAllegati;
import it.unicas.project.template.address.model.dao.mysql.DAOTasks;
import it.unicas.project.template.address.model.dao.mysql.DAOCategorie;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;

/**
 * Controller principale dell'applicazione (MainScreen).
 * <p>
 * Questa classe funge da coordinatore centrale per la dashboard utente. Gestisce:
 * <ul>
 * <li>La visualizzazione dei task in diverse modalità (Lista, Griglia, Calendario) tramite {@link TasksList}.</li>
 * <li>Il pannello laterale destro per i dettagli, sottotask, timer e allegati tramite {@link TasksInfoPane}.</li>
 * <li>La logica di filtraggio tramite {@link FiltersPane}.</li>
 * <li>Il form per la creazione rapida di nuovi task.</li>
 * <li>La navigazione e le notifiche di sistema.</li>
 * </ul>
 */
public class MainScreenController {


    // --- Header e Menu Laterale ---
    @FXML private TextField txtSearch;
    @FXML private VBox sideMenu;
    @FXML private Label usernameLabelHeader;
    @FXML private ListView<Tasks> taskListView;
    @FXML private ScrollPane gridViewContainer;
    @FXML private FlowPane gridFlowPane;
    @FXML private javafx.scene.shape.Circle notificationBadge;

    // --- Viste Calendario ---
    @FXML private BorderPane calendarViewContainer;
    @FXML private GridPane calendarGrid;
    @FXML private ScrollPane weekViewContainer;
    @FXML private HBox weekViewBox;
    @FXML private Label calendarMonthLabel;
    @FXML private Label viewLabel;

    // --- Filtri ---
    @FXML private VBox categoryMenuContainer;
    @FXML private ComboBox<String> filterPriorityCombo;
    @FXML private DatePicker filterDatePicker;
    @FXML private ToggleButton btnFilterTodo;
    @FXML private ToggleButton btnFilterDone;

    // --- Dettagli Task (Pannello Destro) ---
    @FXML private VBox rightDetailPanel;
    @FXML private Label detailTitleLabel, detailCategoryLabel;
    @FXML private DatePicker detailDueDatePicker;
    @FXML private TextArea detailDescArea;
    @FXML private ListView<SubTasks> subTaskListView;
    @FXML private TextField newSubTaskField;
    @FXML private ListView<Allegati> attachmentListView;

    // --- Timer e Sessioni ---
    @FXML private Label timerLabel;
    @FXML private Label timerStatusLabel;
    @FXML private Button btnTimerToggle;
    @FXML private Button btnTimerReset;
    @FXML private Button btnTimerMenu;
    @FXML private VBox timerHistoryContainer;
    @FXML private ListView<TimerSessions> timerHistoryList;
    @FXML private Label timerTotalLabel;

    // --- Creazione Task (Form in basso) ---
    @FXML private TextField newTaskField;
    @FXML private TextArea descriptionArea;
    @FXML private ComboBox<Categorie> categoryComboBox;
    @FXML private ComboBox<String> priorityComboBox;
    @FXML private DatePicker dueDateField;
    @FXML private Button btnAddAttachment;

    /** Riferimento all'applicazione principale per navigazione e dati globali. */
    private MainApp mainApp;

    /** Flag per tracciare lo stato di apertura del menu laterale sinistro. */
    private boolean isSideMenuOpen = false;

    /** Helper per gestire la logica di visualizzazione della lista task (Lista, Griglia, Calendario). */
    private TasksList tasksListHelper;

    /** Helper per gestire il pannello destro (Dettagli, Sottotask, Timer). */
    private TasksInfoPane tasksInfoPane;

    /** Helper per gestire la logica dei filtri. */
    private FiltersPane filtersPane;

    /** File temporaneo selezionato durante la creazione di un nuovo task, prima del salvataggio. */
    private File pendingFile = null;



    /**
     * Collega l'applicazione principale a questo controller.
     * Viene chiamato da MainApp dopo il caricamento dell'FXML.
     * Inizializza i dati dell'utente corrente e avvia il controllo delle notifiche.
     *
     * @param mainApp L'istanza principale dell'applicazione.
     */
    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
        refreshUserInfo();
        if (tasksListHelper != null && MainApp.getCurrentUser() != null) {
            tasksListHelper.loadTasks(MainApp.getCurrentUser().getIdUtente());

            new Thread(() -> {
                try { Thread.sleep(500); } catch (InterruptedException e) {}
                javafx.application.Platform.runLater(this::checkNotifications);
            }).start();
        }
    }

    /**
     * Aggiorna l'interfaccia con le informazioni dell'utente loggato (es. nome nell'header).
     */
    public void refreshUserInfo() {
        Utenti u = MainApp.getCurrentUser();
        if (u != null) usernameLabelHeader.setText(u.getNome());
    }

    /**
     * Metodo di inizializzazione chiamato automaticamente dopo il caricamento del file FXML.
     * Configura i componenti UI, istanzia le classi Helper (TasksList, TasksInfoPane, FiltersPane)
     * e imposta i listener.
     */
    @FXML
    private void initialize() {

        if (filterDatePicker != null) filterDatePicker.setShowWeekNumbers(false);
        if (detailDueDatePicker != null) detailDueDatePicker.setShowWeekNumbers(false);
        if (dueDateField != null) dueDateField.setShowWeekNumbers(false);


        tasksListHelper = new TasksList(
                taskListView, gridViewContainer, gridFlowPane,
                calendarViewContainer, calendarGrid, weekViewContainer, weekViewBox, calendarMonthLabel,
                mainApp, this::handleEditTask, this::handleDeleteTask, this::handleOpenDetail
        );


        if (txtSearch != null) {
            txtSearch.textProperty().addListener((observable, oldValue, newValue) ->
                    tasksListHelper.setFilterKeyword(newValue));
        }


        tasksInfoPane = new TasksInfoPane(
                rightDetailPanel, detailTitleLabel, detailCategoryLabel,
                detailDueDatePicker, detailDescArea, subTaskListView,
                newSubTaskField, taskListView,
                attachmentListView,
                timerLabel, timerStatusLabel, btnTimerToggle, btnTimerReset,
                btnTimerMenu, timerHistoryContainer, timerHistoryList, timerTotalLabel
        );


        filtersPane = new FiltersPane(filterPriorityCombo, filterDatePicker,
                btnFilterTodo, btnFilterDone, categoryMenuContainer,
                categoryComboBox, tasksListHelper);

        setupCreationForm();


        taskListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Tasks sel = taskListView.getSelectionModel().getSelectedItem();
                if (sel != null) handleOpenDetail(sel);
            }
        });


        if (tasksListHelper != null) {
            tasksListHelper.getTasks().addListener((javafx.collections.ListChangeListener<Tasks>) c -> {
                checkNotifications();
            });
        }
    }


    /**
     * Gestisce il click sul pulsante "Graffetta" nel form di creazione.
     * Apre un FileChooser per selezionare un file locale.
     */
    @FXML
    public void handleAddAttachment() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleziona file da allegare");
        Stage stage = (Stage) btnAddAttachment.getScene().getWindow();
        File selected = fileChooser.showOpenDialog(stage);

        if (selected != null) {
            this.pendingFile = selected;
            // Feedback visivo sul bottone
            btnAddAttachment.setStyle("-fx-background-color: #50fa7b; -fx-text-fill: #282a36; -fx-font-weight: bold;");
            btnAddAttachment.setTooltip(new Tooltip("File pronto: " + selected.getName()));
        }
    }

    /**
     * Salva fisicamente il file pendente nella cartella "attachments" e inserisce il record nel DB.
     * Viene chiamato dopo che il Task è stato creato con successo.
     *
     * @param taskId L'ID del task appena creato a cui associare l'allegato.
     */
    private void savePendingFileToDB(Integer taskId) {
        if (pendingFile == null) return;
        try {
            String projectPath = System.getProperty("user.dir");
            File destDir = new File(projectPath, "attachments");

            if (!destDir.exists()) {
                boolean created = destDir.mkdir();
                if (!created) System.out.println("Impossibile creare cartella attachments");
            }

            String newFileName = System.currentTimeMillis() + "_" + pendingFile.getName();
            File destFile = new File(destDir, newFileName);

            System.out.println("Sto salvando il file in: " + destFile.getAbsolutePath());

            Files.copy(pendingFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            Allegati allegato = new Allegati();
            allegato.setIdTask(taskId);
            allegato.setNomeFile(pendingFile.getName());
            allegato.setPercorsoFile(destFile.getAbsolutePath());

            String ext = "";
            int i = newFileName.lastIndexOf('.');
            if (i > 0) ext = newFileName.substring(i + 1);
            allegato.setTipoFile(ext);

            DAOAllegati.getInstance().insert(allegato);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Errore nel salvataggio dell'allegato: " + e.getMessage());
        }
    }

    /**
     * Gestisce la creazione di un nuovo task dal form in basso.
     * Raccoglie i dati, valida l'input, inserisce nel DB e aggiorna la UI.
     */
    @FXML
    private void handleNewTask() {
        String titolo = newTaskField.getText().trim();
        String priorita = priorityComboBox.getValue();

        if (titolo.isEmpty()) {
            showAlert("Titolo obbligatorio");
            return;
        }

        if (priorita == null || priorita.isEmpty()) {
            showAlert("Attenzione: Devi selezionare una Priorità!");
            return;
        }

        try {
            Integer idCat = null;

            String nomeCatInput = categoryComboBox.getEditor().getText().trim();

            if (!nomeCatInput.isEmpty()) {

                Categorie catTrovata = categoryComboBox.getItems().stream()
                        .filter(c -> c.getNomeCategoria().equalsIgnoreCase(nomeCatInput))
                        .findFirst()
                        .orElse(null);

                if (catTrovata != null && catTrovata.getIdCategoria() != null && catTrovata.getIdCategoria() > 0) {
                    idCat = catTrovata.getIdCategoria();
                } else {

                    Categorie nuovaCat = new Categorie();
                    nuovaCat.setNomeCategoria(nomeCatInput);

                    if (MainApp.getCurrentUser() != null) {
                        nuovaCat.setIdUtente(MainApp.getCurrentUser().getIdUtente());
                    }


                    DAOCategorie.getInstance().insert(nuovaCat);
                    idCat = nuovaCat.getIdCategoria();


                    if (filtersPane != null) {
                        filtersPane.refreshCategories();
                    }
                }
            }


            Tasks t = new Tasks();
            t.setTitolo(titolo);
            t.setDescrizione(descriptionArea.getText());
            t.setPriorita(priorita);
            t.setScadenza(dueDateField.getValue() != null ? dueDateField.getValue().toString() : null);
            t.setIdUtente(MainApp.getCurrentUser().getIdUtente());
            t.setIdCategoria(idCat);
            t.setCompletamento(false);

            DAOTasks.getInstance().insert(t);

            if (pendingFile != null && t.getIdTask() != null) {
                savePendingFileToDB(t.getIdTask());
            }

            tasksListHelper.addTask(t);
            resetCreationForm();

        } catch (DAOException e) {
            showAlert("Errore: " + e.getMessage());
        }
    }

    /**
     * Resetta i campi del form di creazione task dopo un inserimento.
     */
    private void resetCreationForm() {
        newTaskField.clear();
        descriptionArea.clear();
        dueDateField.setValue(null);


        categoryComboBox.getEditor().clear();
        categoryComboBox.getSelectionModel().clearSelection();

        priorityComboBox.getSelectionModel().clearSelection();

        pendingFile = null;
        if (btnAddAttachment != null) {
            btnAddAttachment.setStyle("-fx-background-color: #3F2E51; -fx-text-fill: white;");
            btnAddAttachment.setTooltip(null);
        }
    }


    /**
     * Apre il pannello laterale destro mostrando i dettagli del task selezionato.
     *
     * @param t Il task da visualizzare.
     */
    private void handleOpenDetail(Tasks t) {
        String catName = tasksListHelper.getCategoryName(t.getIdCategoria(), categoryComboBox.getItems());
        tasksInfoPane.openPanel(t, catName);
    }

    @FXML private void closeRightPanel() { tasksInfoPane.closePanel(); }

    // Metodi per cambiare la modalità di visualizzazione (Lista, Griglia, Calendario)
    @FXML private void showListView() { tasksListHelper.switchView(TasksList.ViewMode.LIST); if (viewLabel != null) viewLabel.setText("Vista: Lista"); }
    @FXML private void showGridView() { tasksListHelper.switchView(TasksList.ViewMode.GRID); if (viewLabel != null) viewLabel.setText("Vista: Board"); }
    @FXML private void showCalendarView() { tasksListHelper.switchView(TasksList.ViewMode.CALENDAR); if (viewLabel != null) viewLabel.setText("Vista: Calendario"); }

    // Metodi di navigazione calendario
    @FXML private void prevMonth() { tasksListHelper.calendarBack(); }
    @FXML private void nextMonth() { tasksListHelper.calendarForward(); }
    @FXML private void handleCalViewMonth() { tasksListHelper.setCalendarMode(TasksList.CalendarMode.MONTH); }
    @FXML private void handleCalViewWeek()  { tasksListHelper.setCalendarMode(TasksList.CalendarMode.WEEK); }
    @FXML private void handleCalViewDay()   { tasksListHelper.setCalendarMode(TasksList.CalendarMode.DAY); }


    /**
     * Resetta tutti i filtri attivi e mostra tutti i task.
     */
    @FXML
    private void handleShowAll() {
        filtersPane.resetAllFilters();
        if (txtSearch != null) txtSearch.clear();
        if (btnFilterTodo != null) btnFilterTodo.setSelected(false);
        if (btnFilterDone != null) btnFilterDone.setSelected(false);
    }

    @FXML
    private void handleFilterToDo() {
        if (btnFilterTodo.isSelected()) { btnFilterDone.setSelected(false); filtersPane.setFilterStatus(false); }
        else { filtersPane.setFilterStatus(null); }
    }

    @FXML
    private void handleFilterCompleted() {
        if (btnFilterDone.isSelected()) { btnFilterTodo.setSelected(false); filtersPane.setFilterStatus(true); }
        else { filtersPane.setFilterStatus(null); }
    }

    /**
     * Gestisce la modifica di un task esistente.
     * Apre il dialog di modifica, attende la chiusura e aggiorna DB e UI se necessario.
     *
     * @param t Il task da modificare.
     */
    private void handleEditTask(Tasks t) {
        if (mainApp == null) return;

        if (t.getIdTask() == null || t.getIdTask() <= 0) {
            showAlert("Il task non ha un ID valido.");
            return;
        }

        String scadenzaPrecedente = t.getScadenza();

        boolean okClicked = mainApp.showTasksEditDialog(t);

        if (okClicked) {

            if (t.getScadenza() == null && scadenzaPrecedente != null) {
                t.setScadenza(scadenzaPrecedente);
            }

            new Thread(() -> {
                try {

                    DAOTasks.getInstance().update(t);

                    Platform.runLater(() -> {

                        tasksListHelper.updateTaskInList(t);

                        int index = taskListView.getItems().indexOf(t);
                        if (index >= 0) {
                            taskListView.getItems().set(index, null);
                            taskListView.getItems().set(index, t);
                            taskListView.getSelectionModel().select(t);
                        }


                        if (tasksInfoPane.isOpen() && tasksInfoPane.getCurrentTask() != null &&
                                tasksInfoPane.getCurrentTask().getIdTask().equals(t.getIdTask())) {

                            String catName = tasksListHelper.getCategoryName(t.getIdCategoria(), categoryComboBox.getItems());
                            tasksInfoPane.openPanel(t, catName);
                        }
                    });
                } catch (DAOException e) {
                    Platform.runLater(() -> showAlert("Errore Database: " + e.getMessage()));
                }
            }).start();
        }
    }

    /**
     * Gestisce l'eliminazione di un task.
     * Chiede conferma all'utente, poi cancella dal DB e aggiorna la UI.
     *
     * @param t Il task da eliminare.
     */
    private void handleDeleteTask(Tasks t) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Sei sicuro di voler eliminare questo task?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait();

        if (alert.getResult() == ButtonType.YES) {
            new Thread(() -> {
                try {
                    DAOTasks.getInstance().delete(t);
                    Platform.runLater(() -> {
                        tasksListHelper.removeTask(t);
                        if (tasksInfoPane.getCurrentTask() == t) {
                            tasksInfoPane.closePanel();
                        }
                    });
                } catch (DAOException e) {
                    Platform.runLater(() -> showAlert("Impossibile eliminare il task: " + e.getMessage()));
                }
            }).start();
        }
    }

    @FXML private void handleNewSubTask() { tasksInfoPane.createSubTask(); }
    @FXML private void handleToggleTimerMenu() { if (tasksInfoPane != null) tasksInfoPane.toggleHistoryMenu(); }
    @FXML private void handleTimerToggle() { if (tasksInfoPane != null) tasksInfoPane.toggleTimer(); }
    @FXML private void handleTimerReset() { if (tasksInfoPane != null) tasksInfoPane.resetTimer(); }


    /**
     * Configura i componenti del form di creazione (ComboBox, DatePicker).
     */
    private void setupCreationForm() {
        if (priorityComboBox != null) priorityComboBox.getItems().setAll("BASSA", "MEDIA", "ALTA");

        categoryComboBox.setEditable(true);

        categoryComboBox.setConverter(new StringConverter<Categorie>() {
            @Override
            public String toString(Categorie c) {
                return (c == null) ? "" : c.getNomeCategoria();
            }

            @Override
            public Categorie fromString(String string) {
                if (string == null || string.trim().isEmpty()) return null;

                // Cerca se esiste già, altrimenti crea un oggetto temporaneo
                return categoryComboBox.getItems().stream()
                        .filter(c -> c.getNomeCategoria().equalsIgnoreCase(string.trim()))
                        .findFirst()
                        .orElseGet(() -> {
                            Categorie temp = new Categorie();
                            temp.setNomeCategoria(string.trim());
                            return temp;
                        });
            }
        });
        if (dueDateField != null) {
            dueDateField.setShowWeekNumbers(false);
            dueDateField.setEditable(false);
            dueDateField.setDayCellFactory(picker -> new DateCell() {
                @Override
                public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    if (date != null && !empty && date.isBefore(LocalDate.now())) {
                        setDisable(true);
                        setStyle("-fx-background-color: #ffc0cb;");
                    }
                }
            });
            dueDateField.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && newVal.isBefore(LocalDate.now())) dueDateField.setValue(null);
            });
        }
    }

    @FXML
    private void toggleMenu() {
        TranslateTransition tt = new TranslateTransition(Duration.millis(350), sideMenu);
        tt.setToX(isSideMenuOpen ? -300 : 0);
        tt.play();
        isSideMenuOpen = !isSideMenuOpen;
    }

    @FXML private void handleLogout() { mainApp.showUtentiLogin(); }
    @FXML private void handleExit() { mainApp.handleExit(); }
    @FXML private void handleProfile() { mainApp.showUtentiProfile(MainApp.getCurrentUser()); }
    @FXML private void handleStatistics() { mainApp.showTasksStatistics(MainApp.getCurrentUser().getIdUtente()); }

    private void showAlert(String msg) { new Alert(Alert.AlertType.WARNING, msg).show(); }


    /**
     * Apre la finestra dei promemoria con le scadenze imminenti.
     * Definisce le azioni da eseguire alla chiusura (nascondere badge) e alla selezione (aprire dettagli).
     */
    @FXML
    private void handleShowPromemoria() {
        if (MainApp.getCurrentUser() != null) {
            mainApp.showPromemoria(
                    MainApp.getCurrentUser().getIdUtente(),

                    () -> {
                        if (notificationBadge != null) {
                            notificationBadge.setVisible(false);
                        }
                    },

                    (selectedTask) -> {
                        taskListView.getSelectionModel().select(selectedTask);
                        taskListView.scrollTo(selectedTask);
                        handleOpenDetail(selectedTask);
                    }
            );
        } else {
            showAlert("Devi essere loggato per vedere i promemoria.");
        }
    }

    /**
     * Controlla se ci sono task urgenti (scaduti, oggi, domani) e aggiorna la visibilità
     * del pallino delle notifiche.
     */
    private void checkNotifications() {
        if (tasksListHelper == null || tasksListHelper.getTasks() == null) return;

        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);


        boolean hasUrgentTasks = tasksListHelper.getTasks().stream().anyMatch(t -> {
            if (t.getCompletamento()) return false;
            if (t.getScadenza() == null || t.getScadenza().isEmpty()) return false;
            LocalDate due = smartParse(t.getScadenza());
            if (due == null) return false;

            return !due.isAfter(tomorrow);
        });


        if (notificationBadge != null) {
            notificationBadge.setVisible(hasUrgentTasks);
        }
    }

    private LocalDate smartParse(String dateString) {
        if (dateString == null || dateString.isEmpty()) return null;
        try {

            return LocalDate.parse(dateString);
        } catch (Exception e) {
            try {

                return it.unicas.project.template.address.util.DateUtil.parse(dateString);
            } catch (Exception ex) {
                return null;
            }
        }
    }
}