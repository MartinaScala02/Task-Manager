package it.unicas.project.template.address.view;

import it.unicas.project.template.address.MainApp;
import it.unicas.project.template.address.model.Categorie;
import it.unicas.project.template.address.model.SubTasks;
import it.unicas.project.template.address.model.Tasks;
import it.unicas.project.template.address.model.Utenti;
import it.unicas.project.template.address.model.dao.DAOException;
import it.unicas.project.template.address.model.dao.mysql.DAOTasks;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;
import java.time.LocalDate;
import javafx.scene.control.DateCell;

public class MainScreenController {

    // --- FXML Components: TOP BAR ---
    @FXML private VBox sideMenu;
    @FXML private Label usernameLabelHeader;
    @FXML private ToggleButton tglList, tglBoard, tglCalendar;

    // --- AREA CENTRALE ---
    @FXML private ListView<Tasks> taskListView;
    @FXML private ScrollPane gridViewContainer;
    @FXML private FlowPane gridFlowPane;

    @FXML private BorderPane calendarViewContainer;
    @FXML private GridPane calendarGrid;
    @FXML private ScrollPane weekViewContainer;
    @FXML private HBox weekViewBox;
    @FXML private Label calendarMonthLabel;
    @FXML private Label viewLabel;

    // --- SIDEBAR FILTRI ---
    @FXML private VBox categoryMenuContainer;
    @FXML private ComboBox<String> filterPriorityCombo;
    @FXML private DatePicker filterDatePicker;
    @FXML private Button btnFilterTodo, btnFilterDone;

    // --- PANNELLO DETTAGLI ---
    @FXML private VBox rightDetailPanel;
    @FXML private Label detailTitleLabel, detailCategoryLabel;
    @FXML private DatePicker detailDueDatePicker;
    @FXML private TextArea detailDescArea;
    @FXML private ListView<SubTasks> subTaskListView;
    @FXML private TextField newSubTaskField;

    // --- TIMER ---
    @FXML private Label timerLabel;
    @FXML private Label timerStatusLabel;
    @FXML private Button btnTimerToggle;
    @FXML private Button btnTimerReset;

    // --- FORM CREAZIONE (IL PUNTO CRITICO) ---
    @FXML private TextField newTaskField;
    @FXML private TextArea descriptionArea;
    @FXML private ComboBox<Categorie> categoryComboBox;
    @FXML private ComboBox<String> priorityComboBox;
    @FXML private DatePicker dueDateField; // CAMPO DATA CON PROTEZIONE

    // --- LOGICA ---
    private MainApp mainApp;
    private boolean isSideMenuOpen = false;

