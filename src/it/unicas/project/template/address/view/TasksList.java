package it.unicas.project.template.address.view;

import it.unicas.project.template.address.MainApp;
import it.unicas.project.template.address.model.Categorie;
import it.unicas.project.template.address.model.Tasks;
import it.unicas.project.template.address.model.dao.DAOException;
import it.unicas.project.template.address.model.dao.mysql.DAOTasks;
import it.unicas.project.template.address.util.DateUtil;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.function.Consumer;

/**
 * Controller helper che gestisce la logica di visualizzazione della lista dei task.
 * <p>
 * Questa classe si occupa di:
 * <ul>
 * <li>Gestire le diverse modalità di visualizzazione: Lista, Griglia (Board) e Calendario (Mese/Settimana/Giorno).</li>
 * <li>Applicare filtri dinamici (categoria, stato, priorità, data, parola chiave).</li>
 * <li>Ordinare i task in base a completamento, priorità e scadenza.</li>
 * <li>Gestire il rendering personalizzato delle celle (Card grafiche).</li>
 * <li>Sincronizzare i dati con il database in background.</li>
 * </ul>
 */
public class TasksList {

    /** Restituisce la lista osservabile dei task caricati. */
    public ObservableList<Tasks> getTasks() { return tasks; }

    /** Modalità di visualizzazione disponibili. */
    public enum ViewMode { LIST, GRID, CALENDAR }
    /** Modalità di visualizzazione del calendario. */
    public enum CalendarMode { MONTH, WEEK, DAY }

    // --- Componenti UI iniettati ---
    private final ListView<Tasks> taskListView;
    private final ScrollPane gridViewContainer;
    private final FlowPane gridFlowPane;

    private final BorderPane calendarViewContainer;
    private final GridPane calendarGrid;
    private final ScrollPane weekViewContainer;
    private final HBox weekViewBox;
    private final Label calendarMonthLabel;
    private ScrollPane monthScrollPane;

    // --- Callback ---
    private final MainApp mainApp;
    private final Consumer<Tasks> onEditRequest;
    private final Consumer<Tasks> onDeleteRequest;
    private final Consumer<Tasks> onItemClick;

    // --- Dati ---
    private ObservableList<Tasks> tasks;
    private SortedList<Tasks> sortedTasks;
    private FilteredList<Tasks> filteredTasks;

    // --- Stato Filtri ---
    private Categorie filterCategory = null;
    private Boolean filterStatus = null;
    private String filterPriority = null;
    private LocalDate filterDate = null;
    private String filterKeyword = null;

    // --- Stato Visualizzazione ---
    private ViewMode currentViewMode = ViewMode.LIST;
    private CalendarMode currentCalendarMode = CalendarMode.MONTH;
    private LocalDate currentCalendarDate = LocalDate.now();

    /** Mappa per tenere traccia del momento esatto del completamento per l'ordinamento. */
    private final Map<Integer, Long> completionTimestamps = new HashMap<>();

    /**
     * Costruttore principale.
     * Inizializza i componenti e la logica interna.
     *
     * @param taskListView Lista visuale standard.
     * @param gridViewContainer Contenitore scrollabile per la griglia.
     * @param gridFlowPane Pannello a flusso per le card della griglia.
     * @param calendarViewContainer Contenitore principale vista calendario.
     * @param calendarGrid Griglia per il calendario mensile.
     * @param weekViewContainer Contenitore scrollabile vista settimanale.
     * @param weekViewBox Box orizzontale vista settimanale.
     * @param calendarMonthLabel Etichetta mese/anno calendario.
     * @param mainApp Riferimento all'applicazione principale.
     * @param onEditRequest Callback per la modifica task.
     * @param onDeleteRequest Callback per eliminazione task.
     * @param onItemClick Callback per click su task (apertura dettagli).
     */
    public TasksList(ListView<Tasks> taskListView,
                     ScrollPane gridViewContainer, FlowPane gridFlowPane,
                     BorderPane calendarViewContainer, GridPane calendarGrid,
                     ScrollPane weekViewContainer, HBox weekViewBox, Label calendarMonthLabel,
                     MainApp mainApp,
                     Consumer<Tasks> onEditRequest, Consumer<Tasks> onDeleteRequest, Consumer<Tasks> onItemClick) {

        this.taskListView = taskListView;
        this.gridViewContainer = gridViewContainer;
        this.gridFlowPane = gridFlowPane;
        this.calendarViewContainer = calendarViewContainer;
        this.calendarGrid = calendarGrid;
        this.weekViewContainer = weekViewContainer;
        this.weekViewBox = weekViewBox;
        this.calendarMonthLabel = calendarMonthLabel;

        this.mainApp = mainApp;
        this.onEditRequest = onEditRequest;
        this.onDeleteRequest = onDeleteRequest;
        this.onItemClick = onItemClick;

        init();
    }

