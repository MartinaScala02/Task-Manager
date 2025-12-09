package it.unicas.project.template.address.view;

import it.unicas.project.template.address.model.Tasks;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class CalendarViewHelper {

    public enum Mode { MONTH, WEEK, DAY }

    private final GridPane calendarGrid;        // Griglia Mese
    private final ScrollPane weekViewContainer; // Scroll Settimana
    private final HBox weekViewBox;             // Contenitore colonne Settimana
    private final Label calendarLabel;          // Titolo periodo
    private final Consumer<Tasks> onItemClick;  // Azione click

    private Mode currentMode = Mode.MONTH;
    private LocalDate currentDate = LocalDate.now();
    private List<Tasks> currentTasks; // I task da mostrare (già filtrati)

    public CalendarViewHelper(GridPane calendarGrid, ScrollPane weekViewContainer,
                              HBox weekViewBox, Label calendarLabel, Consumer<Tasks> onItemClick) {
        this.calendarGrid = calendarGrid;
        this.weekViewContainer = weekViewContainer;
        this.weekViewBox = weekViewBox;
        this.calendarLabel = calendarLabel;
        this.onItemClick = onItemClick;

        // Listener responsive per la settimana
        weekViewContainer.widthProperty().addListener((obs, o, n) -> {
            if (currentMode == Mode.WEEK) renderWeek();
        });
    }

    // --- API PUBBLICHE ---

    public void setMode(Mode mode) {
        this.currentMode = mode;
        refresh();
    }

    public void setData(List<Tasks> tasks) {
        this.currentTasks = tasks;
        refresh();
    }

    public void next() {
        switch (currentMode) {
            case MONTH -> currentDate = currentDate.plusMonths(1);
            case WEEK -> currentDate = currentDate.plusWeeks(1);
            case DAY -> currentDate = currentDate.plusDays(1);
        }
        refresh();
    }

    public void prev() {
        switch (currentMode) {
            case MONTH -> currentDate = currentDate.minusMonths(1);
            case WEEK -> currentDate = currentDate.minusWeeks(1);
            case DAY -> currentDate = currentDate.minusDays(1);
        }
        refresh();
    }

    public void refresh() {
        if (currentTasks == null) return;

        // Gestione visibilità contenitori
        boolean isMonth = (currentMode == Mode.MONTH);
        calendarGrid.setVisible(isMonth); calendarGrid.setManaged(isMonth);
        weekViewContainer.setVisible(!isMonth); weekViewContainer.setManaged(!isMonth);

        switch (currentMode) {
            case MONTH -> renderMonth();
            case WEEK -> renderWeek();
            case DAY -> renderDay();
        }
    }

    // --- RENDERERS ---

    private void renderMonth() {
        calendarGrid.getChildren().clear();
        YearMonth ym = YearMonth.from(currentDate);
        calendarLabel.setText(ym.getMonth().getDisplayName(TextStyle.FULL, Locale.ITALIAN).toUpperCase() + " " + ym.getYear());

        // Header Giorni
        String[] days = {"LUN", "MAR", "MER", "GIO", "VEN", "SAB", "DOM"};
        for(int i=0; i<7; i++) {
            Label l = new Label(days[i]);
            l.setMaxWidth(Double.MAX_VALUE); l.setAlignment(Pos.CENTER);
            l.setStyle("-fx-text-fill: #F071A7; -fx-font-weight: bold; -fx-background-color: #1F162A; -fx-padding: 5;");
            calendarGrid.add(l, i, 0);
        }

        LocalDate firstOfMonth = ym.atDay(1);
        int dayOfWeek = firstOfMonth.getDayOfWeek().getValue() - 1;
        int daysInMonth = ym.lengthOfMonth();
        int row = 1; int col = dayOfWeek;

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = ym.atDay(day);
            VBox cellContent = new VBox(2);
            ScrollPane sp = new ScrollPane(cellContent);
            sp.setFitToWidth(true); sp.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

            BorderPane cell = new BorderPane();
            cell.setCenter(sp);

            // Stile Cella
            boolean isToday = date.equals(LocalDate.now());
            cell.setStyle("-fx-background-color: " + (isToday ? "#362945" : "#2b2236") + "; -fx-border-color: #3F2E51;");

            Label dayNum = new Label(String.valueOf(day));
            dayNum.setStyle("-fx-text-fill: #aaa; -fx-padding: 2; -fx-font-size: 11px;");
            cell.setTop(dayNum);
            BorderPane.setAlignment(dayNum, Pos.TOP_RIGHT);

            // Filtra task per questo giorno
            for(Tasks t : currentTasks) {
                if(t.getScadenza() != null && t.getScadenza().equals(date.toString())) {
                    cellContent.getChildren().add(createMiniTaskCard(t));
                }
            }

            calendarGrid.add(cell, col, row);
            col++;
            if (col > 6) { col = 0; row++; }
        }
    }

    private void renderWeek() {
        weekViewBox.getChildren().clear();
        LocalDate startOfWeek = currentDate.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("d MMM");
        calendarLabel.setText(startOfWeek.format(fmt) + " - " + endOfWeek.format(fmt));

        double colWidth = (weekViewContainer.getWidth() - 20) / 7;
        if (colWidth < 120) colWidth = 120;

        for (int i = 0; i < 7; i++) {
            LocalDate date = startOfWeek.plusDays(i);
            boolean isToday = date.equals(LocalDate.now());

            VBox col = new VBox(5);
            col.setMinWidth(colWidth); col.setPrefWidth(colWidth);
            HBox.setHgrow(col, Priority.ALWAYS);
            col.setStyle("-fx-border-color: #3F2E51; -fx-border-width: 0 1 0 0; -fx-padding: 5;");
            if(isToday) col.setStyle("-fx-background-color: rgba(240, 113, 167, 0.05); -fx-border-color: #F071A7;");

            Label head = new Label(date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ITALIAN).toUpperCase() + " " + date.getDayOfMonth());
            head.setStyle("-fx-text-fill: " + (isToday ? "#F071A7" : "white") + "; -fx-font-weight: bold;");
            col.getChildren().add(head);

            for(Tasks t : currentTasks) {
                if(t.getScadenza() != null && t.getScadenza().equals(date.toString())) {
                    col.getChildren().add(createMiniTaskCard(t));
                }
            }
            weekViewBox.getChildren().add(col);
        }
    }

    private void renderDay() {
        weekViewBox.getChildren().clear();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ITALIAN);
        calendarLabel.setText(currentDate.format(fmt).toUpperCase());

        VBox dayContainer = new VBox(10);
        dayContainer.setPadding(new Insets(20));
        dayContainer.prefWidthProperty().bind(weekViewContainer.widthProperty().subtract(20));

        List<Tasks> dailyTasks = currentTasks.stream()
                .filter(t -> t.getScadenza() != null && t.getScadenza().equals(currentDate.toString()))
                .collect(Collectors.toList());

        if (dailyTasks.isEmpty()) {
            dayContainer.getChildren().add(new Label("Nessun impegno per oggi."));
        } else {
            // Qui potresti riusare la logica delle schede grandi se vuoi
            for (Tasks t : dailyTasks) dayContainer.getChildren().add(createMiniTaskCard(t));
        }
        weekViewBox.getChildren().add(dayContainer);
    }

    // Piccola scheda per il calendario
    private Node createMiniTaskCard(Tasks t) {
        HBox box = new HBox(5);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(3));
        box.setStyle("-fx-background-color: #2F223D; -fx-background-radius: 4; -fx-cursor: hand; -fx-border-color: #444; -fx-border-radius: 4;");

        if (t.getCompletamento()) box.setOpacity(0.5);

        String color = switch (t.getPriorita() != null ? t.getPriorita() : "") {
            case "ALTA" -> "#FF5555"; case "MEDIA" -> "#FFB86C"; default -> "#50fa7b";
        };
        javafx.scene.shape.Rectangle indic = new javafx.scene.shape.Rectangle(3, 15, javafx.scene.paint.Color.web(color));

        Label lbl = new Label(t.getTitolo());
        lbl.setStyle("-fx-text-fill: white; -fx-font-size: 10px;");

        box.getChildren().addAll(indic, lbl);
        box.setOnMouseClicked(e -> { if(e.getClickCount()==2) onItemClick.accept(t); });
        return box;
    }
}