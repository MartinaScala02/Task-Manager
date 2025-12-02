package it.unicas.project.template.address.view;

import it.unicas.project.template.address.model.Utenti;
import it.unicas.project.template.address.model.Tasks;
import it.unicas.project.template.address.model.dao.DAOException;
import it.unicas.project.template.address.model.dao.mysql.DAOUtenti;
import it.unicas.project.template.address.model.dao.mysql.DAOTasks;
import it.unicas.project.template.address.MainApp;
import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDate;
import java.util.function.Predicate;

public class MainScreenController {

    private MainApp mainApp;
    private boolean isOpen = false;

    @FXML private VBox sideMenu;
    @FXML private TextField newTaskField;
    @FXML private TextArea descriptionArea;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private ComboBox<String> priorityComboBox;
    @FXML private DatePicker dueDateField;
    @FXML private Label usernameLabelHeader;
    @FXML private ListView<Tasks> taskListView;

    private ObservableList<Tasks> tasks;
    private FilteredList<Tasks> filteredTasks;

    public MainScreenController() {}

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
        Utenti currentUser = MainApp.getCurrentUser();
        if (currentUser != null && currentUser.getNome() != null) {
            usernameLabelHeader.setText(currentUser.getNome());
        }
    }

    @FXML
    private void initialize() {
        tasks = FXCollections.observableArrayList();
        filteredTasks = new FilteredList<>(tasks, t -> true);
        taskListView.setItems(filteredTasks);

        setupComboBoxes();
        setupCellFactory();
        setupFilters();
    }

    private void setupComboBoxes() {
        categoryComboBox.getItems().addAll("Categoria", "Generale", "Lavoro", "Personale");
        priorityComboBox.getItems().addAll("Priorità", "Bassa", "Media", "Alta");
        categoryComboBox.getSelectionModel().selectFirst();
        priorityComboBox.getSelectionModel().selectFirst();
    }

    private void setupFilters() {
        Predicate<Tasks> combinedFilter = task -> true;
        categoryComboBox.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        priorityComboBox.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void applyFilters() {
        String selectedCategory = categoryComboBox.getValue();
        String selectedPriority = priorityComboBox.getValue();

        filteredTasks.setPredicate(task -> {
            boolean categoryMatches = selectedCategory.equals("Categoria") || task.getCategoria().equals(selectedCategory);
            boolean priorityMatches = selectedPriority.equals("Priorità") || task.getPriorita().equals(selectedPriority);
            return categoryMatches && priorityMatches;
        });
    }

    private void setupCellFactory() {
        taskListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Tasks task, boolean empty) {
                super.updateItem(task, empty);

                if (empty || task == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    // Checkbox completamento
                    CheckBox completeBox = new CheckBox();
                    completeBox.setSelected(task.getCompletamento());

                    // Badge priorità
                    Label priorityBadge = new Label(task.getPriorita());
                    priorityBadge.setStyle("-fx-text-fill: white; -fx-padding: 3 7 3 7; -fx-background-radius: 5; -fx-font-weight: bold;");
                    String coloreSfondo;
                    switch (task.getPriorita()) {
                        case "Alta": coloreSfondo = "#e74c3c"; break;
                        case "Media": coloreSfondo = "#f39c12"; break;
                        case "Bassa": coloreSfondo = "#27ae60"; break;
                        default: coloreSfondo = "grey"; break;
                    }

                    // Testo task
                    Label textLabel = new Label(task.getTitolo() + " (" + task.getCategoria() + ")");
                    textLabel.setStyle("-fx-font-size: 14px;");

                    // Funzione per applicare lo stile in base allo stato completamento
                    Runnable applyStyle = () -> {
                        if (completeBox.isSelected()) {
                            textLabel.setStyle("-fx-strikethrough: true; -fx-text-fill: gray; -fx-font-size: 14px;");
                            priorityBadge.setStyle("-fx-background-color: lightgrey; -fx-text-fill: white; -fx-padding: 3 7 3 7; -fx-background-radius: 5;");
                        } else {
                            textLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
                            priorityBadge.setStyle("-fx-background-color: " + coloreSfondo + "; -fx-text-fill: white; -fx-padding: 3 7 3 7; -fx-background-radius: 5;");
                        }
                    };
                    applyStyle.run();

                    // Gestione click checkbox
                    completeBox.setOnAction(event -> {
                        task.setCompletamento(completeBox.isSelected());
                        applyStyle.run();
                        handleTaskStatusChange(task);
                    });

                    // Layout HBox
                    HBox hbox = new HBox(10);
                    hbox.getChildren().addAll(completeBox, priorityBadge, textLabel);
                    hbox.setAlignment(Pos.CENTER_LEFT);

                    setText(null);
                    setGraphic(hbox);
                }
            }
        });
    }

    @FXML
    private void handleNewTask() {
        String testo = newTaskField.getText().trim();
        String descrizione = descriptionArea.getText().trim();
        String categoria = categoryComboBox.getValue();
        String priorita = priorityComboBox.getValue();
        LocalDate scadenza = dueDateField.getValue();

        if (testo.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Il titolo non può essere vuoto.");
            alert.showAndWait();
            return;
        }
        if (categoria.equals("Categoria") || priorita.equals("Priorità")) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Seleziona Categoria e Priorità valide.");
            alert.showAndWait();
            return;
        }

        Tasks nuovaTask = new Tasks();
        nuovaTask.setTitolo(testo);
        nuovaTask.setDescrizione(descrizione);
        nuovaTask.setCategoria(categoria);
        nuovaTask.setPriorita(priorita);
        nuovaTask.setScadenza(scadenza != null ? scadenza.toString() : "");
        nuovaTask.setIdUtente(MainApp.getCurrentUser().getIdUtente());

        try {
            DAOTasks.getInstance().insert(nuovaTask);
            tasks.add(0, nuovaTask);
            taskListView.scrollTo(0);

            // Reset campi
            newTaskField.clear();
            descriptionArea.clear();
            dueDateField.setValue(null);
            categoryComboBox.getSelectionModel().selectFirst();
            priorityComboBox.getSelectionModel().selectFirst();
        } catch (DAOException e) {
            new Alert(Alert.AlertType.ERROR, "Errore salvataggio task: " + e.getMessage()).show();
        }
    }

    private void handleTaskStatusChange(Tasks task) {
        try {
            DAOTasks.getInstance().update(task);
        } catch (DAOException e) {
            new Alert(Alert.AlertType.ERROR, "Errore aggiornamento stato: " + e.getMessage()).show();
        }
    }

    @FXML
    private void handleLogout() {
        mainApp.showUtentiLogin();
    }

    @FXML
    private void toggleMenu() {
        double target = isOpen ? -300 : 0;
        TranslateTransition tt = new TranslateTransition(Duration.millis(350), sideMenu);
        tt.setToX(target);
        tt.play();
        isOpen = !isOpen;
    }

    @FXML
    private void handleProfile() {

    }

    @FXML
    private void handleExit() {
        System.exit(0);
    }
}
