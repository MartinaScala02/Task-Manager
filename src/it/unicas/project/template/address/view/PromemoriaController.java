package it.unicas.project.template.address.view;

import it.unicas.project.template.address.model.Tasks;
import it.unicas.project.template.address.model.dao.mysql.DAOTasks;
import it.unicas.project.template.address.util.DateUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class PromemoriaController {

    @FXML
    private ListView<Tasks> reminderListView;

    private Stage dialogStage;

    // --- CALLBACKS ---
    private Runnable onCloseCallback;
    private Consumer<Tasks> onTaskSelected;

    // --- SETTERS ---
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setOnCloseCallback(Runnable onCloseCallback) {
        this.onCloseCallback = onCloseCallback;
    }

    public void setOnTaskSelected(Consumer<Tasks> onTaskSelected) {
        this.onTaskSelected = onTaskSelected;
    }

    @FXML
    private void initialize() {
        // Messaggio se la lista è vuota
        reminderListView.setPlaceholder(new Label("Nessuna scadenza imminente! 🎉"));

        // Setup Grafica Cella
        reminderListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Tasks item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    // 1. TESTI (Titolo e Data)
                    Text title = new Text(item.getTitolo() + "\n");
                    title.setFont(Font.font("System", FontWeight.BOLD, 14));
                    title.setFill(Color.WHITE);

                    String msgData = item.getScadenza();
                    Color colorData = Color.web("#bd93f9");

                    try {
                        LocalDate due = DateUtil.parse(item.getScadenza());
                        LocalDate today = LocalDate.now();

                        if (due != null) {
                            if (due.isBefore(today)) {
                                msgData = "SCADUTA IL: " + item.getScadenza();
                                colorData = Color.web("#ff5555");
                            } else if (due.isEqual(today)) {
                                msgData = "SCADE OGGI!";
                                colorData = Color.web("#ffb86c");
                            } else if (due.isEqual(today.plusDays(1))) {
                                msgData = "SCADE DOMANI";
                                colorData = Color.web("#8be9fd");
                            }
                        }
                    } catch (Exception e) {}

                    Text date = new Text(msgData);
                    date.setFill(colorData);
                    date.setFont(Font.font("System", FontWeight.BOLD, 11));

                    // TextFlow combina titolo e data
                    TextFlow textFlow = new TextFlow(title, date);

                    // Layout HBox
                    HBox container = new HBox(textFlow);
                    container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                    setGraphic(container);
                    setStyle("-fx-background-color: #3F2E51; -fx-background-radius: 10; -fx-border-width: 0 0 5 0; -fx-border-color: #2b2236; -fx-padding: 10; -fx-cursor: hand;");
                }
            }
        });

        // GESTIONE DOPPIO CLICK (Navigazione al Task)
        reminderListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Tasks selected = reminderListView.getSelectionModel().getSelectedItem();
                if (selected != null && onTaskSelected != null) {
                    onTaskSelected.accept(selected);
                    handleClose();
                }
            }
        });
    }

    public void loadUrgentTasks(int userId) {
        try {
            Tasks template = new Tasks();
            template.setIdUtente(userId);
            template.setCompletamento(false);

            List<Tasks> allTasks = DAOTasks.getInstance().select(template);

            LocalDate today = LocalDate.now();
            LocalDate tomorrow = today.plusDays(1);

            List<Tasks> urgentTasks = allTasks.stream()
                    .filter(t -> {
                        if (t.getScadenza() == null || t.getScadenza().isEmpty()) return false;
                        try {
                            LocalDate due = DateUtil.parse(t.getScadenza());
                            if (due == null) return false;
                            return !due.isAfter(tomorrow);
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList());

            ObservableList<Tasks> data = FXCollections.observableArrayList(urgentTasks);
            reminderListView.setItems(data);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleClose() {
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
}