    /**
     * Inizializza le liste osservabili, il comparatore per l'ordinamento e configura i componenti UI.
     */
    private void init() {
        tasks = FXCollections.observableArrayList();
        filteredTasks = new FilteredList<>(tasks, t -> true);
        sortedTasks = new SortedList<>(filteredTasks);

        // Comparator personalizzato:
        // 1. Task completati in fondo.
        // 2. Task completati ordinati per data completamento decrescente.
        // 3. Task aperti ordinati per data scadenza.
        sortedTasks.setComparator((t1, t2) -> {
            if (t1.getCompletamento() != t2.getCompletamento()) return t1.getCompletamento() ? 1 : -1;
            if (t1.getCompletamento()) {
                long ts1 = completionTimestamps.getOrDefault(t1.getIdTask(), 0L);
                long ts2 = completionTimestamps.getOrDefault(t2.getIdTask(), 0L);
                int cmp = Long.compare(ts2, ts1);
                if (cmp != 0) return cmp;
            }
            String d1 = t1.getScadenza() == null ? "9999-12-31" : t1.getScadenza();
            String d2 = t2.getScadenza() == null ? "9999-12-31" : t2.getScadenza();
            int dateCmp = d1.compareTo(d2);
            if (dateCmp != 0) return dateCmp;
            Integer id1 = t1.getIdTask() != null ? t1.getIdTask() : 0;
            Integer id2 = t2.getIdTask() != null ? t2.getIdTask() : 0;
            return Integer.compare(id1, id2);
        });

        taskListView.setItems(sortedTasks);
        setupCellFactory();

        // Configurazione responsive per la griglia e il calendario
        if (gridViewContainer != null) {
            gridViewContainer.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        }
        if (gridFlowPane != null) {
            gridFlowPane.widthProperty().addListener((obs, oldVal, newVal) -> {
                if (currentViewMode == ViewMode.GRID) resizeGridCards(newVal.doubleValue());
            });
        }
        if(weekViewContainer != null) {
            weekViewContainer.widthProperty().addListener((obs, o, n) -> {
                if(currentViewMode == ViewMode.CALENDAR && currentCalendarMode == CalendarMode.WEEK) refreshView();
            });
        }

        // ScrollPane dedicato per il mese
        monthScrollPane = new ScrollPane(calendarGrid);
        monthScrollPane.setFitToWidth(true);
        monthScrollPane.setFitToHeight(false);
        monthScrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0;");
        monthScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        monthScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
    }

    /**
     * Recupera il nome della categoria dato il suo ID.
     * @param id ID della categoria.
     * @param allCats Lista di tutte le categorie disponibili.
     * @return Nome della categoria o stringa vuota se non trovata.
     */
    public String getCategoryName(Integer id, List<Categorie> allCats) {
        if (id == null || allCats == null) return "";
        return allCats.stream()
                .filter(c -> c.getIdCategoria().equals(id))
                .findFirst()
                .map(Categorie::getNomeCategoria)
                .orElse("");
    }

    /**
     * Parsing intelligente della data (gestisce formati ISO e custom).
     * @param dateString Stringa data.
     * @return LocalDate o null se parsing fallisce.
     */
    private LocalDate smartParse(String dateString) {
        if (dateString == null || dateString.isEmpty()) return null;
        try { return LocalDate.parse(dateString); }
        catch (Exception e) { return DateUtil.parse(dateString); }
    }

    /**
     * Restituisce il colore esadecimale associato alla priorità del task.
     * @param priority Priorità (ALTA, MEDIA, BASSA).
     * @param isCompleted Se il task è completato (colori più tenui).
     * @return Stringa colore HEX.
     */
    private String getPriorityColor(String priority, boolean isCompleted) {
        String p = (priority != null) ? priority.toUpperCase() : "";
        if (isCompleted) {
            return switch (p) {
                case "ALTA" -> "#a96b6b";
                case "MEDIA" -> "#a9986b";
                default -> "#6ba983";
            };
        } else {
            return switch (p) {
                case "ALTA" -> "#e74c3c";
                case "MEDIA" -> "#f39c12";
                default -> "#27ae60";
            };
        }
    }

    /**
     * Avvia il caricamento dei task dal database per l'utente specificato.
     * @param userId ID utente.
     */
    public void loadTasks(Integer userId) { reloadTasksFromDB(); }

    /**
     * Ricarica i task dal database in un thread separato, applicando i filtri di base.
     */
    private void reloadTasksFromDB() {
        if (MainApp.getCurrentUser() == null) return;
        Thread dbThread = new Thread(() -> {
            try {
                Tasks filterTemplate = new Tasks();
                filterTemplate.setIdUtente(MainApp.getCurrentUser().getIdUtente());
                if (filterCategory != null) filterTemplate.setIdCategoria(filterCategory.getIdCategoria());
                if (filterStatus != null) filterTemplate.setCompletamento(filterStatus);
                if (filterPriority != null && !filterPriority.equalsIgnoreCase("TUTTE")) filterTemplate.setPriorita(filterPriority);

                List<Tasks> results = DAOTasks.getInstance().select(filterTemplate);
                Platform.runLater(() -> {
                    tasks.setAll(results);
                    applyFilters();

                    // Refresh forzato per evitare glitch grafici nella lista
                    if (currentViewMode == ViewMode.LIST) {
                        taskListView.refresh();
                    } else {
                        refreshView();
                    }
                });
            } catch (DAOException e) {
                Platform.runLater(() -> {
                    e.printStackTrace();
                    new Alert(Alert.AlertType.ERROR, "Errore DB: " + e.getMessage()).show();
                });
            }
        });
        dbThread.setDaemon(true);
        dbThread.start();
    }