    private TasksList tasksListHelper;
    private TasksInfoPane tasksInfoPane;
    private FiltersPane filtersPane;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
        Utenti u = MainApp.getCurrentUser();
        if (u != null) usernameLabelHeader.setText(u.getNome());
        tasksListHelper.loadTasks(u.getIdUtente());
    }

    @FXML
    private void initialize() {
        tasksListHelper = new TasksList(
                taskListView, gridViewContainer, gridFlowPane,
                calendarViewContainer, calendarGrid, weekViewContainer, weekViewBox, calendarMonthLabel,
                mainApp,
                this::handleEditTask,
                this::handleDeleteTask,
                this::handleOpenDetail
        );

        tasksInfoPane = new TasksInfoPane(
                rightDetailPanel, detailTitleLabel, detailCategoryLabel,
                detailDueDatePicker, detailDescArea, subTaskListView,
                newSubTaskField, taskListView,
                timerLabel, timerStatusLabel, btnTimerToggle, btnTimerReset
        );

        filtersPane = new FiltersPane(filterPriorityCombo, filterDatePicker,
                btnFilterTodo, btnFilterDone, categoryMenuContainer,
                categoryComboBox, tasksListHelper);

        // Configura il form e PROTEGGE LA DATA
        setupCreationForm();

        taskListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Tasks sel = taskListView.getSelectionModel().getSelectedItem();
                if (sel != null) handleOpenDetail(sel);
            }
        });
    }

    // =========================================================
    //  GESTIONE VISTE
    // =========================================================
    @FXML private void showListView() { tasksListHelper.switchView(TasksList.ViewMode.LIST); if (viewLabel != null) viewLabel.setText("Vista: Lista"); }
    @FXML private void showGridView() { tasksListHelper.switchView(TasksList.ViewMode.GRID); if (viewLabel != null) viewLabel.setText("Vista: Board"); }
    @FXML private void showCalendarView() { tasksListHelper.switchView(TasksList.ViewMode.CALENDAR); if (viewLabel != null) viewLabel.setText("Vista: Calendario"); }

    @FXML private void prevMonth() { tasksListHelper.calendarBack(); }
    @FXML private void nextMonth() { tasksListHelper.calendarForward(); }
    @FXML private void handleCalViewMonth() { tasksListHelper.setCalendarMode(TasksList.CalendarMode.MONTH); }
    @FXML private void handleCalViewWeek()  { tasksListHelper.setCalendarMode(TasksList.CalendarMode.WEEK); }
    @FXML private void handleCalViewDay()   { tasksListHelper.setCalendarMode(TasksList.CalendarMode.DAY); }

    // =========================================================
    //  TIMER & DETTAGLI
    // =========================================================
    @FXML private void handleTimerReset() { if (tasksInfoPane != null) tasksInfoPane.resetTimer(); }
    @FXML private void closeRightPanel() { tasksInfoPane.closePanel(); }
    @FXML private void handleNewSubTask() { tasksInfoPane.createSubTask(); }

    private void handleOpenDetail(Tasks t) {
        String catName = tasksListHelper.getCategoryName(t.getIdCategoria(), categoryComboBox.getItems());
        tasksInfoPane.openPanel(t, catName);
    }

    // =========================================================
    //  FILTRI
    // =========================================================
    @FXML private void handleShowAll() { filtersPane.resetAllFilters(); }
    @FXML private void handleFilterToDo() { filtersPane.setFilterStatus(false); }
    @FXML private void handleFilterCompleted() { filtersPane.setFilterStatus(true); }
    @FXML private void handleStatistics() {}

    // =========================================================
    //  CRUD TASK (CON PROTEZIONE DATA)
    // =========================================================
    @FXML
    private void handleNewTask() {
        String titolo = newTaskField.getText().trim();
        if (titolo.isEmpty()) { showAlert("Titolo obbligatorio"); return; }

        // CHECK DI SICUREZZA AGGIUNTIVO PRIMA DI INSERIRE
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
            t.setIdUtente(MainApp.getCurrentUser().getIdUtente());
            t.setIdCategoria(idCat);
            t.setCompletamento(false);

            DAOTasks.getInstance().insert(t);
            tasksListHelper.addTask(t);

            // Reset campi
            newTaskField.clear(); descriptionArea.clear();
            dueDateField.setValue(null);
            categoryComboBox.getSelectionModel().clearSelection();
            priorityComboBox.getSelectionModel().clearSelection();

        } catch (DAOException e) { showAlert("Errore inserimento: " + e.getMessage()); }
    }

    private void handleEditTask(Tasks t) {
        if (mainApp.showTasksEditDialog(t)) {
            try {
                DAOTasks.getInstance().update(t);
                tasksListHelper.updateTaskInList(t);
                if (tasksInfoPane.isOpen() && tasksInfoPane.getCurrentTask().equals(t)) {
                    String catName = tasksListHelper.getCategoryName(t.getIdCategoria(), categoryComboBox.getItems());
                    tasksInfoPane.openPanel(t, catName);
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

    // =========================================================
    //  SETUP UI E VALIDAZIONE DATA
    // =========================================================
    private void setupCreationForm() {
        if (priorityComboBox != null) {
            priorityComboBox.getItems().setAll("BASSA", "MEDIA", "ALTA");
        }
        categoryComboBox.setConverter(new StringConverter<>() {
            @Override public String toString(Categorie c) { return c==null?"":c.getNomeCategoria(); }
            @Override public Categorie fromString(String s) { return null; }
        });

        // --- PROTEZIONE CAMPO DATA ---
        if (dueDateField != null) {
            // 1. Non scrivibile a mano
            dueDateField.setEditable(false);

            // 2. Giorni passati colorati e disabilitati
            dueDateField.setDayCellFactory(picker -> new DateCell() {
                @Override
                public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    if (date != null && !empty && date.isBefore(LocalDate.now())) {
                        setDisable(true);
                        setStyle("-fx-background-color: #ffc0cb;"); // Rosa
                    }
                }
            });

            // 3. Listener automatico che resetta se selezioni data vecchia
            dueDateField.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && newVal.isBefore(LocalDate.now())) {
                    dueDateField.setValue(null);
                }
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
    @FXML private void handleExit() { System.exit(0); }
    @FXML private void handleProfile() { mainApp.showUtentiProfile(MainApp.getCurrentUser()); }

    private void showAlert(String msg) { new Alert(Alert.AlertType.WARNING, msg).show(); }
}