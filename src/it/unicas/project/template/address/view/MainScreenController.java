package it.unicas.project.template.address.view;

import it.unicas.project.template.address.MainApp;
import it.unicas.project.template.address.model.Categorie;
import it.unicas.project.template.address.model.SubTasks;
import it.unicas.project.template.address.model.Tasks;
import it.unicas.project.template.address.model.TimerSessions; // IMPORTATO IL MODEL
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

public class MainScreenController {

    @FXML private TextField txtSearch;

    @FXML private VBox sideMenu;
    @FXML private Label usernameLabelHeader;
    @FXML private ListView<Tasks> taskListView;
    @FXML private ScrollPane gridViewContainer;
    @FXML private FlowPane gridFlowPane;

    @FXML private BorderPane calendarViewContainer;
    @FXML private GridPane calendarGrid;
    @FXML private ScrollPane weekViewContainer;
    @FXML private HBox weekViewBox;
    @FXML private Label calendarMonthLabel;

    @FXML private Label viewLabel;

    @FXML private VBox categoryMenuContainer;
    @FXML private ComboBox<String> filterPriorityCombo;
    @FXML private DatePicker filterDatePicker;

    @FXML private ToggleButton btnFilterTodo;
    @FXML private ToggleButton btnFilterDone;

    @FXML private VBox rightDetailPanel;
    @FXML private Label detailTitleLabel, detailCategoryLabel;
    @FXML private DatePicker detailDueDatePicker;
    @FXML private TextArea detailDescArea;
    @FXML private ListView<SubTasks> subTaskListView;
    @FXML private TextField newSubTaskField;

    // --- CAMPI TIMER BASE ---
    @FXML private Label timerLabel;
    @FXML private Label timerStatusLabel;
    @FXML private Button btnTimerToggle;
    @FXML private Button btnTimerReset;

    // --- NUOVI CAMPI TIMER (STORICO & MENU) ---
    @FXML private Button btnTimerMenu;           // La freccia ▼
    @FXML private VBox timerHistoryContainer;    // Il contenitore a tendina nascosto
    @FXML private ListView<TimerSessions> timerHistoryList; // La lista delle sessioni
    @FXML private Label timerTotalLabel;         // La label del totale ore

    @FXML private TextField newTaskField;
    @FXML private TextArea descriptionArea;
    @FXML private ComboBox<Categorie> categoryComboBox;
    @FXML private ComboBox<String> priorityComboBox;
    @FXML private DatePicker dueDateField;

    private MainApp mainApp;
    private boolean isSideMenuOpen = false;

    private TasksList tasksListHelper;
    private TasksInfoPane tasksInfoPane;
    private FiltersPane filtersPane;

    // --- STILI DEFINITIVI ---
    private final String STYLE_COMMON = "-fx-alignment: CENTER_LEFT; -fx-padding: 10 15 10 15; -fx-cursor: hand; -fx-background-radius: 0; -fx-font-size: 13px; ";
    private final String STYLE_NORMAL = STYLE_COMMON + "-fx-background-color: transparent; -fx-text-fill: #aaaaaa; -fx-border-width: 0;";
    private final String STYLE_SELECTED = STYLE_COMMON + "-fx-background-color: #2F223D; -fx-text-fill: #F071A7; -fx-font-weight: bold; -fx-border-color: #F071A7; -fx-border-width: 0 0 0 3;";

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
        Utenti u = MainApp.getCurrentUser();
        if (u != null) usernameLabelHeader.setText(u.getNome());

        if (tasksListHelper != null && u != null) {
            tasksListHelper.loadTasks(u.getIdUtente());
        }
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

