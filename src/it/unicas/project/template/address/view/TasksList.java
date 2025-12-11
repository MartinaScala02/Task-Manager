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
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class TasksList {

    public ObservableList<Tasks> getTasks() {
        return tasks;
    }

    public enum ViewMode { LIST, GRID, CALENDAR }
    public enum CalendarMode { MONTH, WEEK, DAY }

    private final ListView<Tasks> taskListView;
    private final ScrollPane gridViewContainer;
    private final FlowPane gridFlowPane;

    private final BorderPane calendarViewContainer;
    private final GridPane calendarGrid;
    private final ScrollPane weekViewContainer;
    private final HBox weekViewBox;
    private final Label calendarMonthLabel;

    private final MainApp mainApp;
    private final Consumer<Tasks> onEditRequest;
    private final Consumer<Tasks> onDeleteRequest;
    private final Consumer<Tasks> onItemClick;

    // DATA
    private ObservableList<Tasks> tasks;
    private SortedList<Tasks> sortedTasks;
    private FilteredList<Tasks> filteredTasks;

    // STATE FILTRI
    private Categorie filterCategory = null;
    private Boolean filterStatus = null;
    private String filterPriority = null;
    private LocalDate filterDate = null;
    private String filterKeyword = null;

    private ViewMode currentViewMode = ViewMode.LIST;
    private CalendarMode currentCalendarMode = CalendarMode.MONTH;
    private LocalDate currentCalendarDate = LocalDate.now();

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

    private void init() {
        tasks = FXCollections.observableArrayList();
        filteredTasks = new FilteredList<>(tasks, t -> true);
        sortedTasks = new SortedList<>(filteredTasks);

        sortedTasks.setComparator((t1, t2) -> {
            if (t1.getCompletamento() != t2.getCompletamento()) return t1.getCompletamento() ? 1 : -1;
            String d1 = t1.getScadenza() == null ? "9999-12-31" : t1.getScadenza();
            String d2 = t2.getScadenza() == null ? "9999-12-31" : t2.getScadenza();
            return d1.compareTo(d2);
        });

        taskListView.setItems(sortedTasks);
        setupCellFactory();
        if (gridViewContainer != null) {
            gridViewContainer.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        }

        gridFlowPane.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (currentViewMode == ViewMode.GRID) resizeGridCards(newVal.doubleValue());
        });

        if(weekViewContainer != null) {
            weekViewContainer.widthProperty().addListener((obs, o, n) -> {
                if(currentViewMode == ViewMode.CALENDAR && currentCalendarMode == CalendarMode.WEEK) refreshView();
            });
        }
    }

    private LocalDate smartParse(String dateString) {
        if (dateString == null || dateString.isEmpty()) return null;
        try {
            return LocalDate.parse(dateString); // Standard ISO
        } catch (Exception e) {
            return DateUtil.parse(dateString); // Formato Italiano
        }
    }

    public void loadTasks(Integer userId) {
        reloadTasksFromDB();
    }

    private void reloadTasksFromDB() {
        if (MainApp.getCurrentUser() == null) return;

        Thread dbThread = new Thread(() -> {
            try {
                Tasks filterTemplate = new Tasks();
                filterTemplate.setIdUtente(MainApp.getCurrentUser().getIdUtente());

                // Passiamo al DB solo i filtri sicuri (non la data!)
                if (filterCategory != null) filterTemplate.setIdCategoria(filterCategory.getIdCategoria());
                if (filterStatus != null) filterTemplate.setCompletamento(filterStatus);
                if (filterPriority != null && !filterPriority.equalsIgnoreCase("TUTTE")) filterTemplate.setPriorita(filterPriority);
                // NOTA: Non settiamo la scadenza qui! La filtriamo noi in Java per gestire i formati misti.
                // if (filterDate != null) filterTemplate.setScadenza(filterDate.toString());
                if (filterKeyword != null && !filterKeyword.isEmpty()) filterTemplate.setTitolo(filterKeyword);

                List<Tasks> results = DAOTasks.getInstance().select(filterTemplate);

                Platform.runLater(() -> {
                    tasks.setAll(results);
                    // Rilanciamo il filtro locale per applicare la data
                    applyFilters();
                    taskListView.refresh();
                    if (currentViewMode != ViewMode.LIST) refreshView();
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

    public void setFilterKeyword(String keyword) {
        this.filterKeyword = (keyword != null) ? keyword.trim() : null;
        reloadTasksFromDB();
    }

    public void addTask(Tasks t) { tasks.add(0, t); refreshView(); }

    public void updateTaskInList(Tasks t) {
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getIdTask().equals(t.getIdTask())) {
                tasks.set(i, t);
                break;
            }
        }
        refreshView();
    }

    public void removeTask(Tasks t) { tasks.remove(t); refreshView(); }

    public void switchView(ViewMode mode) {
        this.currentViewMode = mode;
        taskListView.setVisible(mode == ViewMode.LIST); taskListView.setManaged(mode == ViewMode.LIST);
        gridViewContainer.setVisible(mode == ViewMode.GRID); gridViewContainer.setManaged(mode == ViewMode.GRID);
        calendarViewContainer.setVisible(mode == ViewMode.CALENDAR); calendarViewContainer.setManaged(mode == ViewMode.CALENDAR);
        refreshView();
    }

    public ViewMode getCurrentViewMode() { return currentViewMode; }

    public void setCalendarMode(CalendarMode mode) {
        this.currentCalendarMode = mode;
        refreshView();
    }

    public void calendarForward() {
        switch(currentCalendarMode) {
            case MONTH -> currentCalendarDate = currentCalendarDate.plusMonths(1);
            case WEEK -> currentCalendarDate = currentCalendarDate.plusWeeks(1);
            case DAY -> currentCalendarDate = currentCalendarDate.plusDays(1);
        }
        refreshView();
    }

    public void calendarBack() {
        switch(currentCalendarMode) {
            case MONTH -> currentCalendarDate = currentCalendarDate.minusMonths(1);
            case WEEK -> currentCalendarDate = currentCalendarDate.minusWeeks(1);
            case DAY -> currentCalendarDate = currentCalendarDate.minusDays(1);
        }
        refreshView();
    }

    private void refreshView() {
        applyFilters(); // Assicuriamoci che i filtri siano applicati
        Platform.runLater(() -> {
            if (currentViewMode == ViewMode.GRID) renderGrid();
            else if (currentViewMode == ViewMode.CALENDAR) renderCalendarDispatcher();
            else taskListView.refresh();
        });
    }

    public void setFilterCategory(Categorie c) { this.filterCategory = c; reloadTasksFromDB(); }
    public void setFilterStatus(Boolean s) { this.filterStatus = s; reloadTasksFromDB(); }
    public void setFilterPriority(String p) { this.filterPriority = p; reloadTasksFromDB(); }

    // Quando settiamo la data, non serve ricaricare dal DB (abbiamo tolto il filtro lì),
    // basta riapplicare il filtro locale. È molto più veloce!
    public void setFilterDate(LocalDate d) {
        this.filterDate = d;
        if (d != null && currentViewMode == ViewMode.CALENDAR) currentCalendarDate = d;
        applyFilters();
        refreshView();
    }

    public void clearFilters() {
        this.filterCategory = null; this.filterStatus = null;
        this.filterPriority = null; this.filterDate = null; this.filterKeyword = null;
        reloadTasksFromDB();
    }

    // --- FILTRO CLIENT-SIDE POTENZIATO (Gestisce date miste) ---
    private void applyFilters() {
        filteredTasks.setPredicate(task -> {
            // Filtri già applicati dal DB (ridondanti ma sicuri)
            boolean catMatch = (filterCategory == null) || (task.getIdCategoria() != null && task.getIdCategoria().equals(filterCategory.getIdCategoria()));
            boolean statMatch = (filterStatus == null) || (task.getCompletamento() == filterStatus);
            boolean prioMatch = true;
            if (filterPriority != null && !filterPriority.isEmpty() && !filterPriority.equalsIgnoreCase("TUTTE")) {
                prioMatch = task.getPriorita() != null && task.getPriorita().equalsIgnoreCase(filterPriority);
            }

            // FILTRO DATA INTELLIGENTE
            boolean dateMatch = true;
            if (filterDate != null) {
                if (task.getScadenza() == null || task.getScadenza().isEmpty()) {
                    dateMatch = false;
                } else {
                    LocalDate tDate = smartParse(task.getScadenza());
                    dateMatch = (tDate != null && tDate.isEqual(filterDate));
                }
            }

            boolean keyMatch = (filterKeyword == null || filterKeyword.isEmpty()) || (task.getTitolo().toLowerCase().contains(filterKeyword.toLowerCase()));

            return catMatch && statMatch && prioMatch && dateMatch && keyMatch;
        });
    }

    private void renderGrid() {
        gridFlowPane.getChildren().clear();
        for (Tasks t : sortedTasks) {
            gridFlowPane.getChildren().add(createGridCard(t));
        }
        resizeGridCards(gridFlowPane.getWidth());
    }

    private void resizeGridCards(double containerWidth) {
        if (containerWidth <= 0) return;
        double gap = gridFlowPane.getHgap();
        double padding = 60;
        double availableWidth = containerWidth - padding;
        double minCardWidth = 240.0;
        int columns = (int) Math.max(1, Math.floor((availableWidth + gap) / (minCardWidth + gap)));
        double newWidth = (availableWidth - (gap * (columns - 1))) / columns;
        for (Node node : gridFlowPane.getChildren()) {
            if (node instanceof VBox) ((VBox) node).setPrefWidth(newWidth);
        }
    }

    private VBox createGridCard(Tasks task) {
        VBox scheda = new VBox(8);
        scheda.getStyleClass().add("scheda-task");
        scheda.setStyle("-fx-background-color: #2F223D; -fx-background-radius: 10; -fx-padding: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 8, 0, 0, 2);");
        scheda.setPrefHeight(140);
        scheda.setMinHeight(140);
        scheda.setMaxHeight(140);

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Circle dot = new Circle(5);
        String colorHex = switch (task.getPriorita() != null ? task.getPriorita() : "") {
            case "ALTA" -> "#FF5555"; case "MEDIA" -> "#FFB86C"; default -> "#50fa7b";
        };
        dot.setFill(Color.web(colorHex));
        Label lblTitolo = new Label(task.getTitolo());
        lblTitolo.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px;");
        lblTitolo.setWrapText(false);
        lblTitolo.setTextOverrun(OverrunStyle.ELLIPSIS);
        header.getChildren().addAll(dot, lblTitolo);

        String descText = (task.getDescrizione() != null) ? task.getDescrizione() : "Nessuna descrizione";
        Label lblDesc = new Label(descText);
        lblDesc.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 11px;");
        lblDesc.setWrapText(true);
        lblDesc.setAlignment(Pos.TOP_LEFT);
        lblDesc.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(lblDesc, Priority.ALWAYS);

        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_LEFT);
        Label lblData = new Label();
        String dateText = "";
        String dateStyle = "-fx-text-fill: #666; -fx-font-size: 10px;";

        if (task.getScadenza() != null && !task.getScadenza().isEmpty() && !task.getCompletamento()) {
            try {
                LocalDate scad = smartParse(task.getScadenza());
                if (scad != null) {
                    LocalDate oggi = LocalDate.now();
                    if (scad.isBefore(oggi)) {
                        dateText = "⚠ SCADUTA IL " + DateUtil.format(scad);
                        dateStyle = "-fx-text-fill: #FF5555; -fx-font-weight: bold; -fx-font-size: 10px;";
                    } else if (scad.isEqual(oggi)) {
                        dateText = "🔥 SCADE OGGI";
                        dateStyle = "-fx-text-fill: #FFB86C; -fx-font-weight: bold; -fx-font-size: 10px;";
                    } else if (scad.isEqual(oggi.plusDays(1))) {
                        dateText = "⏳ SCADE DOMANI";
                        dateStyle = "-fx-text-fill: #8BE9FD; -fx-font-weight: bold; -fx-font-size: 10px;";
                    } else {
                        dateText = "📅 " + DateUtil.format(scad);
                        dateStyle = "-fx-text-fill: #bd93f9; -fx-font-size: 10px;";
                    }
                } else {
                    dateText = task.getScadenza();
                }
            } catch (Exception e) { dateText = task.getScadenza(); }
        }
        lblData.setText(dateText);
        lblData.setStyle(dateStyle);
        footer.getChildren().add(lblData);
        scheda.getChildren().addAll(header, lblDesc, footer);
        if (Boolean.TRUE.equals(task.getCompletamento())) {
            scheda.setOpacity(0.5);
            scheda.setStyle(scheda.getStyle() + "-fx-border-color: #6c6c6c; -fx-border-width: 1;");
            lblTitolo.setStyle(lblTitolo.getStyle() + "-fx-strikethrough: true;");
        }
        scheda.setOnMouseClicked(e -> { if (e.getClickCount() == 2 && onItemClick != null) onItemClick.accept(task); });
        return scheda;
    }

    private void renderCalendarDispatcher() {
        if (currentCalendarMode == CalendarMode.MONTH) {
            calendarGrid.setVisible(true); calendarGrid.setManaged(true);
            weekViewContainer.setVisible(false); weekViewContainer.setManaged(false);
            renderMonthView();
        } else {
            calendarGrid.setVisible(false); calendarGrid.setManaged(false);
            weekViewContainer.setVisible(true); weekViewContainer.setManaged(true);
            if (currentCalendarMode == CalendarMode.WEEK) renderWeekView();
            else renderDayView();
        }
    }

    private void renderMonthView() {
        calendarGrid.getChildren().clear();
        calendarGrid.getColumnConstraints().clear();
        calendarGrid.getRowConstraints().clear();
        calendarMonthLabel.setText(currentCalendarDate.getMonth().getDisplayName(TextStyle.FULL, Locale.ITALIAN).toUpperCase() + " " + currentCalendarDate.getYear());

        for (int i = 0; i < 7; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(100.0 / 7);
            calendarGrid.getColumnConstraints().add(col);
        }
        RowConstraints headerRow = new RowConstraints();
        headerRow.setPrefHeight(30); headerRow.setVgrow(Priority.NEVER); calendarGrid.getRowConstraints().add(headerRow);
        for (int i = 0; i < 6; i++) {
            RowConstraints row = new RowConstraints(); row.setVgrow(Priority.ALWAYS); calendarGrid.getRowConstraints().add(row);
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

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentCalendarDate.withDayOfMonth(day);
            BorderPane cell = new BorderPane();
            String style = "-fx-background-color: #2b2236; -fx-border-color: #3F2E51; -fx-border-width: 0.5;";
            if (date.equals(today)) style = "-fx-background-color: #362945; -fx-border-color: #F071A7; -fx-border-width: 2;";
            cell.setStyle(style);

            Label dayNum = new Label(String.valueOf(day));
            dayNum.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 5;");
            BorderPane.setAlignment(dayNum, Pos.TOP_RIGHT);
            cell.setTop(dayNum);

            VBox tasksContainer = new VBox(2); tasksContainer.setPadding(new Insets(2));
            ScrollPane sp = new ScrollPane(tasksContainer); sp.setFitToWidth(true); sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED); sp.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0;");

            for (Tasks t : sortedTasks) {
                if (t.getScadenza() != null && !t.getScadenza().isEmpty()) {
                    LocalDate taskDate = smartParse(t.getScadenza());
                    if (taskDate != null && taskDate.isEqual(date)) {
                        tasksContainer.getChildren().add(createMiniTaskCard(t));
                    }
                }
            }
            cell.setCenter(sp);
            calendarGrid.add(cell, col, row);

            col++; if (col > 6) { col = 0; row++; }
        }
    }

    private void renderWeekView() {
        weekViewBox.getChildren().clear();
        LocalDate startOfWeek = currentCalendarDate.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("d MMM");
        calendarMonthLabel.setText(startOfWeek.format(fmt) + " - " + endOfWeek.format(fmt));
        double containerWidth = weekViewContainer.getWidth();
        double colWidth = (containerWidth > 100) ? (containerWidth - 20) / 7 : 150;
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
            for (Tasks t : sortedTasks) {
                if (t.getScadenza() != null && !t.getScadenza().isEmpty()) {
                    LocalDate taskDate = smartParse(t.getScadenza());
                    if (taskDate != null && taskDate.isEqual(date)) {
                        colBody.getChildren().add(createMiniTaskCard(t));
                    }
                }
            }
            VBox fullColumn = new VBox(0, colHeader, colBody); fullColumn.setMinWidth(colWidth); fullColumn.setPrefWidth(colWidth);
            HBox.setHgrow(fullColumn, Priority.ALWAYS); weekViewBox.getChildren().add(fullColumn);
        }
    }

    private void renderDayView() {
        weekViewBox.getChildren().clear();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ITALIAN);
        calendarMonthLabel.setText(currentCalendarDate.format(fmt).toUpperCase());
        VBox dayContainer = new VBox(10); dayContainer.setPadding(new Insets(20)); dayContainer.setStyle("-fx-background-color: #231a2e; -fx-background-radius: 10;"); dayContainer.prefWidthProperty().bind(weekViewContainer.widthProperty().subtract(40));

        List<Tasks> dailyTasks = sortedTasks.stream().filter(t -> {
            if (t.getScadenza() == null || t.getScadenza().isEmpty()) return false;
            LocalDate taskDate = smartParse(t.getScadenza());
            return taskDate != null && taskDate.isEqual(currentCalendarDate);
        }).collect(Collectors.toList());

        if (dailyTasks.isEmpty()) { Label empty = new Label("Nessuna attività per questo giorno."); empty.setStyle("-fx-text-fill: #666; -fx-font-size: 14px; -fx-padding: 20;"); dayContainer.getChildren().add(empty); }
        else { for (Tasks t : dailyTasks) dayContainer.getChildren().add(createGridCard(t)); }
        weekViewBox.getChildren().add(dayContainer);
    }

    private Node createMiniTaskCard(Tasks t) {
        HBox box = new HBox(5); box.setAlignment(Pos.CENTER_LEFT); box.setPadding(new Insets(3, 6, 3, 6));
        String pColor = switch(t.getPriorita() != null ? t.getPriorita() : "") { case "ALTA" -> "#FF5555"; case "MEDIA" -> "#FFB86C"; default -> "#50fa7b"; };
        String bg = t.getCompletamento() ? "rgba(100,100,100,0.2)" : "rgba(63, 46, 81, 0.9)";
        box.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 4; -fx-border-color: transparent transparent transparent " + pColor + "; -fx-border-width: 0 0 0 4; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 2, 0, 0, 1);");
        Label lbl = new Label(t.getTitolo()); lbl.setStyle("-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;");
        if (t.getCompletamento()) lbl.setStyle("-fx-text-fill: #aaa; -fx-font-size: 11px; -fx-strikethrough: true;");
        lbl.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(lbl, Priority.ALWAYS);
        Tooltip tp = new Tooltip(t.getTitolo()); Tooltip.install(box, tp);
        box.getChildren().add(lbl); box.setOnMouseClicked(e -> { if(e.getClickCount() == 2 && onItemClick != null) onItemClick.accept(t); });
        return box;
    }

    public String getCategoryName(Integer id, List<Categorie> allCats) {
        if (id == null || allCats == null) return "";
        return allCats.stream().filter(c -> c.getIdCategoria().equals(id)).findFirst().map(Categorie::getNomeCategoria).orElse("");
    }

    private void setupCellFactory() {
        taskListView.setCellFactory(param -> new ListCell<>() {

            // Listener Semplificato che chiama il refresh dell'intera lista
            // Questo evita glitch grafici quando si resettano i filtri
            private final ChangeListener<String> scadenzaListener = (obs, oldVal, newVal) -> {
                taskListView.refresh();
            };
            private final ChangeListener<Boolean> completamentoListener = (obs, oldVal, newVal) -> {
                taskListView.refresh();
            };

            @Override
            protected void updateItem(Tasks task, boolean empty) {
                Tasks oldTask = getItem();
                if (oldTask != null) {
                    oldTask.scadenzaProperty().removeListener(scadenzaListener);
                    oldTask.completamentoProperty().removeListener(completamentoListener);
                }

                super.updateItem(task, empty);

                if (empty || task == null) {
                    setText(null); setGraphic(null); return;
                }

                task.scadenzaProperty().addListener(scadenzaListener);
                task.completamentoProperty().addListener(completamentoListener);


                // Dentro setupCellFactory -> updateItem...

                CheckBox completeBox = new CheckBox();
                completeBox.setSelected(task.getCompletamento());

                completeBox.setOnAction(e -> {
                    boolean nuovoStato = completeBox.isSelected();
                    task.setCompletamento(nuovoStato);

                    Platform.runLater(() -> {
                        // Forza la sortedList a rifare l'ordinamento dopo 1 frame
                        tasks.set(tasks.indexOf(task), task);
                    });

                    new Thread(() -> {
                        try { DAOTasks.getInstance().update(task); }
                        catch (Exception ex) { ex.printStackTrace(); }
                    }).start();
                });


                Label priorityBadge = new Label(task.getPriorita() != null ? task.getPriorita().trim() : "");
                String colore = switch (priorityBadge.getText().toUpperCase()) {
                    case "ALTA" -> "#e74c3c"; case "MEDIA" -> "#f39c12"; default -> "#27ae60";
                };
                priorityBadge.setStyle("-fx-text-fill:white; -fx-background-color:" + colore + "; -fx-padding: 5 12; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 12px;");
                priorityBadge.setMinWidth(Region.USE_PREF_SIZE); priorityBadge.setMinHeight(Region.USE_PREF_SIZE);

                Label textLabel = new Label(task.getTitolo());
                textLabel.setStyle(task.getCompletamento() ? "-fx-text-fill: #aaa; -fx-strikethrough: true; -fx-font-size: 15px;" : "-fx-text-fill: white; -fx-font-size: 15px;");
                textLabel.setMaxWidth(Double.MAX_VALUE);

                // Cerca questo blocco dentro setupCellFactory e sostituiscilo con questo:

                Label dateLabel = new Label();
                dateLabel.setText(""); // Partiamo con testo vuoto di default

// Controlliamo che il task non sia completato E che la data esista e non sia la stringa "null"
                if (!task.getCompletamento() &&
                        task.getScadenza() != null &&
                        !task.getScadenza().isEmpty() &&
                        !task.getScadenza().equalsIgnoreCase("null")) {

                    try {
                        LocalDate scad = smartParse(task.getScadenza());
                        if (scad != null) {
                            LocalDate oggi = LocalDate.now();
                            if (scad.isBefore(oggi)) {
                                dateLabel.setText("⌛ SCADUTA");
                                dateLabel.setStyle("-fx-text-fill: #FF5555; -fx-font-weight: bold; -fx-font-size: 12px;");
                            }
                            else if (scad.isEqual(oggi)) {
                                dateLabel.setText("🔥 OGGI");
                                dateLabel.setStyle("-fx-text-fill: #FFB86C; -fx-font-weight: bold; -fx-font-size: 12px;");
                            }
                            else if (scad.isEqual(oggi.plusDays(1))) {
                                dateLabel.setText("⏳ DOMANI");
                                dateLabel.setStyle("-fx-text-fill: #8BE9FD; -fx-font-weight: bold; -fx-font-size: 12px;");
                            }
                            else {
                                dateLabel.setText("📅 " + DateUtil.format(scad));
                                dateLabel.setStyle("-fx-text-fill: #bd93f9; -fx-font-size: 10px;");
                            }
                        }
                        // Se scad è null (parsing fallito), non facciamo nulla (il testo resta vuoto)
                    } catch (Exception e) {
                        // In caso di errore, non mostriamo nulla invece di mostrare errori strani
                        dateLabel.setText("");
                    }
                }
                dateLabel.setMinWidth(Region.USE_PREF_SIZE);

                MenuItem editItem = new MenuItem("Modifica");
                MenuItem deleteItem = new MenuItem("Elimina");
                editItem.setOnAction(e -> onEditRequest.accept(task));
                deleteItem.setOnAction(e -> onDeleteRequest.accept(task));
                MenuButton menuButton = new MenuButton("⋮", null, editItem, deleteItem);
                menuButton.getStyleClass().add("task-menu-button");
                menuButton.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 16px;");

                Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
                HBox taskContent = new HBox(15, completeBox, priorityBadge, textLabel, spacer, dateLabel, menuButton);
                taskContent.setAlignment(Pos.CENTER_LEFT); taskContent.setPadding(new Insets(5, 0, 5, 0));
                taskContent.setOpacity(task.getCompletamento() ? 0.5 : 1.0);

                VBox rootContainer = new VBox(0);
                rootContainer.setStyle("-fx-background-color: transparent;");
                boolean showSeparator = false;
                if (task.getCompletamento()) {
                    int index = getIndex();
                    if (index == 0) showSeparator = true;
                    else { if (index > 0 && index < getListView().getItems().size()) { Tasks prevTask = getListView().getItems().get(index - 1); if (!prevTask.getCompletamento()) showSeparator = true; } }
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
                setText(null);
            }
        });
    }


}