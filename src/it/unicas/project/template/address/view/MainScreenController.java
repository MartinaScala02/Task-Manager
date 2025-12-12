package it.unicas.project.template.address.view;

import it.unicas.project.template.address.MainApp;
import it.unicas.project.template.address.model.*;
import it.unicas.project.template.address.model.dao.DAOException;
import it.unicas.project.template.address.model.dao.mysql.DAOAllegati;
import it.unicas.project.template.address.model.dao.mysql.DAOTasks;
import javafx.animation.TranslateTransition;
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

public class MainScreenController {

    // --- CAMPI FXML HEADER E MENU ---
    @FXML private TextField txtSearch;
    @FXML private VBox sideMenu;
    @FXML private Label usernameLabelHeader;
    @FXML private ListView<Tasks> taskListView;
    @FXML private ScrollPane gridViewContainer;
    @FXML private FlowPane gridFlowPane;

    // --- VISTE CALENDARIO ---
    @FXML private BorderPane calendarViewContainer;
    @FXML private GridPane calendarGrid;
    @FXML private ScrollPane weekViewContainer;
    @FXML private HBox weekViewBox;
    @FXML private Label calendarMonthLabel;
    @FXML private Label viewLabel;

    // --- FILTRI ---
    @FXML private VBox categoryMenuContainer;
    @FXML private ComboBox<String> filterPriorityCombo;
    @FXML private DatePicker filterDatePicker;
    @FXML private ToggleButton btnFilterTodo;
    @FXML private ToggleButton btnFilterDone;

    // --- DETTAGLI TASK (Pannello Destro) ---
    @FXML private VBox rightDetailPanel;
    @FXML private Label detailTitleLabel, detailCategoryLabel;
    @FXML private DatePicker detailDueDatePicker;
    @FXML private TextArea detailDescArea;
    @FXML private ListView<SubTasks> subTaskListView;
    @FXML private TextField newSubTaskField;
    @FXML private ListView<Allegati> attachmentListView; // Iniettata qui, ma passata all'InfoPane

    // --- TIMER ---
    @FXML private Label timerLabel;
    @FXML private Label timerStatusLabel;
    @FXML private Button btnTimerToggle;
    @FXML private Button btnTimerReset;
    @FXML private Button btnTimerMenu;
    @FXML private VBox timerHistoryContainer;
    @FXML private ListView<TimerSessions> timerHistoryList;
    @FXML private Label timerTotalLabel;

    // --- CREAZIONE TASK (Form in basso) ---
    @FXML private TextField newTaskField;
    @FXML private TextArea descriptionArea;
    @FXML private ComboBox<Categorie> categoryComboBox;
    @FXML private ComboBox<String> priorityComboBox;
    @FXML private DatePicker dueDateField;
    @FXML private Button btnAddAttachment; // Pulsante Graffetta

    // --- VARIABILI DI STATO ---
    private MainApp mainApp;
    private boolean isSideMenuOpen = false;
    private TasksList tasksListHelper;
    private TasksInfoPane tasksInfoPane;
    private FiltersPane filtersPane;

    // Variabile per memorizzare il file scelto durante la creazione di un task
    private File pendingFile = null;