        if (txtSearch != null) {
            txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
                tasksListHelper.setFilterKeyword(newValue);
            });
        }

        // --- INIZIALIZZAZIONE AGGIORNATA TASKSINFOPANE ---
        // Passiamo tutti i componenti, inclusi quelli nuovi dello storico
        tasksInfoPane = new TasksInfoPane(
                rightDetailPanel, detailTitleLabel, detailCategoryLabel,
                detailDueDatePicker, detailDescArea, subTaskListView,
                newSubTaskField, taskListView,

                // Vecchi parametri timer
                timerLabel, timerStatusLabel, btnTimerToggle, btnTimerReset,

                // NUOVI parametri timer (Menu a tendina e storico)
                btnTimerMenu, timerHistoryContainer, timerHistoryList, timerTotalLabel
        );

        filtersPane = new FiltersPane(filterPriorityCombo, filterDatePicker,
                btnFilterTodo, btnFilterDone, categoryMenuContainer,
                categoryComboBox, tasksListHelper);

        setupCreationForm();

        // --- APPLICAZIONE DESIGN BOTTONI ---
        setupFilterButtonDesign(btnFilterTodo);
        setupFilterButtonDesign(btnFilterDone);

        taskListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Tasks sel = taskListView.getSelectionModel().getSelectedItem();
                if (sel != null) handleOpenDetail(sel);
            }
        });


    }

    // --- NUOVO EVENT HANDLER PER LA FRECCIA DEL TIMER ---
    @FXML
    private void handleToggleTimerMenu() {
        if (tasksInfoPane != null) {
            tasksInfoPane.toggleHistoryMenu();
        }
    }

    @FXML
    private void handleTimerToggle() {
        if (tasksInfoPane != null) {
            tasksInfoPane.toggleTimer();
        }
    }

    private void setupFilterButtonDesign(ToggleButton btn) {
        if (btn == null) return;
        btn.setStyle(STYLE_NORMAL);
        btn.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (isSelected) btn.setStyle(STYLE_SELECTED);
            else btn.setStyle(STYLE_NORMAL);
        });
        btn.hoverProperty().addListener((obs, wasHovered, isHovered) -> {
            if (!btn.isSelected()) {
                if (isHovered) btn.setStyle(STYLE_COMMON + "-fx-background-color: #2F223D; -fx-text-fill: white; -fx-border-width: 0;");
                else btn.setStyle(STYLE_NORMAL);
            }
        });


    }

    @FXML
    private void showListView() {
        tasksListHelper.switchView(TasksList.ViewMode.LIST);
        if (viewLabel != null) viewLabel.setText("Vista: Lista");
    }

    @FXML
    private void showGridView() {
        tasksListHelper.switchView(TasksList.ViewMode.GRID);
        if (viewLabel != null) viewLabel.setText("Vista: Board");
    }

    @FXML
    private void showCalendarView() {
        tasksListHelper.switchView(TasksList.ViewMode.CALENDAR);
        if (viewLabel != null) viewLabel.setText("Vista: Calendario");
    }

    @FXML private void prevMonth() { tasksListHelper.calendarBack(); }
    @FXML private void nextMonth() { tasksListHelper.calendarForward(); }

    @FXML private void handleCalViewMonth() { tasksListHelper.setCalendarMode(TasksList.CalendarMode.MONTH); }
    @FXML private void handleCalViewWeek()  { tasksListHelper.setCalendarMode(TasksList.CalendarMode.WEEK); }
    @FXML private void handleCalViewDay()   { tasksListHelper.setCalendarMode(TasksList.CalendarMode.DAY); }

    @FXML
    private void handleTimerReset() {
        if (tasksInfoPane != null) {
            tasksInfoPane.resetTimer();
        }
    }

    private void handleOpenDetail(Tasks t) {
        String catName = tasksListHelper.getCategoryName(t.getIdCategoria(), categoryComboBox.getItems());
        tasksInfoPane.openPanel(t, catName);
    }

    @FXML private void closeRightPanel() { tasksInfoPane.closePanel(); }
    @FXML private void handleNewSubTask() { tasksInfoPane.createSubTask(); }

    @FXML
    private void handleShowAll() {
        filtersPane.resetAllFilters();
        if (txtSearch != null) txtSearch.clear();
        if (btnFilterTodo != null) btnFilterTodo.setSelected(false);
        if (btnFilterDone != null) btnFilterDone.setSelected(false);
    }

    @FXML
    private void handleFilterToDo() {
        if (btnFilterTodo.isSelected()) {
            btnFilterDone.setSelected(false);
            filtersPane.setFilterStatus(false);
        } else {
            filtersPane.setFilterStatus(null);
        }
    }

    @FXML
    private void handleFilterCompleted() {
        if (btnFilterDone.isSelected()) {
            btnFilterTodo.setSelected(false);
            filtersPane.setFilterStatus(true);
        } else {
            filtersPane.setFilterStatus(null);
        }
    }

    @FXML private void handleStatistics() { mainApp.showBirthdayStatistics(); }

    @FXML
    private void handleNewTask() {
        String titolo = newTaskField.getText().trim();
        if (titolo.isEmpty()) { showAlert("Titolo obbligatorio"); return; }
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
            tasksListHelper.addTask(t);

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

    private void setupCreationForm() {
        if (priorityComboBox != null) {
            priorityComboBox.getItems().setAll("BASSA", "MEDIA", "ALTA");
        }
        categoryComboBox.setConverter(new StringConverter<>() {
            @Override public String toString(Categorie c) { return c==null?"":c.getNomeCategoria(); }
            @Override public Categorie fromString(String s) { return null; }
        });
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

    private void showAlert(String msg) { new Alert(Alert.AlertType.WARNING, msg).show(); }
}