    /**
     * Imposta il filtro per parola chiave (cerca in titolo e descrizione).
     * @param keyword Parola chiave.
     */
    public void setFilterKeyword(String keyword) {
        this.filterKeyword = (keyword != null) ? keyword.trim() : null;
        applyFilters();
        if(currentViewMode == ViewMode.LIST) taskListView.refresh();
        else refreshView();
    }

    public void addTask(Tasks t) { tasks.add(t); refreshView(); }

    public void updateTaskInList(Tasks t) {
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getIdTask().equals(t.getIdTask())) { tasks.set(i, t); break; }
        }
        refreshView();
    }

    public void removeTask(Tasks t) { tasks.remove(t); refreshView(); }

    /**
     * Cambia la modalità di visualizzazione corrente.
     * @param mode Nuova modalità (LIST, GRID, CALENDAR).
     */
    public void switchView(ViewMode mode) {
        this.currentViewMode = mode;
        taskListView.setVisible(mode == ViewMode.LIST); taskListView.setManaged(mode == ViewMode.LIST);
        gridViewContainer.setVisible(mode == ViewMode.GRID); gridViewContainer.setManaged(mode == ViewMode.GRID);
        calendarViewContainer.setVisible(mode == ViewMode.CALENDAR); calendarViewContainer.setManaged(mode == ViewMode.CALENDAR);
        refreshView();
    }

    public ViewMode getCurrentViewMode() { return currentViewMode; }

    public void setCalendarMode(CalendarMode mode) { this.currentCalendarMode = mode; refreshView(); }

    /** Sposta il calendario avanti di un'unità temporale (mese/settimana/giorno). */
    public void calendarForward() {
        switch(currentCalendarMode) {
            case MONTH -> currentCalendarDate = currentCalendarDate.plusMonths(1);
            case WEEK -> currentCalendarDate = currentCalendarDate.plusWeeks(1);
            case DAY -> currentCalendarDate = currentCalendarDate.plusDays(1);
        }
        refreshView();
    }

    /** Sposta il calendario indietro di un'unità temporale. */
    public void calendarBack() {
        switch(currentCalendarMode) {
            case MONTH -> currentCalendarDate = currentCalendarDate.minusMonths(1);
            case WEEK -> currentCalendarDate = currentCalendarDate.minusWeeks(1);
            case DAY -> currentCalendarDate = currentCalendarDate.minusDays(1);
        }
        refreshView();
    }

    /** Aggiorna la vista corrente nel thread UI. */
    private void refreshView() {
        Platform.runLater(() -> {
            if (currentViewMode == ViewMode.GRID) renderGrid();
            else if (currentViewMode == ViewMode.CALENDAR) renderCalendarDispatcher();
            else taskListView.refresh();
        });
    }


    public void setFilterCategory(Categorie c) { this.filterCategory = c; reloadTasksFromDB(); }
    public void setFilterStatus(Boolean s) { this.filterStatus = s; reloadTasksFromDB(); }
    public void setFilterPriority(String p) { this.filterPriority = p; reloadTasksFromDB(); }
    public void setFilterDate(LocalDate d) {
        this.filterDate = d;
        if (d != null && currentViewMode == ViewMode.CALENDAR) currentCalendarDate = d;
        applyFilters();
        refreshView();
    }

    /** Resetta tutti i filtri e ricarica i dati. */
    public void clearFilters() {
        this.filterCategory = null; this.filterStatus = null;
        this.filterPriority = null; this.filterDate = null; this.filterKeyword = null;

        if (filteredTasks != null) {
            filteredTasks.setPredicate(t -> true);
        }

        reloadTasksFromDB();
    }

    /**
     * Applica i filtri correnti alla FilteredList in memoria.
     * Combina in AND logico tutti i criteri (Categoria, Stato, Priorità, Data, Keyword).
     */
    private void applyFilters() {
        filteredTasks.setPredicate(task -> {
            boolean catMatch = (filterCategory == null) || (task.getIdCategoria() != null && task.getIdCategoria().equals(filterCategory.getIdCategoria()));
            boolean statMatch = (filterStatus == null) || (task.getCompletamento() == filterStatus);
            boolean prioMatch = true;
            if (filterPriority != null && !filterPriority.isEmpty() && !filterPriority.equalsIgnoreCase("TUTTE")) {
                prioMatch = task.getPriorita() != null && task.getPriorita().equalsIgnoreCase(filterPriority);
            }
            boolean dateMatch = true;
            if (filterDate != null) {
                if (task.getScadenza() == null || task.getScadenza().isEmpty()) { dateMatch = false; }
                else {
                    LocalDate tDate = smartParse(task.getScadenza());
                    dateMatch = (tDate != null && tDate.isEqual(filterDate));
                }
            }

            boolean keyMatch = true;
            if (filterKeyword != null && !filterKeyword.isEmpty()) {
                String lowerKey = filterKeyword.toLowerCase();
                boolean titleHit = task.getTitolo() != null && task.getTitolo().toLowerCase().contains(lowerKey);
                boolean descHit = task.getDescrizione() != null && task.getDescrizione().toLowerCase().contains(lowerKey);
                keyMatch = titleHit || descHit;
            }

            return catMatch && statMatch && prioMatch && dateMatch && keyMatch;
        });
    }

    /** Rendering della vista a griglia (Board). */
    private void renderGrid() {
        gridFlowPane.getChildren().clear();
        for (Tasks t : sortedTasks) { gridFlowPane.getChildren().add(createGridCard(t)); }
        resizeGridCards(gridFlowPane.getWidth());
    }

    /** Adatta la larghezza delle card nella griglia per riempire lo spazio (responsive). */
    private void resizeGridCards(double containerWidth) {
        if (containerWidth <= 10) return;
        double gap = gridFlowPane.getHgap();
        double padding = 60;
        double availableWidth = containerWidth - padding;
        double minCardWidth = 240.0;
        int columns = (int) Math.max(1, Math.floor((availableWidth + gap) / (minCardWidth + gap)));
        double newWidth = (availableWidth - (gap * (columns - 1))) / columns;
        for (Node node : gridFlowPane.getChildren()) { if (node instanceof VBox) ((VBox) node).setPrefWidth(newWidth); }
    }

    /** Crea una card grafica per la visualizzazione a griglia. */
    private VBox createGridCard(Tasks task) {
        VBox scheda = new VBox(8);
        scheda.getStyleClass().add("scheda-task");
        scheda.setStyle("-fx-background-color: #2F223D; -fx-background-radius: 10; -fx-padding: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 8, 0, 0, 2);");
        scheda.setPrefHeight(140);
        scheda.setMinHeight(140); scheda.setMaxHeight(140);

        HBox header = new HBox(8); header.setAlignment(Pos.CENTER_LEFT);
        Circle dot = new Circle(5);
        String colorHex = getPriorityColor(task.getPriorita(), task.getCompletamento());
        dot.setFill(Color.web(colorHex));

        Text textTitolo = new Text(task.getTitolo());
        textTitolo.setFont(Font.font("System", FontWeight.BOLD, 15));

        if (task.getCompletamento()) {
            textTitolo.setFill(Color.web("#aaaaaa"));
            textTitolo.setStrikethrough(true);
        } else {
            textTitolo.setFill(Color.WHITE);
            textTitolo.setStrikethrough(false);
        }

        textTitolo.setWrappingWidth(180);

        header.getChildren().addAll(dot, textTitolo);

        String descText = (task.getDescrizione() != null) ? task.getDescrizione() : "Nessuna descrizione";
        Label lblDesc = new Label(descText);
        lblDesc.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 11px;");
        lblDesc.setWrapText(true); lblDesc.setAlignment(Pos.TOP_LEFT); lblDesc.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(lblDesc, Priority.ALWAYS);

        HBox footer = new HBox(); footer.setAlignment(Pos.CENTER_LEFT);
        Label lblData = new Label();
        String dateText = ""; String dateStyle = "-fx-text-fill: #666; -fx-font-size: 10px;";
        if (task.getScadenza() != null && !task.getScadenza().isEmpty() && !task.getCompletamento()) {
            try {
                LocalDate scad = smartParse(task.getScadenza());
                if (scad != null) {
                    LocalDate oggi = LocalDate.now();
                    if (scad.isBefore(oggi)) { dateText = "⚠ SCADUTA IL " + DateUtil.format(scad); dateStyle = "-fx-text-fill: #FF5555; -fx-font-weight: bold; -fx-font-size: 10px;"; }
                    else if (scad.isEqual(oggi)) { dateText = "🔥 SCADE OGGI"; dateStyle = "-fx-text-fill: #FFB86C; -fx-font-weight: bold; -fx-font-size: 10px;"; }
                    else if (scad.isEqual(oggi.plusDays(1))) { dateText = "⏳ SCADE DOMANI"; dateStyle = "-fx-text-fill: #8BE9FD; -fx-font-weight: bold; -fx-font-size: 10px;"; }
                    else { dateText = "📅 " + DateUtil.format(scad); dateStyle = "-fx-text-fill: #bd93f9; -fx-font-size: 10px;"; }
                } else { dateText = task.getScadenza(); }
            } catch (Exception e) { dateText = task.getScadenza(); }
        }
        lblData.setText(dateText); lblData.setStyle(dateStyle);
        footer.getChildren().add(lblData);
        scheda.getChildren().addAll(header, lblDesc, footer);
        if (Boolean.TRUE.equals(task.getCompletamento())) {
            scheda.setOpacity(0.5);
            scheda.setStyle(scheda.getStyle() + "-fx-border-color: #6c6c6c; -fx-border-width: 1;");
        }
        scheda.setOnMouseClicked(e -> { if (e.getClickCount() == 2 && onItemClick != null) onItemClick.accept(task); });
        return scheda;
    }

    /** Smista il rendering del calendario in base alla modalità corrente (Mese, Settimana, Giorno). */
    private void renderCalendarDispatcher() {
        if (currentCalendarMode == CalendarMode.MONTH) {
            monthScrollPane.setVisible(true); monthScrollPane.setManaged(true);
            calendarViewContainer.setCenter(monthScrollPane);
            weekViewContainer.setVisible(false); weekViewContainer.setManaged(false);
            renderMonthView();
        } else {
            monthScrollPane.setVisible(false); monthScrollPane.setManaged(false);
            weekViewContainer.setVisible(true); weekViewContainer.setManaged(true);
            calendarViewContainer.setCenter(weekViewContainer);
            if (currentCalendarMode == CalendarMode.WEEK) renderWeekView();
            else renderDayView();
        }
    }

    /** Raggruppa i task per data di scadenza. */
    private Map<LocalDate, List<Tasks>> groupTasksByDate(List<Tasks> taskList) {
        Map<LocalDate, List<Tasks>> map = new HashMap<>();
        for (Tasks t : taskList) {
            if (t.getScadenza() != null && !t.getScadenza().isEmpty()) {
                LocalDate d = smartParse(t.getScadenza());
                if (d != null) {
                    map.computeIfAbsent(d, k -> new ArrayList<>()).add(t);
                }
            }
        }
        return map;
    }

    /** Rendering vista mensile del calendario. */
    private void renderMonthView() {
        calendarGrid.getChildren().clear();
        calendarGrid.getColumnConstraints().clear();
        calendarGrid.getRowConstraints().clear();

        calendarGrid.setHgap(0);
        calendarGrid.setVgap(0);
        calendarGrid.setPadding(Insets.EMPTY);
        calendarGrid.setStyle("-fx-background-color: transparent;");

        calendarMonthLabel.setText(currentCalendarDate.getMonth().getDisplayName(TextStyle.FULL, Locale.ITALIAN).toUpperCase() + " " + currentCalendarDate.getYear());

        for (int i = 0; i < 7; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(100.0 / 7);
            calendarGrid.getColumnConstraints().add(col);
        }

        RowConstraints headerRow = new RowConstraints();
        headerRow.setPrefHeight(30); headerRow.setVgrow(Priority.NEVER);
        calendarGrid.getRowConstraints().add(headerRow);

        for (int i = 0; i < 6; i++) {
            RowConstraints row = new RowConstraints();
            row.setMinHeight(120);
            row.setPrefHeight(Region.USE_COMPUTED_SIZE);
            row.setVgrow(Priority.ALWAYS);
            calendarGrid.getRowConstraints().add(row);
        }

        String[] days = {"LUN", "MAR", "MER", "GIO", "VEN", "SAB", "DOM"};
        for (int i = 0; i < 7; i++) {
            Label l = new Label(days[i]); l.setMaxWidth(Double.MAX_VALUE); l.setAlignment(Pos.CENTER);
            l.setStyle("-fx-text-fill: #F071A7; -fx-font-weight: bold; -fx-background-color: #1F162A; -fx-padding: 5;");
            calendarGrid.add(l, i, 0);
        }

        LocalDate firstOfMonth = currentCalendarDate.withDayOfMonth(1);
        int dayOfWeek = firstOfMonth.getDayOfWeek().getValue() - 1;
        int daysInMonth = currentCalendarDate.lengthOfMonth();
        LocalDate today = LocalDate.now();
        int row = 1; int col = dayOfWeek;

        Map<LocalDate, List<Tasks>> tasksMap = groupTasksByDate(sortedTasks);

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentCalendarDate.withDayOfMonth(day);
            boolean isToday = date.equals(today);

            AnchorPane cell = new AnchorPane();
            String style = isToday
                    ? "-fx-background-color: #362945; -fx-border-color: #F071A7; -fx-border-width: 2;"
                    : "-fx-background-color: #2b2236; -fx-border-color: #3F2E51; -fx-border-width: 0.5;";
            cell.setStyle(style);

            Label dayNum = new Label(String.valueOf(day));
            if(isToday) {
                dayNum.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; " +
                        "-fx-background-color: #F071A7; -fx-background-radius: 20; -fx-min-width: 22px; -fx-min-height: 22px; -fx-alignment: center;");
            } else {
                dayNum.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 5;");
            }
            AnchorPane.setTopAnchor(dayNum, 3.0);
            AnchorPane.setRightAnchor(dayNum, 3.0);

            VBox tasksContainer = new VBox(2);
            tasksContainer.setPadding(Insets.EMPTY);

            ScrollPane sp = new ScrollPane(tasksContainer);
            sp.setFitToWidth(true);
            sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            sp.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0;");
            sp.getStyleClass().add("thin-scroll");

            AnchorPane.setTopAnchor(sp, 2.0); AnchorPane.setLeftAnchor(sp, 2.0);
            AnchorPane.setBottomAnchor(sp, 2.0); AnchorPane.setRightAnchor(sp, 30.0);

            List<Tasks> dailyTasks = tasksMap.get(date);
            if (dailyTasks != null) {
                for (Tasks t : dailyTasks) {
                    tasksContainer.getChildren().add(createMicroTaskCard(t));
                }
            }

            cell.getChildren().addAll(sp, dayNum);
            calendarGrid.add(cell, col, row);

            col++; if (col > 6) { col = 0; row++; }
        }
    }

    /** Crea micro-card per la vista mensile. */
    private Node createMicroTaskCard(Tasks t) {
        HBox box = new HBox(4);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(2, 4, 2, 4));

        String pColor = getPriorityColor(t.getPriorita(), t.getCompletamento());
        String bg = t.getCompletamento() ? "rgba(100,100,100,0.2)" : "rgba(63, 46, 81, 0.95)";

        box.setStyle("-fx-background-color: " + bg + "; " +
                "-fx-background-radius: 3; " +
                "-fx-border-color: transparent transparent transparent " + pColor + "; " +
                "-fx-border-width: 0 0 0 3; " +
                "-fx-cursor: hand;");

        Text text = new Text(t.getTitolo());
        text.setFont(Font.font("System", FontWeight.BOLD, 12));
        if (t.getCompletamento()) {
            text.setFill(Color.web("#aaaaaa"));
            text.setStrikethrough(true);
        } else {
            text.setFill(Color.WHITE);
            text.setStrikethrough(false);
        }

        HBox.setHgrow(text, Priority.ALWAYS);
        Tooltip.install(box, new Tooltip(t.getTitolo()));

        box.getChildren().add(text);
        box.setOnMouseClicked(e -> { if(e.getClickCount() == 2 && onItemClick != null) onItemClick.accept(t); });

        return box;
    }

    /** Rendering vista settimanale. */
    private void renderWeekView() {
        weekViewBox.getChildren().clear();
        LocalDate startOfWeek = currentCalendarDate.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("d MMM");
        calendarMonthLabel.setText(startOfWeek.format(fmt) + " - " + endOfWeek.format(fmt));

        double containerWidth = weekViewContainer.getWidth();
        double colWidth = (containerWidth > 100) ? (containerWidth - 20) / 7 : 150;

        Map<LocalDate, List<Tasks>> tasksMap = groupTasksByDate(sortedTasks);

        for (int i = 0; i < 7; i++) {
            LocalDate date = startOfWeek.plusDays(i); boolean isToday = date.equals(LocalDate.now());

            VBox colHeader = new VBox(2); colHeader.setAlignment(Pos.CENTER); colHeader.setPadding(new Insets(10));
            colHeader.setStyle("-fx-background-color: " + (isToday ? "#F071A7" : "#2F223D") + "; -fx-background-radius: 8 8 0 0;");

            Label dayName = new Label(date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ITALIAN).toUpperCase());
            Label dayNum = new Label(String.valueOf(date.getDayOfMonth()));
            dayName.setStyle("-fx-text-fill: " + (isToday?"white":"#aaa") + "; -fx-font-size: 10px;");
            dayNum.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
            colHeader.getChildren().addAll(dayName, dayNum);

            VBox colBody = new VBox(5); colBody.setPadding(new Insets(5));
            colBody.setStyle("-fx-background-color: " + (isToday ? "rgba(240, 113, 167, 0.1)" : "#231a2e") + "; -fx-border-color: #3F2E51; -fx-border-width: 0 1 1 1;");
            VBox.setVgrow(colBody, Priority.ALWAYS); colBody.setMinHeight(300);

            List<Tasks> dailyTasks = tasksMap.get(date);
            if (dailyTasks != null) {
                for (Tasks t : dailyTasks) {
                    colBody.getChildren().add(createMiniTaskCard(t));
                }
            }

            VBox fullColumn = new VBox(0, colHeader, colBody); fullColumn.setMinWidth(colWidth); fullColumn.setPrefWidth(colWidth);
            HBox.setHgrow(fullColumn, Priority.ALWAYS); weekViewBox.getChildren().add(fullColumn);
        }
    }

    /** Rendering vista giornaliera. */
    private void renderDayView() {
        weekViewBox.getChildren().clear();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ITALIAN);
        calendarMonthLabel.setText(currentCalendarDate.format(fmt).toUpperCase());

        VBox dayContainer = new VBox(10);
        dayContainer.setPadding(new Insets(20));
        dayContainer.setStyle("-fx-background-color: #231a2e; -fx-background-radius: 10;");
        if(weekViewContainer.getWidth() > 40) {
            dayContainer.prefWidthProperty().bind(weekViewContainer.widthProperty().subtract(40));
        }

        Map<LocalDate, List<Tasks>> tasksMap = groupTasksByDate(sortedTasks);
        List<Tasks> dailyTasks = tasksMap.get(currentCalendarDate);

        if (dailyTasks == null || dailyTasks.isEmpty()) {
            Label empty = new Label("Nessuna attività per questo giorno.");
            empty.setStyle("-fx-text-fill: #666; -fx-font-size: 14px; -fx-padding: 20;");
            dayContainer.getChildren().add(empty);
        } else {
            for (Tasks t : dailyTasks) {
                dayContainer.getChildren().add(createDayCard(t));
            }
        }
        weekViewBox.getChildren().add(dayContainer);
    }

    /** Crea card per vista giornaliera. */
    private Node createDayCard(Tasks t) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10, 15, 10, 15));

        String pColor = getPriorityColor(t.getPriorita(), t.getCompletamento());
        String bg = t.getCompletamento() ? "rgba(100,100,100,0.2)" : "rgba(47, 34, 61, 0.9)";

        card.setStyle("-fx-background-color: " + bg + "; " +
                "-fx-background-radius: 8; " +
                "-fx-border-color: transparent transparent transparent " + pColor + "; " +
                "-fx-border-width: 0 0 0 6; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 5, 0, 0, 2); " +
                "-fx-cursor: hand;");

        VBox textContent = new VBox(4); textContent.setAlignment(Pos.CENTER_LEFT);

        Text title = new Text(t.getTitolo());
        title.setFont(Font.font("System", FontWeight.BOLD, 16));
        if(t.getCompletamento()) {
            title.setFill(Color.web("#aaaaaa"));
            title.setStrikethrough(true);
        } else {
            title.setFill(Color.WHITE);
            title.setStrikethrough(false);
        }
        textContent.getChildren().addAll(title);

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        card.getChildren().addAll(textContent, spacer);
        card.setOnMouseClicked(e -> { if(e.getClickCount() == 2 && onItemClick != null) onItemClick.accept(t); });

        return card;
    }

    /** Crea card per vista settimanale. */
    private Node createMiniTaskCard(Tasks t) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(5, 8, 5, 8));

        String pColor = getPriorityColor(t.getPriorita(), t.getCompletamento());
        String bg = t.getCompletamento() ? "rgba(100,100,100,0.2)" : "rgba(63, 46, 81, 0.95)";
        box.setStyle("-fx-background-color: " + bg + "; " +
                "-fx-background-radius: 4; " +
                "-fx-border-color: transparent transparent transparent " + pColor + "; " +
                "-fx-border-width: 0 0 0 5; " +
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 2, 0, 0, 1);");

        Text lbl = new Text(t.getTitolo());
        lbl.setFont(Font.font("System", FontWeight.BOLD, 13));
        if (t.getCompletamento()) {
            lbl.setFill(Color.web("#aaaaaa"));
            lbl.setStrikethrough(true);
        } else {
            lbl.setFill(Color.WHITE);
            lbl.setStrikethrough(false);
        }

        HBox.setHgrow(lbl, Priority.ALWAYS);
        Tooltip tp = new Tooltip(t.getTitolo()); Tooltip.install(box, tp);
        box.getChildren().add(lbl);
        box.setOnMouseClicked(e -> { if(e.getClickCount() == 2 && onItemClick != null) onItemClick.accept(t); });
        return box;
    }

    /**
     * Configura la CellFactory per la ListView principale.
     * Gestisce la visualizzazione completa di una riga task:
     * Checkbox, Titolo (sbarrato se completato), Badge priorità, Data scadenza, Menu azioni.
     */
    private void setupCellFactory() {
        taskListView.setCellFactory(param -> new ListCell<>() {
            private final ChangeListener<String> scadenzaListener = (obs, oldVal, newVal) -> { if(getItem()!=null) taskListView.refresh(); };
            private final ChangeListener<Boolean> completamentoListener = (obs, oldVal, newVal) -> { if(getItem()!=null) taskListView.refresh(); };

            // Creiamo il contenitore una volta sola per efficienza
            private final VBox rootContainer = new VBox(0);

            {
                rootContainer.setStyle("-fx-background-color: transparent;");
            }

            @Override
            protected void updateItem(Tasks task, boolean empty) {
                // Pulizia listener precedenti
                Tasks oldTask = getItem();
                if (oldTask != null) {
                    oldTask.scadenzaProperty().removeListener(scadenzaListener);
                    oldTask.completamentoProperty().removeListener(completamentoListener);
                }

                super.updateItem(task, empty);

                // --- Pulizia Grafica Fondamentale per evitare glitch ---
                rootContainer.getChildren().clear();
                setText(null);

                if (empty || task == null) {
                    setGraphic(null);
                    return;
                }

                // Ricollega listener
                task.scadenzaProperty().addListener(scadenzaListener);
                task.completamentoProperty().addListener(completamentoListener);

                // CheckBox
                CheckBox completeBox = new CheckBox();
                completeBox.setSelected(task.getCompletamento());
                completeBox.setOnAction(e -> {
                    boolean nuovoStato = completeBox.isSelected();
                    task.setCompletamento(nuovoStato);
                    if (nuovoStato) completionTimestamps.put(task.getIdTask(), System.currentTimeMillis());
                    else completionTimestamps.remove(task.getIdTask());
                    Platform.runLater(() -> {
                        Comparator<Tasks> cmp = (Comparator<Tasks>) sortedTasks.getComparator();
                        sortedTasks.setComparator(null); sortedTasks.setComparator(cmp);
                    });
                    new Thread(() -> { try { DAOTasks.getInstance().update(task); } catch (Exception ex) { ex.printStackTrace(); } }).start();
                });

                // Badge Priorità
                Label priorityBadge = new Label(task.getPriorita() != null ? task.getPriorita().trim() : "");
                String colore = getPriorityColor(task.getPriorita(), task.getCompletamento());
                priorityBadge.setStyle("-fx-text-fill:white; -fx-background-color:" + colore + "; -fx-padding: 5 12; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 12px;");
                priorityBadge.setMinWidth(Region.USE_PREF_SIZE);

                // Titolo
                Text textLabel = new Text(task.getTitolo());
                textLabel.setFont(Font.font("System", 15));
                if (task.getCompletamento()) {
                    textLabel.setFill(Color.web("#aaaaaa"));
                    textLabel.setStrikethrough(true);
                } else {
                    textLabel.setFill(Color.WHITE);
                    textLabel.setStrikethrough(false);
                }

                // Data
                Label dateLabel = new Label();
                if (!task.getCompletamento() && task.getScadenza() != null && !task.getScadenza().isEmpty() && !task.getScadenza().equalsIgnoreCase("null")) {
                    try {
                        LocalDate scad = smartParse(task.getScadenza());
                        if (scad != null) {
                            LocalDate oggi = LocalDate.now();
                            if (scad.isBefore(oggi)) { dateLabel.setText("⌛ SCADUTA"); dateLabel.setStyle("-fx-text-fill: #FF5555; -fx-font-weight: bold; -fx-font-size: 12px;"); }
                            else if (scad.isEqual(oggi)) { dateLabel.setText("🔥 OGGI"); dateLabel.setStyle("-fx-text-fill: #FFB86C; -fx-font-weight: bold; -fx-font-size: 12px;"); }
                            else if (scad.isEqual(oggi.plusDays(1))) { dateLabel.setText("⏳ DOMANI"); dateLabel.setStyle("-fx-text-fill: #8BE9FD; -fx-font-weight: bold; -fx-font-size: 12px;"); }
                            else { dateLabel.setText("📅 " + DateUtil.format(scad)); dateLabel.setStyle("-fx-text-fill: #bd93f9; -fx-font-size: 10px;"); }
                        }
                    } catch (Exception e) {}
                }
                dateLabel.setMinWidth(Region.USE_PREF_SIZE);

                // Menu
                MenuItem editItem = new MenuItem("Modifica"); MenuItem deleteItem = new MenuItem("Elimina");
                editItem.setOnAction(e -> onEditRequest.accept(task)); deleteItem.setOnAction(e -> onDeleteRequest.accept(task));
                MenuButton menuButton = new MenuButton("⋮", null, editItem, deleteItem);
                menuButton.getStyleClass().add("task-menu-button");
                menuButton.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 16px;");

                // Layout Riga
                Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
                HBox taskContent = new HBox(15, completeBox, priorityBadge, textLabel, spacer, dateLabel, menuButton);
                taskContent.setAlignment(Pos.CENTER_LEFT); taskContent.setPadding(new Insets(5, 0, 5, 0));
                taskContent.setOpacity(task.getCompletamento() ? 0.5 : 1.0);

                // Separatore (COMPLETATE)
                boolean showSeparator = false;
                if (task.getCompletamento()) {
                    int index = getIndex();
                    if (index == 0) showSeparator = true;
                    else {
                        // Safe check su lista filtrata corrente
                        ObservableList<Tasks> currentList = getListView().getItems();
                        if (currentList != null && index > 0 && index < currentList.size()) {
                            Tasks prevTask = currentList.get(index - 1);
                            if (!prevTask.getCompletamento()) showSeparator = true;
                        }
                    }
                }

                if (showSeparator) {
                    HBox separatorBox = new HBox(); separatorBox.setAlignment(Pos.CENTER_LEFT); separatorBox.setPadding(new Insets(20, 0, 10, 0));
                    Label sepLabel = new Label("COMPLETATE"); sepLabel.setStyle("-fx-text-fill: #f071a7; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 0 10 0 0;");
                    Separator line = new Separator(); HBox.setHgrow(line, Priority.ALWAYS); line.setStyle("-fx-opacity: 0.3;");
                    separatorBox.getChildren().addAll(sepLabel, line);
                    rootContainer.getChildren().add(separatorBox);
                }

                rootContainer.getChildren().add(taskContent);
                setGraphic(rootContainer);
            }
        });
    }
}