    // --- INIZIALIZZAZIONE ---

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
        refreshUserInfo();
        if (tasksListHelper != null && MainApp.getCurrentUser() != null) {
            tasksListHelper.loadTasks(MainApp.getCurrentUser().getIdUtente());
        }
    }

    public void refreshUserInfo() {
        Utenti u = MainApp.getCurrentUser();
        if (u != null) usernameLabelHeader.setText(u.getNome());
    }

    @FXML
    private void initialize() {
        // Configurazione DatePicker
        if (filterDatePicker != null) filterDatePicker.setShowWeekNumbers(false);
        if (detailDueDatePicker != null) detailDueDatePicker.setShowWeekNumbers(false);
        if (dueDateField != null) dueDateField.setShowWeekNumbers(false);

        // Inizializzazione Helper Liste e Viste
        tasksListHelper = new TasksList(
                taskListView, gridViewContainer, gridFlowPane,
                calendarViewContainer, calendarGrid, weekViewContainer, weekViewBox, calendarMonthLabel,
                mainApp, this::handleEditTask, this::handleDeleteTask, this::handleOpenDetail
        );

        if (txtSearch != null) {
            txtSearch.textProperty().addListener((observable, oldValue, newValue) -> tasksListHelper.setFilterKeyword(newValue));
        }

        // Inizializzazione Pannello Dettagli e Timer
        // NOTA: Passiamo attachmentListView qui!
        tasksInfoPane = new TasksInfoPane(
                rightDetailPanel, detailTitleLabel, detailCategoryLabel,
                detailDueDatePicker, detailDescArea, subTaskListView,
                newSubTaskField, taskListView,
                attachmentListView, // <--- LISTA ALLEGATI PASSATA ALL'HELPER
                timerLabel, timerStatusLabel, btnTimerToggle, btnTimerReset,
                btnTimerMenu, timerHistoryContainer, timerHistoryList, timerTotalLabel
        );

        // Inizializzazione Filtri
        filtersPane = new FiltersPane(filterPriorityCombo, filterDatePicker,
                btnFilterTodo, btnFilterDone, categoryMenuContainer,
                categoryComboBox, tasksListHelper);

        setupCreationForm();

        // Configurazione Lista Allegati: RIMOSSA DA QUI (Spostata in TasksInfoPane)
        // Gestione Click Lista Task
        taskListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Tasks sel = taskListView.getSelectionModel().getSelectedItem();
                if (sel != null) handleOpenDetail(sel);
            }
        });
    }

    // --- LOGICA GESTIONE ALLEGATI (SOLO CREAZIONE) ---
    // La logica di visualizzazione/apertura è stata spostata in TasksInfoPane

    @FXML
    public void handleAddAttachment() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleziona file da allegare");
        Stage stage = (Stage) btnAddAttachment.getScene().getWindow();
        File selected = fileChooser.showOpenDialog(stage);

        if (selected != null) {
            this.pendingFile = selected;
            btnAddAttachment.setStyle("-fx-background-color: #50fa7b; -fx-text-fill: #282a36; -fx-font-weight: bold;");
            btnAddAttachment.setTooltip(new Tooltip("File pronto: " + selected.getName()));
        }
    }

    private void savePendingFileToDB(Integer taskId) {
        if (pendingFile == null) return;
        try {
            String projectPath = System.getProperty("user.dir");
            File destDir = new File(projectPath, "attachments");

            if (!destDir.exists()) {
                boolean created = destDir.mkdir();
                if(!created) System.out.println("Impossibile creare cartella attachments");
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
            if (i > 0) ext = newFileName.substring(i+1);
            allegato.setTipoFile(ext);

            DAOAllegati.getInstance().insert(allegato);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Errore nel salvataggio dell'allegato: " + e.getMessage());
        }
    }

    // --- CREAZIONE NUOVO TASK ---

    @FXML
    private void handleNewTask() {
        String titolo = newTaskField.getText().trim();
        if (titolo.isEmpty()) { showAlert("Titolo obbligatorio"); return; }

        if (dueDateField.getValue() != null && dueDateField.getValue().isBefore(LocalDate.now())) {
            showAlert("Errore: Non puoi creare un task nel passato!");
            return;
        }

        try {
            Integer idCat = null;
            if (categoryComboBox.getValue() != null) idCat = categoryComboBox.getValue().getIdCategoria();

            Tasks t = new Tasks();
            t.setTitolo(titolo);
            t.setDescrizione(descriptionArea.getText());
            t.setPriorita(priorityComboBox.getValue());
            t.setScadenza(dueDateField.getValue() != null ? dueDateField.getValue().toString() : null);

            if (MainApp.getCurrentUser() != null) {
                t.setIdUtente(MainApp.getCurrentUser().getIdUtente());
            } else {
                showAlert("Nessun utente loggato!");
                return;
            }

            t.setIdCategoria(idCat);
            t.setCompletamento(false);

            DAOTasks.getInstance().insert(t);

            if (pendingFile != null && t.getIdTask() != null) {
                savePendingFileToDB(t.getIdTask());
            }

            tasksListHelper.addTask(t);
            resetCreationForm();

        } catch (DAOException e) {
            showAlert("Errore inserimento: " + e.getMessage());
        }
    }

    private void resetCreationForm() {
        newTaskField.clear();
        descriptionArea.clear();
        dueDateField.setValue(null);
        categoryComboBox.getSelectionModel().clearSelection();
        priorityComboBox.getSelectionModel().clearSelection();

        pendingFile = null;
        btnAddAttachment.setStyle("-fx-background-color: #3F2E51; -fx-text-fill: white; -fx-font-size: 18px; -fx-cursor: hand; -fx-background-radius: 4; -fx-border-color: #555; -fx-border-radius: 4;");
        btnAddAttachment.setTooltip(null);
    }

    // --- GESTIONE DETTAGLI E VISTE ---

    private void handleOpenDetail(Tasks t) {
        String catName = tasksListHelper.getCategoryName(t.getIdCategoria(), categoryComboBox.getItems());
        tasksInfoPane.openPanel(t, catName);
        // loadAttachments è stato rimosso: lo fa l'InfoPane
    }

    @FXML private void closeRightPanel() { tasksInfoPane.closePanel(); }

    @FXML private void showListView() { tasksListHelper.switchView(TasksList.ViewMode.LIST); if (viewLabel != null) viewLabel.setText("Vista: Lista"); }
    @FXML private void showGridView() { tasksListHelper.switchView(TasksList.ViewMode.GRID); if (viewLabel != null) viewLabel.setText("Vista: Board"); }
    @FXML private void showCalendarView() { tasksListHelper.switchView(TasksList.ViewMode.CALENDAR); if (viewLabel != null) viewLabel.setText("Vista: Calendario"); }

    @FXML private void prevMonth() { tasksListHelper.calendarBack(); }
    @FXML private void nextMonth() { tasksListHelper.calendarForward(); }
    @FXML private void handleCalViewMonth() { tasksListHelper.setCalendarMode(TasksList.CalendarMode.MONTH); }
    @FXML private void handleCalViewWeek()  { tasksListHelper.setCalendarMode(TasksList.CalendarMode.WEEK); }
    @FXML private void handleCalViewDay()   { tasksListHelper.setCalendarMode(TasksList.CalendarMode.DAY); }

    // --- FILTRI ---

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

    // --- EDIT / DELETE / SUBTASKS / TIMER ---

    private void handleEditTask(Tasks t) {
        if (mainApp.showTasksEditDialog(t)) {
            try {
                DAOTasks.getInstance().update(t);
                tasksListHelper.updateTaskInList(t);
                if (tasksInfoPane.isOpen() && tasksInfoPane.getCurrentTask().equals(t)) {
                    String catName = tasksListHelper.getCategoryName(t.getIdCategoria(), categoryComboBox.getItems());
                    tasksInfoPane.openPanel(t, catName); // Ricarica tutto (inclusi allegati)
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void handleDeleteTask(Tasks t) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Eliminare task?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait();
        if (alert.getResult() == ButtonType.YES) {
            try {
                DAOTasks.getInstance().delete(t);
                tasksListHelper.removeTask(t);
                if (tasksInfoPane.getCurrentTask() == t) tasksInfoPane.closePanel();
            } catch (Exception e) { showAlert("Errore eliminazione: " + e.getMessage()); }
        }
    }

    @FXML private void handleNewSubTask() { tasksInfoPane.createSubTask(); }
    @FXML private void handleToggleTimerMenu() { if (tasksInfoPane != null) tasksInfoPane.toggleHistoryMenu(); }
    @FXML private void handleTimerToggle() { if (tasksInfoPane != null) tasksInfoPane.toggleTimer(); }
    @FXML private void handleTimerReset() { if (tasksInfoPane != null) tasksInfoPane.resetTimer(); }

    // --- SETUP UI / UTILITY ---

    private void setupCreationForm() {
        if (priorityComboBox != null) priorityComboBox.getItems().setAll("BASSA", "MEDIA", "ALTA");
        categoryComboBox.setConverter(new StringConverter<>() {
            @Override public String toString(Categorie c) { return c==null?"":c.getNomeCategoria(); }
            @Override public Categorie fromString(String s) { return null; }
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
}