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

public class MainScreenController {

    // --- FXML Components: TOP BAR ---
    @FXML private VBox sideMenu;
    @FXML private Label usernameLabelHeader;
    // I toggle button della top bar (Lista, Board, Calendario)
    @FXML private ToggleButton tglList, tglBoard, tglCalendar;

    // --- AREA CENTRALE (VISTE MULTIPLE) ---
    @FXML private ListView<Tasks> taskListView; // Vista 1: Lista

    @FXML private ScrollPane gridViewContainer; // Vista 2: Griglia
    @FXML private FlowPane gridFlowPane;

    // Vista 3: Calendario (Aggiornato per supportare Mese/Settimana)
    @FXML private BorderPane calendarViewContainer; // NOTA: Ora è un BorderPane nel nuovo FXML
    @FXML private GridPane calendarGrid;            // Griglia Mese
    @FXML private ScrollPane weekViewContainer;     // Scroll orizzontale per Settimana
    @FXML private HBox weekViewBox;                 // Contenitore colonne Settimana
    @FXML private Label calendarMonthLabel;         // Titolo periodo

    // Etichetta informativa vista (Opzionale, se presente nell'FXML)
    @FXML private Label viewLabel;

    // --- SIDEBAR FILTRI (ACCORDION) ---
    @FXML private VBox categoryMenuContainer;
    @FXML private ComboBox<String> filterPriorityCombo;
    @FXML private DatePicker filterDatePicker;
    @FXML private Button btnFilterTodo, btnFilterDone;

    // --- PANNELLO DETTAGLI (DX) ---
    @FXML private VBox rightDetailPanel;
    @FXML private Label detailTitleLabel, detailCategoryLabel;
    @FXML private DatePicker detailDueDatePicker;
    @FXML private TextArea detailDescArea;
    @FXML private ListView<SubTasks> subTaskListView;
    @FXML private TextField newSubTaskField;

    // --- TIMER COMPONENTS ---
    @FXML private Label timerLabel;
    @FXML private Label timerStatusLabel;
    @FXML private Button btnTimerToggle;
    @FXML private Button btnTimerReset;

    // --- FORM CREAZIONE (BASSO) ---
    @FXML private TextField newTaskField;
    @FXML private TextArea descriptionArea;
    @FXML private ComboBox<Categorie> categoryComboBox;
    @FXML private ComboBox<String> priorityComboBox;
    @FXML private DatePicker dueDateField;

    // --- LOGICA ---
    private MainApp mainApp;
    private boolean isSideMenuOpen = false;

    // --- I MANAGERS ---
    private TasksList tasksListHelper;
    private TasksInfoPane tasksInfoPane;
    private FiltersPane filtersPane;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
        Utenti u = MainApp.getCurrentUser();
        if (u != null) usernameLabelHeader.setText(u.getNome());

        // Carichiamo i task. Le categorie si caricano nel setup di FiltersPane
        tasksListHelper.loadTasks(u.getIdUtente());
    }

    @FXML
    private void initialize() {
        // 1. Setup Helper LISTA (Passiamo tutti i container delle viste, inclusi quelli nuovi per la settimana)
        tasksListHelper = new TasksList(
                taskListView, gridViewContainer, gridFlowPane,
                calendarViewContainer, calendarGrid, weekViewContainer, weekViewBox, calendarMonthLabel,
                mainApp,
                this::handleEditTask,   // Edit callback
                this::handleDeleteTask, // Delete callback
                this::handleOpenDetail  // Click callback
        );

        // 2. Setup Pannello Dettagli (Passiamo anche i componenti del TIMER)
        tasksInfoPane = new TasksInfoPane(
                rightDetailPanel, detailTitleLabel, detailCategoryLabel,
                detailDueDatePicker, detailDescArea, subTaskListView,
                newSubTaskField, taskListView,
                timerLabel, timerStatusLabel, btnTimerToggle, btnTimerReset // Nuovi parametri
        );

        // 3. Setup Filtri e Categorie
        filtersPane = new FiltersPane(filterPriorityCombo, filterDatePicker,
                btnFilterTodo, btnFilterDone, categoryMenuContainer,
                categoryComboBox, tasksListHelper);

        // 4. Setup Form Creazione
        setupCreationForm();

        // 5. Listener Doppio Click (Solo per ListView standard)
        taskListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Tasks sel = taskListView.getSelectionModel().getSelectedItem();
                if (sel != null) handleOpenDetail(sel);
            }
        });
    }

    // =========================================================
    //  GESTIONE VISTE (Top Bar Switcher)
    // =========================================================
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

    // =========================================================
    //  GESTIONE CALENDARIO (Mese / Settimana / Giorno)
    // =========================================================
    @FXML private void prevMonth() { tasksListHelper.calendarBack(); }
    @FXML private void nextMonth() { tasksListHelper.calendarForward(); }

    // NOTA: Qui usiamo TasksList.CalendarMode perché l'enum è definito dentro TasksList
    @FXML private void handleCalViewMonth() { tasksListHelper.setCalendarMode(TasksList.CalendarMode.MONTH); }
    @FXML private void handleCalViewWeek()  { tasksListHelper.setCalendarMode(TasksList.CalendarMode.WEEK); }
    @FXML private void handleCalViewDay()   { tasksListHelper.setCalendarMode(TasksList.CalendarMode.DAY); }


    // =========================================================
    //  TIMER & DETTAGLI
    // =========================================================
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

    // =========================================================
    //  FILTRI
    // =========================================================
    @FXML private void handleShowAll() { filtersPane.resetAllFilters(); }
    @FXML private void handleFilterToDo() { filtersPane.setFilterStatus(false); }
    @FXML private void handleFilterCompleted() { filtersPane.setFilterStatus(true); }
    // handleStatistics da implementare...
    @FXML private void handleStatistics() {}

    // =========================================================
    //  CRUD TASK
    // =========================================================
    @FXML
    private void handleNewTask() {
        String titolo = newTaskField.getText().trim();
        if (titolo.isEmpty()) { showAlert("Titolo obbligatorio"); return; }

        try {
            Integer idCat = null;
            if (categoryComboBox.getValue() != null) idCat = categoryComboBox.getValue().getIdCategoria();

            Tasks t = new Tasks();
            t.setTitolo(titolo);
            t.setDescrizione(descriptionArea.getText()); // Prendiamo la descrizione dall'area grande
            t.setPriorita(priorityComboBox.getValue());  // Prendiamo la priorità
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

                // Aggiorna pannello dettagli se aperto su quel task
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
    //  UTILITIES UI
    // =========================================================
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
    @FXML private void handleExit() { System.exit(0); }
    @FXML private void handleProfile() { mainApp.showUtentiProfile(MainApp.getCurrentUser()); }

    private void showAlert(String msg) { new Alert(Alert.AlertType.WARNING, msg).show(); }
}